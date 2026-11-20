package com.vaultpulse.app.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vaultpulse.app.data.local.AccountKind
import com.vaultpulse.app.data.local.AccountProfileEntity
import com.vaultpulse.app.data.local.KeywordRuleEntity
import com.vaultpulse.app.data.local.MerchantAccountOverrideEntity
import com.vaultpulse.app.data.local.TransactionEntity
import com.vaultpulse.app.data.local.TransactionType
import com.vaultpulse.app.data.local.VaultDatabase
import com.vaultpulse.app.data.parser.DeduplicationEngine
import com.vaultpulse.app.domain.BankSyncManager
import com.vaultpulse.app.domain.BankSyncOutcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

private const val TAG = "VaultPulse"

data class VaultUiState(
    val accounts: List<AccountProfileEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val keywordRules: List<KeywordRuleEntity> = emptyList(),
    val netLiquidity: Double = 0.0,
    val totalSpent: Double = 0.0,
    val totalIncome: Double = 0.0,
    val savedPercent: Int = 0,
    val syncAlertMessage: String? = null
)

class VaultViewModel(application: Application) : AndroidViewModel(application) {
    private val db = VaultDatabase.getInstance(application)
    private val syncAlert = MutableStateFlow<String?>(null)

    val uiState: StateFlow<VaultUiState> = combine(
        db.accountDao().observeAll(),
        db.transactionDao().observeAll(),
        db.keywordRuleDao().observeAll(),
        syncAlert
    ) { accounts, txs, rules, alert ->
        // Exclude SELF_TRANSFER from income/expense calculations
        val nonTransfers = txs.filter { it.type != TransactionType.SELF_TRANSFER }
        val spent = nonTransfers.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount }
        val income = nonTransfers.filter { it.type == TransactionType.CREDIT }.sumOf { it.amount }
        val savedPct = if (income > 0.0) (((income - spent) / income) * 100).toInt().coerceAtLeast(0) else 0

        VaultUiState(
            accounts = accounts,
            transactions = txs,
            keywordRules = rules,
            netLiquidity = accounts.sumOf { it.balance },
            totalSpent = spent,
            totalIncome = income,
            savedPercent = savedPct,
            syncAlertMessage = alert
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VaultUiState())

    fun dismissSyncAlert() {
        syncAlert.value = null
    }

    // ==========================================
    // ACCOUNT CRUD
    // ==========================================

    fun saveAccount(
        existingId: String?,
        name: String,
        shortName: String,
        accountKind: AccountKind,
        maskedDigits: String,
        tagLabel: String,
        balance: Double,
        isPrimaryUpi: Boolean,
        upiAlias: String?,
        senderId: String?,
        missedCallNumber: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val id = existingId ?: UUID.randomUUID().toString()

            val account = AccountProfileEntity(
                id = id,
                name = name.trim(),
                shortName = shortName.trim().ifBlank { name.take(6).trim() },
                accountKind = accountKind,
                maskedDigits = maskedDigits.trim(),
                tagLabel = tagLabel.trim().ifBlank { if (accountKind == AccountKind.BANK) "Bank Account" else "Cash" },
                balance = balance,
                isPrimaryUpi = isPrimaryUpi,
                upiAlias = upiAlias?.trim()?.ifBlank { null },
                senderId = senderId?.trim()?.ifBlank { null },
                missedCallNumber = missedCallNumber?.trim()?.ifBlank { null },
                lastSyncedSmsTimestamp = 0L,
                lastUpdatedTimestamp = now
            )

            if (isPrimaryUpi) {
                db.accountDao().setPrimaryUpi(id)
            }
            db.accountDao().upsert(account)
            Log.i(TAG, "Saved account: ${account.name} (id: $id, balance: ₹$balance)")
        }
    }

    fun deleteAccount(accountId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.transactionDao().deleteByAccountId(accountId)
            db.accountDao().deleteById(accountId)
            Log.i(TAG, "Deleted account $accountId and its transactions.")
        }
    }

    fun setPrimaryUpiProfile(accountId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.accountDao().setPrimaryUpi(accountId)
        }
    }

    fun callBank(profile: AccountProfileEntity) {
        val number = profile.missedCallNumber ?: return
        Log.i(TAG, "Calling bank for profile: ${profile.name} via $number")
        BankSyncManager.launchMissedCallDialer(getApplication(), number)
    }

    fun syncBankSms(profile: AccountProfileEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.i(TAG, "Triggering SMS sync for ${profile.shortName}")
                when (val outcome = BankSyncManager.syncLatestBankSms(getApplication(), db, profile)) {
                    is BankSyncOutcome.Success -> {
                        syncAlert.value = "Checkpoint synced ${profile.shortName} to ₹${outcome.newBalance}."
                        Log.i(TAG, "SMS sync success: ₹${outcome.newBalance}")
                    }
                    is BankSyncOutcome.CallBankFirst -> {
                        syncAlert.value = outcome.reason
                        Log.w(TAG, "SMS sync notice: ${outcome.reason}")
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Exception during SMS sync", t)
                syncAlert.value = "Could not sync SMS: ${t.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    // ==========================================
    // TRANSACTION CRUD
    // ==========================================

    fun addTransaction(
        type: TransactionType,
        amount: Double,
        merchant: String,
        category: String,
        accountId: String,
        toAccountId: String?,
        taxDeductible: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val rules = db.keywordRuleDao().getAll()
            val resolvedCategory = if (type == TransactionType.SELF_TRANSFER) {
                "Transfer"
            } else if (category.isBlank()) {
                DeduplicationEngine.categorize(merchant, "", type, rules)
            } else {
                category
            }

            Log.i(TAG, "Adding transaction: $type ₹$amount to $merchant [Category: $resolvedCategory, Account: $accountId]")

            db.transactionDao().upsert(
                TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    type = type,
                    amount = amount,
                    merchant = merchant,
                    category = resolvedCategory,
                    accountId = accountId,
                    toAccountId = toAccountId,
                    timestamp = now,
                    source = "MANUAL",
                    avlBalSnapshot = null,
                    taxDeductible = taxDeductible,
                    rawRef = null
                )
            )

            val fromAcc = db.accountDao().getById(accountId)
            if (fromAcc != null) {
                val nextFromBal = when (type) {
                    TransactionType.CREDIT -> fromAcc.balance + amount
                    TransactionType.DEBIT, TransactionType.SELF_TRANSFER -> fromAcc.balance - amount
                }
                db.accountDao().updateBalance(fromAcc.id, nextFromBal, now)
            }

            if (type == TransactionType.SELF_TRANSFER && toAccountId != null) {
                val toAcc = db.accountDao().getById(toAccountId)
                if (toAcc != null) {
                    db.accountDao().updateBalance(toAcc.id, toAcc.balance + amount, now)
                }
            }
        }
    }

    fun updateTransaction(
        existingTx: TransactionEntity,
        newType: TransactionType,
        newAmount: Double,
        newMerchant: String,
        newCategory: String,
        newAccountId: String,
        newToAccountId: String?,
        taxDeductible: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()

            // 1. Reverse old balance effect
            val oldFromAcc = db.accountDao().getById(existingTx.accountId)
            if (oldFromAcc != null) {
                val reversed = when (existingTx.type) {
                    TransactionType.CREDIT -> oldFromAcc.balance - existingTx.amount
                    TransactionType.DEBIT, TransactionType.SELF_TRANSFER -> oldFromAcc.balance + existingTx.amount
                }
                db.accountDao().updateBalance(oldFromAcc.id, reversed, now)
            }
            if (existingTx.type == TransactionType.SELF_TRANSFER && existingTx.toAccountId != null) {
                val oldToAcc = db.accountDao().getById(existingTx.toAccountId)
                if (oldToAcc != null) {
                    db.accountDao().updateBalance(oldToAcc.id, oldToAcc.balance - existingTx.amount, now)
                }
            }

            // 2. Apply new balance effect
            val newFromAcc = db.accountDao().getById(newAccountId)
            if (newFromAcc != null) {
                val applied = when (newType) {
                    TransactionType.CREDIT -> newFromAcc.balance + newAmount
                    TransactionType.DEBIT, TransactionType.SELF_TRANSFER -> newFromAcc.balance - newAmount
                }
                db.accountDao().updateBalance(newFromAcc.id, applied, now)
            }
            if (newType == TransactionType.SELF_TRANSFER && newToAccountId != null) {
                val newToAcc = db.accountDao().getById(newToAccountId)
                if (newToAcc != null) {
                    db.accountDao().updateBalance(newToAcc.id, newToAcc.balance + newAmount, now)
                }
            }

            // 3. Update transaction record
            db.transactionDao().update(
                existingTx.copy(
                    type = newType,
                    amount = newAmount,
                    merchant = newMerchant,
                    category = newCategory.ifBlank { "General" },
                    accountId = newAccountId,
                    toAccountId = newToAccountId,
                    taxDeductible = taxDeductible
                )
            )
            Log.i(TAG, "Updated transaction: ${existingTx.id}")
        }
    }

    fun deleteTransaction(tx: TransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            // Reverse account balance impact
            val fromAcc = db.accountDao().getById(tx.accountId)
            if (fromAcc != null) {
                val reversed = when (tx.type) {
                    TransactionType.CREDIT -> fromAcc.balance - tx.amount
                    TransactionType.DEBIT, TransactionType.SELF_TRANSFER -> fromAcc.balance + tx.amount
                }
                db.accountDao().updateBalance(fromAcc.id, reversed, now)
            }
            if (tx.type == TransactionType.SELF_TRANSFER && tx.toAccountId != null) {
                val toAcc = db.accountDao().getById(tx.toAccountId)
                if (toAcc != null) {
                    db.accountDao().updateBalance(toAcc.id, toAcc.balance - tx.amount, now)
                }
            }

            db.transactionDao().deleteById(tx.id)
            Log.i(TAG, "Deleted transaction ${tx.id} and reversed account balance.")
        }
    }

    fun reassignTransactionAccount(
        tx: TransactionEntity,
        newAccountId: String,
        autoReassignFuture: Boolean
    ) {
        if (tx.accountId == newAccountId) return
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val oldAcc = db.accountDao().getById(tx.accountId)
            val newAcc = db.accountDao().getById(newAccountId)

            if (oldAcc != null) {
                val reversed = if (tx.type == TransactionType.CREDIT) oldAcc.balance - tx.amount else oldAcc.balance + tx.amount
                db.accountDao().updateBalance(oldAcc.id, reversed, now)
            }
            if (newAcc != null) {
                val applied = if (tx.type == TransactionType.CREDIT) newAcc.balance + tx.amount else newAcc.balance - tx.amount
                db.accountDao().updateBalance(newAcc.id, applied, now)
            }

            db.transactionDao().update(tx.copy(accountId = newAccountId))

            if (autoReassignFuture) {
                val token = tx.merchant.split(" ").firstOrNull()?.lowercase().orEmpty()
                if (token.isNotBlank()) {
                    db.merchantOverrideDao().upsert(MerchantAccountOverrideEntity(token, newAccountId))
                }
            }
        }
    }

    // ==========================================
    // KEYWORD RULES CRUD
    // ==========================================

    fun saveKeywordRule(id: String?, keywordsCsv: String, category: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.keywordRuleDao().upsert(
                KeywordRuleEntity(
                    id = id ?: UUID.randomUUID().toString(),
                    keywordsCsv = keywordsCsv.trim(),
                    category = category.trim()
                )
            )
        }
    }

    fun deleteKeywordRule(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.keywordRuleDao().deleteById(id)
        }
    }
}
