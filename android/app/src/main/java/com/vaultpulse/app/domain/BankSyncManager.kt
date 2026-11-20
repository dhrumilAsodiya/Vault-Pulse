package com.vaultpulse.app.domain

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony
import com.vaultpulse.app.data.local.AccountProfileEntity
import com.vaultpulse.app.data.local.VaultDatabase
import com.vaultpulse.app.data.parser.SmsParser
import java.util.Calendar

sealed class BankSyncOutcome {
    data class Success(val newBalance: Double, val syncedAt: Long, val smsBody: String) : BankSyncOutcome()
    data class CallBankFirst(val reason: String) : BankSyncOutcome()
}

/**
 * 2-Step Bank Sync & Checkpoint Ledger manager:
 * Step 1: Launch Intent.ACTION_DIAL with profile's saved missed-call number (Dual SIM safe).
 * Step 2: Query SMS Inbox for latest balance SMS from bank senderId, verify TODAY + timestamp > lastSyncedSmsTimestamp.
 */
object BankSyncManager {

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
        val senderKey = profile.senderId
            ?: return BankSyncOutcome.CallBankFirst("No SMS sender ID configured for ${profile.name}.")

        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        var matchedBody: String? = null
        var matchedTimestamp = 0L

        context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            projection,
            "${Telephony.Sms.ADDRESS} LIKE ?",
            arrayOf("%$senderKey%"),
            "${Telephony.Sms.DATE} DESC"
        )?.use { cursor ->
            val bodyIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            while (cursor.moveToNext()) {
                val body = cursor.getString(bodyIdx).orEmpty()
                val ts = cursor.getLong(dateIdx)
                if (SmsParser.extractAvailableBalance(body) != null) {
                    matchedBody = body
                    matchedTimestamp = ts
                    break
                }
            }
        }

        val latestBody = matchedBody
            ?: return BankSyncOutcome.CallBankFirst(
                "No balance SMS found from $senderKey. Tap Call Bank first, wait for the SMS, then run Sync SMS."
            )

        if (!isSameLocalDay(matchedTimestamp, System.currentTimeMillis()) ||
            matchedTimestamp <= profile.lastSyncedSmsTimestamp
        ) {
            return BankSyncOutcome.CallBankFirst(
                "Latest balance SMS is not newer than your last checkpoint sync today. Please call the bank again first."
            )
        }

        val parsedBalance = SmsParser.extractAvailableBalance(latestBody)
            ?: return BankSyncOutcome.CallBankFirst(
                "Could not read Avl Bal from the latest SMS. Please call the bank again first."
            )

        db.accountDao().snapCheckpointBalance(profile.id, parsedBalance, matchedTimestamp)
        return BankSyncOutcome.Success(parsedBalance, matchedTimestamp, latestBody)
    }

    private fun isSameLocalDay(tsA: Long, tsB: Long): Boolean {
        val calA = Calendar.getInstance().apply { timeInMillis = tsA }
        val calB = Calendar.getInstance().apply { timeInMillis = tsB }
        return calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR) &&
            calA.get(Calendar.DAY_OF_YEAR) == calB.get(Calendar.DAY_OF_YEAR)
    }
}
