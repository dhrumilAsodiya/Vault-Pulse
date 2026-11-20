package com.vaultpulse.app.data.parser

import com.vaultpulse.app.data.local.AccountProfileEntity
import com.vaultpulse.app.data.local.KeywordRuleEntity
import com.vaultpulse.app.data.local.MerchantAccountOverrideEntity
import com.vaultpulse.app.data.local.TransactionEntity
import com.vaultpulse.app.data.local.TransactionType
import com.vaultpulse.app.data.local.VaultDatabase
import java.util.UUID

data class ParsedEvent(
    val type: TransactionType,
    val amount: Double,
    val merchant: String,
    val maskedDigits: String?,
    val avlBal: Double?,
    val senderOrPackage: String,
    val source: String,
    val timestamp: Long,
    val rawBody: String
)

object SmsParser {
    private val NEGATIVE_BLOCKLIST = listOf(
        Regex("\\botp\\b", RegexOption.IGNORE_CASE),
        Regex("\\bfailed\\b", RegexOption.IGNORE_CASE),
        Regex("\\bdeclined\\b", RegexOption.IGNORE_CASE),
        Regex("\\bunsuccessful\\b", RegexOption.IGNORE_CASE),
        Regex("\\brequested\\b", RegexOption.IGNORE_CASE)
    )

    val WHITELISTED_UPI_PACKAGES = setOf(
        "com.google.android.apps.nbu.paisa.user", // Google Pay
        "com.phonepe.app",                        // PhonePe
        "net.one97.paytm",                        // Paytm
        "in.org.npci.upiapp"                      // BHIM
    )

    private val AMOUNT_REGEX = Regex("(?:rs\\.?|inr|₹)\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)", RegexOption.IGNORE_CASE)
    private val AVL_BAL_REGEX = Regex(
        "(?:avl\\.?\\s*bal(?:ance)?|available\\s*bal(?:ance)?|clear\\s*bal(?:ance)?|bal(?:ance)?)\\s*(?:is|:|-)?\\s*(?:rs\\.?|inr|₹)?\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)",
        RegexOption.IGNORE_CASE
    )
    private val MASKED_DIGITS_REGEX = Regex("(?:a/c|acct|account|card|[xX*•]{2,})\\s*(?:ending\\s*(?:with|in)?\\s*)?(?:[xX*•]+\\s*)?(\\d{4})\\b", RegexOption.IGNORE_CASE)
    private val DEBIT_REGEX = Regex("\\b(debited|debit|spent|paid|withdrawn|sent|purchase|charged|deducted)\\b", RegexOption.IGNORE_CASE)
    private val CREDIT_REGEX = Regex("\\b(credited|credit|received|deposited|refund|cashback|salary|payout)\\b", RegexOption.IGNORE_CASE)
    private val MERCHANT_REGEX = Regex("\\b(?:paid to|sent to|to|at|towards|from)\\s+([A-Za-z0-9 &._'-]{2,36}?)(?:\\s+(?:on|via|ref|utr|upi|avl|bal|using)|[.,;]|$)", RegexOption.IGNORE_CASE)

    fun isBlocked(text: String): Boolean = NEGATIVE_BLOCKLIST.any { it.containsMatchIn(text) }

    fun extractAvailableBalance(text: String): Double? {
        val match = AVL_BAL_REGEX.find(text) ?: return null
        return match.groupValues[1].replace(",", "").toDoubleOrNull()
    }

    fun parseSms(senderId: String, body: String, timestamp: Long): ParsedEvent? {
        if (isBlocked(body)) return null
        val type = when {
            DEBIT_REGEX.containsMatchIn(body) -> TransactionType.DEBIT
            CREDIT_REGEX.containsMatchIn(body) -> TransactionType.CREDIT
            else -> return null
        }
        val amount = AMOUNT_REGEX.find(body)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
            ?: return null
        if (amount <= 0.0) return null

        val digits = MASKED_DIGITS_REGEX.find(body)?.groupValues?.get(1)
        val avlBal = extractAvailableBalance(body)
        val merchant = MERCHANT_REGEX.find(body)?.groupValues?.get(1)?.trim() ?: "$senderId Transfer"

        return ParsedEvent(
            type = type,
            amount = amount,
            merchant = merchant,
            maskedDigits = digits,
            avlBal = avlBal,
            senderOrPackage = senderId,
            source = "SMS_AUTO",
            timestamp = timestamp,
            rawBody = body
        )
    }

    fun parseUpiNotification(packageName: String, title: String, body: String, timestamp: Long): ParsedEvent? {
        if (!WHITELISTED_UPI_PACKAGES.contains(packageName)) return null
        val combined = "$title $body".trim()
        if (isBlocked(combined)) return null

        val type = when {
            DEBIT_REGEX.containsMatchIn(combined) -> TransactionType.DEBIT
            CREDIT_REGEX.containsMatchIn(combined) -> TransactionType.CREDIT
            else -> return null
        }
        val amount = AMOUNT_REGEX.find(combined)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
            ?: return null
        if (amount <= 0.0) return null

        val merchant = MERCHANT_REGEX.find(combined)?.groupValues?.get(1)?.trim() ?: "UPI Transfer"
        return ParsedEvent(
            type = type,
            amount = amount,
            merchant = merchant,
            maskedDigits = null,
            avlBal = null,
            senderOrPackage = packageName,
            source = "UPI_LISTENER",
            timestamp = timestamp,
            rawBody = combined
        )
    }
}

object DeduplicationEngine {
    private const val DEDUP_WINDOW_MS = 180_000L // ±3 minutes

    fun categorize(
        merchant: String,
        rawBody: String,
        txType: TransactionType,
        rules: List<KeywordRuleEntity>
    ): String {
        if (txType == TransactionType.SELF_TRANSFER) return "Transfer"
        val haystack = "$merchant $rawBody".lowercase()
        for (rule in rules) {
            val tokens = rule.keywordsCsv.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
            if (tokens.any { haystack.contains(it) }) {
                return rule.category
            }
        }
        return if (txType == TransactionType.CREDIT) "Income / Salary" else "Other"
    }

    fun resolveProfile(
        event: ParsedEvent,
        accounts: List<AccountProfileEntity>,
        overrides: List<MerchantAccountOverrideEntity>
    ): AccountProfileEntity? {
        val lowerMerchant = event.merchant.lowercase()
        overrides.firstOrNull { lowerMerchant.contains(it.merchantTokenLower) }?.let { override ->
            accounts.firstOrNull { it.id == override.accountId }?.let { return it }
        }

        if (!event.maskedDigits.isNullOrBlank()) {
            accounts.firstOrNull { it.maskedDigits == event.maskedDigits }?.let { return it }
        }

        val upperSender = event.senderOrPackage.uppercase()
        accounts.firstOrNull { !it.senderId.isNullOrBlank() && upperSender.contains(it.senderId.uppercase()) }
            ?.let { return it }

        // Default Primary Profile for UPI notifications & unmatched events
        return accounts.firstOrNull { it.isPrimaryUpi } ?: accounts.firstOrNull()
    }

    suspend fun ingestEvent(db: VaultDatabase, event: ParsedEvent) {
        val txDao = db.transactionDao()
        val accDao = db.accountDao()
        val rules = db.keywordRuleDao().getAll()
        val overrides = db.merchantOverrideDao().getAll()
        val accounts = accDao.getAll()

        val existing = txDao.findDuplicateInWindow(
            type = event.type,
            amount = event.amount,
            timestamp = event.timestamp,
            windowMs = DEDUP_WINDOW_MS
        )

        if (existing != null) {
            val merged = existing.copy(
                merchant = if (existing.merchant.contains("UPI") || existing.merchant.contains("Transfer")) event.merchant else existing.merchant,
                avlBalSnapshot = event.avlBal ?: existing.avlBalSnapshot,
                rawRef = listOfNotNull(existing.rawRef, event.senderOrPackage).joinToString(" | ")
            )
            txDao.update(merged)
            if (event.avlBal != null) {
                accDao.snapCheckpointBalance(existing.accountId, event.avlBal, event.timestamp)
            }
            return
        }

        val targetAccount = resolveProfile(event, accounts, overrides) ?: return
        val category = categorize(event.merchant, event.rawBody, event.type, rules)

        val entity = TransactionEntity(
            id = UUID.randomUUID().toString(),
            type = event.type,
            amount = event.amount,
            merchant = event.merchant,
            category = category,
            accountId = targetAccount.id,
            toAccountId = null,
            timestamp = event.timestamp,
            source = event.source,
            avlBalSnapshot = event.avlBal,
            taxDeductible = false,
            rawRef = event.senderOrPackage
        )
        txDao.upsert(entity)

        if (event.avlBal != null) {
            accDao.snapCheckpointBalance(targetAccount.id, event.avlBal, event.timestamp)
        } else {
            val nextBal = if (event.type == TransactionType.CREDIT) {
                targetAccount.balance + event.amount
            } else {
                targetAccount.balance - event.amount
            }
            accDao.updateBalance(targetAccount.id, nextBal, event.timestamp)
        }
    }
}
