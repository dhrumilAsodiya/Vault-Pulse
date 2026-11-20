package com.vaultpulse.app.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.vaultpulse.app.data.local.VaultDatabase
import com.vaultpulse.app.data.parser.DeduplicationEngine
import com.vaultpulse.app.data.parser.SmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Event-driven NotificationListenerService capturing whitelisted Indian UPI packages
 * (GPay, PhonePe, Paytm, BHIM) and routing them to the Default Primary Profile.
 */
class UpiNotificationListenerService : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return
        if (!SmsParser.WHITELISTED_UPI_PACKAGES.contains(pkg)) return

        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val body = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val timestamp = sbn.postTime

        val parsed = SmsParser.parseUpiNotification(pkg, title, body, timestamp) ?: return
        val db = VaultDatabase.getInstance(applicationContext)

        serviceScope.launch {
            DeduplicationEngine.ingestEvent(db, parsed)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
