package com.vaultpulse.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.vaultpulse.app.data.local.VaultDatabase
import com.vaultpulse.app.data.parser.DeduplicationEngine
import com.vaultpulse.app.data.parser.SmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Event-driven SMS_RECEIVED BroadcastReceiver (zero background polling).
 */
class SmsBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) return

        val senderId = messages.firstOrNull()?.originatingAddress ?: return
        val body = messages.joinToString(separator = "") { it.messageBody ?: "" }
        val timestamp = messages.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()

        val parsed = SmsParser.parseSms(senderId, body, timestamp) ?: return

        val pendingResult = goAsync()
        val db = VaultDatabase.getInstance(context.applicationContext)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                DeduplicationEngine.ingestEvent(db, parsed)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
