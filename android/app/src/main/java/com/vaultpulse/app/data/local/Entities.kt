package com.vaultpulse.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType {
    DEBIT,
    CREDIT,
    SELF_TRANSFER
}

enum class AccountKind {
    BANK,
    CASH
}

@Entity(tableName = "account_profiles")
data class AccountProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val shortName: String,
    val accountKind: AccountKind,
    val maskedDigits: String,
    val tagLabel: String,
    val balance: Double,
    val isPrimaryUpi: Boolean,
    val upiAlias: String?,
    val senderId: String?,
    val missedCallNumber: String?,
    val lastSyncedSmsTimestamp: Long,
    val lastUpdatedTimestamp: Long
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val type: TransactionType,
    val amount: Double,
    val merchant: String,
    val category: String,
    val accountId: String,
    val toAccountId: String?,
    val timestamp: Long,
    val source: String, // MANUAL, SMS_AUTO, UPI_LISTENER
    val avlBalSnapshot: Double?,
    val taxDeductible: Boolean,
    val rawRef: String?
)

@Entity(tableName = "keyword_rules")
data class KeywordRuleEntity(
    @PrimaryKey val id: String,
    val keywordsCsv: String,
    val category: String
)

@Entity(tableName = "merchant_account_overrides")
data class MerchantAccountOverrideEntity(
    @PrimaryKey val merchantTokenLower: String,
    val accountId: String
)
