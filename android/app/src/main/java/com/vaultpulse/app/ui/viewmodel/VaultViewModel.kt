package com.vaultpulse.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
        // Strictly exclude SELF_TRANSFER from income/expense totals
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

    fun callBank(profile: AccountProfileEntity) {
        val number = profile.missedCallNumber ?: return
        BankSyncManager.launchMissedCallDialer(getApplication(), number)
    }

    fun syncBankSms(profile: AccountProfileEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            when (val outcome = BankSyncManager.syncLatestBankSms(getApplication(), db, profile)) {
                is BankSyncOutcome.Success -> {
                    syncAlert.value = "Checkpoint synced ${profile.shortName} to ₹${outcome.newBalance}."
                }
                is BankSyncOutcome.CallBankFirst -> {
                    syncAlert.value = outcome.reason
                }
            }
        }
    }

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

    fun setPrimaryUpiProfile(accountId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.accountDao().setPrimaryUpi(accountId)
        }
    }

    fun saveKeywordRule(keywordsCsv: String, category: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.keywordRuleDao().upsert(
                KeywordRuleEntity(
                    id = UUID.randomUUID().toString(),
                    keywordsCsv = keywordsCsv,
                    category = category
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
