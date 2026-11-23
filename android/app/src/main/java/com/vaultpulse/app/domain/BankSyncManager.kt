package com.vaultpulse.app.domain

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import androidx.core.content.ContextCompat
import com.vaultpulse.app.data.local.AccountProfileEntity
import com.vaultpulse.app.data.local.VaultDatabase
import com.vaultpulse.app.data.parser.SmsParser
import java.util.Calendar

private const val TAG = "VaultPulse"

sealed class BankSyncOutcome {
    data class Success(val newBalance: Double, val syncedAt: Long, val smsBody: String) : BankSyncOutcome()
    data class CallBankFirst(val reason: String) : BankSyncOutcome()
}

data class BankPreset(
    val name: String,
    val keyword: String,
    val missedCallNumber: String
)

/**
 * 2-Step Bank Sync & Checkpoint Ledger manager:
 * Step 1: Launch Intent.ACTION_DIAL with profile's saved missed-call number (Dual SIM safe).
 * Step 2: Query SMS Inbox for latest balance SMS from bank, verify TODAY + timestamp > lastSyncedSmsTimestamp.
 * 
 * Matches bank SMS flexibly by:
 * - Stripping carrier prefixes (AD-, VM-, etc.)
 * - Matching bank keyword in sender ID or body
 * - Matching account last 4 digits
 * - Extracting available balance via resilient regex patterns
 */
object BankSyncManager {

    val POPULAR_BANKS = listOf(
        BankPreset("State Bank of India (SBI)", "SBI", "09223766666"),
        BankPreset("HDFC Bank", "HDFC", "18002703333"),
        BankPreset("ICICI Bank", "ICICI", "9594666123"),
        BankPreset("Axis Bank", "AXIS", "18004195959"),
        BankPreset("Kotak Mahindra", "KOTAK", "18002740110"),
        BankPreset("Punjab National Bank", "PNB", "18001802223"),
        BankPreset("Bank of Baroda", "BOB", "8468001111"),
        BankPreset("Canara Bank", "CANARA", "09015483483"),
        BankPreset("Union Bank of India", "UNION", "09223008586"),
        BankPreset("IDBI Bank", "IDBI", "18008431122")
    )

    fun launchMissedCallDialer(context: Context, missedCallNumber: String) {
        val cleanNumber = missedCallNumber.replace("\\s+".toRegex(), "")
        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$cleanNumber")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(dialIntent)
    }

    suspend fun syncLatestBankSms(
        context: Context,
        db: VaultDatabase,
        profile: AccountProfileEntity
    ): BankSyncOutcome {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Log.w(TAG, "READ_SMS permission not granted to app.")
            return BankSyncOutcome.CallBankFirst(
                "SMS permission is required to read bank balance checkpoints. Please grant SMS access in your phone settings."
            )
        }

        try {
            val projection = arrayOf(
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE
            )

            // Extract bank keyword from senderId, shortName, or name
            val bankKeyword = when {
                !profile.senderId.isNullOrBlank() -> profile.senderId.trim().uppercase()
                !profile.shortName.isNullOrBlank() -> profile.shortName.trim().uppercase()
                else -> profile.name.trim().uppercase()
            }.replace(Regex("[^A-Z0-9]"), "")

            val targetDigits = profile.maskedDigits.trim().replace(Regex("[^0-9]"), "")

            var bestBody: String? = null
            var bestTimestamp = 0L
            var bestBalance = 0.0

            // 1. Query the most recent 100 messages from SMS Inbox
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC"
            )?.use { cursor ->
                val addrIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)

                var count = 0
                while (cursor.moveToNext() && count < 100) {
                    count++
                    val rawSender = cursor.getString(addrIdx).orEmpty()
                    val body = cursor.getString(bodyIdx).orEmpty()
                    val ts = cursor.getLong(dateIdx)

                    val cleanSender = SmsParser.cleanSenderHeader(rawSender)
                    val senderMatches = bankKeyword.isEmpty() ||
                        cleanSender.contains(bankKeyword) ||
                        rawSender.uppercase().contains(bankKeyword) ||
                        body.uppercase().contains(bankKeyword)

                    val digitsMatch = if (targetDigits.isNotEmpty()) {
                        body.contains(targetDigits)
                    } else {
                        true
                    }

                    if (senderMatches && digitsMatch) {
                        val bal = SmsParser.extractAvailableBalance(body)
                        if (bal != null) {
                            bestBody = body
                            bestTimestamp = ts
                            bestBalance = bal
                            break // Top-most message is the newest matching balance SMS
                        }
                    }
                }
            }

            // 2. Fallback: If not found with bank keyword, match purely by last 4 digits + balance pattern
            if (bestBody == null && targetDigits.isNotEmpty()) {
                context.contentResolver.query(
                    Telephony.Sms.Inbox.CONTENT_URI,
                    projection,
                    "${Telephony.Sms.BODY} LIKE ?",
                    arrayOf("%$targetDigits%"),
                    "${Telephony.Sms.DATE} DESC"
                )?.use { cursor ->
                    val bodyIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                    val dateIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
                    while (cursor.moveToNext()) {
                        val body = cursor.getString(bodyIdx).orEmpty()
                        val ts = cursor.getLong(dateIdx)
                        val bal = SmsParser.extractAvailableBalance(body)
                        if (bal != null) {
                            bestBody = body
                            bestTimestamp = ts
                            bestBalance = bal
                            break
                        }
                    }
                }
            }

            val latestBody = bestBody
                ?: return BankSyncOutcome.CallBankFirst(
                    "No balance SMS found for ${profile.name} (ending in ...$targetDigits). Tap 'Call Bank' first, wait for the SMS, then tap 'Sync SMS'."
                )

            val now = System.currentTimeMillis()
            if (!isSameLocalDay(bestTimestamp, now)) {
                return BankSyncOutcome.CallBankFirst(
                    "The latest balance SMS found is from a previous day. Please tap 'Call Bank' to receive a fresh balance update for today."
                )
            }

            if (bestTimestamp <= profile.lastSyncedSmsTimestamp) {
                return BankSyncOutcome.CallBankFirst(
                    "You have already synced with the latest SMS from today. If your balance changed, tap 'Call Bank' first to request a new statement."
                )
            }

            db.accountDao().snapCheckpointBalance(profile.id, bestBalance, bestTimestamp)
            return BankSyncOutcome.Success(bestBalance, bestTimestamp, latestBody)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException reading SMS", e)
            return BankSyncOutcome.CallBankFirst("SMS permission denied by Android security policy. Please grant SMS permission in Settings.")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in syncLatestBankSms", e)
            return BankSyncOutcome.CallBankFirst("Error reading bank SMS: ${e.message ?: "Unknown error"}")
        }
    }

    private fun isSameLocalDay(tsA: Long, tsB: Long): Boolean {
        val calA = Calendar.getInstance().apply { timeInMillis = tsA }
        val calB = Calendar.getInstance().apply { timeInMillis = tsB }
        return calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR) &&
            calA.get(Calendar.DAY_OF_YEAR) == calB.get(Calendar.DAY_OF_YEAR)
    }
}
