package com.vaultpulse.app.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Dao
interface AccountDao {
    @Query("SELECT * FROM account_profiles ORDER BY isPrimaryUpi DESC, name ASC")
    fun observeAll(): Flow<List<AccountProfileEntity>>

    @Query("SELECT * FROM account_profiles")
    suspend fun getAll(): List<AccountProfileEntity>

    @Query("SELECT * FROM account_profiles WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AccountProfileEntity?

    @Query("SELECT * FROM account_profiles WHERE isPrimaryUpi = 1 LIMIT 1")
    suspend fun getPrimaryUpiProfile(): AccountProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(account: AccountProfileEntity)

    @Query("UPDATE account_profiles SET balance = :balance, lastUpdatedTimestamp = :updatedAt WHERE id = :id")
    suspend fun updateBalance(id: String, balance: Double, updatedAt: Long)

    @Query("UPDATE account_profiles SET balance = :balance, lastSyncedSmsTimestamp = :syncTs, lastUpdatedTimestamp = :syncTs WHERE id = :id")
    suspend fun snapCheckpointBalance(id: String, balance: Double, syncTs: Long)

    @Query("UPDATE account_profiles SET isPrimaryUpi = (id = :primaryId)")
    suspend fun setPrimaryUpi(primaryId: String)

    @Query("DELETE FROM account_profiles WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE type = :type
          AND ABS(amount - :amount) < 0.01
          AND ABS(timestamp - :timestamp) <= :windowMs
        LIMIT 1
        """
    )
    suspend fun findDuplicateInWindow(
        type: TransactionType,
        amount: Double,
        timestamp: Long,
        windowMs: Long = 180_000L
    ): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tx: TransactionEntity)

    @Update
    suspend fun update(tx: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM transactions WHERE accountId = :accountId OR toAccountId = :accountId")
    suspend fun deleteByAccountId(accountId: String)
}

@Dao
interface KeywordRuleDao {
    @Query("SELECT * FROM keyword_rules")
    fun observeAll(): Flow<List<KeywordRuleEntity>>

    @Query("SELECT * FROM keyword_rules")
    suspend fun getAll(): List<KeywordRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: KeywordRuleEntity)

    @Query("DELETE FROM keyword_rules WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface MerchantOverrideDao {
    @Query("SELECT * FROM merchant_account_overrides")
    suspend fun getAll(): List<MerchantAccountOverrideEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(override: MerchantAccountOverrideEntity)
}

@Database(
    entities = [
        AccountProfileEntity::class,
        TransactionEntity::class,
        KeywordRuleEntity::class,
        MerchantAccountOverrideEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun keywordRuleDao(): KeywordRuleDao
    abstract fun merchantOverrideDao(): MerchantOverrideDao

    companion object {
        @Volatile
        private var INSTANCE: VaultDatabase? = null

        fun getInstance(context: Context): VaultDatabase {
            return INSTANCE ?: synchronized(this) {
                System.loadLibrary("sqlcipher")
                val passphrase = KeystoreHelper.getDatabasePassphrase(context.applicationContext)
                val factory = SupportOpenHelperFactory(passphrase)
                Room.databaseBuilder(
                    context.applicationContext,
                    VaultDatabase::class.java,
                    "vaultpulse_encrypted.db"
                )
                    .openHelperFactory(factory)
                    .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
