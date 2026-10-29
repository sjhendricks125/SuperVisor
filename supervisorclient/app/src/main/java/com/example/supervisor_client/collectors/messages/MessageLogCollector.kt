package com.example.supervisor_client.collectors.messages

import android.app.Notification
import android.os.Bundle
import android.provider.Telephony
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.supervisor_client.SuperVisorApp
import com.example.supervisor_client.data.local.MessageNotificationLogEntity
import com.example.supervisor_client.data.local.MonitoringConfigStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MessageLogCollector : NotificationListenerService() {
    private companion object {
        const val TAG = "MessageNotificationLog"
    }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val messageLogDao by lazy {
        (application as SuperVisorApp).messageLogDatabase.messageNotificationLogDao()
    }

    override fun onNotificationPosted(notification: StatusBarNotification) {
        if (!MonitoringConfigStore.isMessageNotificationLoggingEnabled(this)) return

        val logEntry = MessageNotificationParser.logEntryFromNotification(
            notificationPackage = notification.packageName,
            defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(this),
            notificationExtras = notification.notification.extras,
            timestamp = notification.postTime
        ) ?: return

        serviceScope.launch {
            if (!MonitoringConfigStore.isMessageNotificationLoggingEnabled(
                    this@MessageLogCollector
                )
            ) {
                return@launch
            }
            try {
                messageLogDao.insertLog(logEntry)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                Log.e(TAG, "Unable to store a message notification log.", exception)
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}

internal object MessageNotificationParser {
    fun logEntryFromNotification(
        notificationPackage: String,
        defaultSmsPackage: String?,
        notificationExtras: Bundle?,
        timestamp: Long
    ): MessageNotificationLogEntity? =
        logEntryFromNotification(
            notificationPackage = notificationPackage,
            defaultSmsPackage = defaultSmsPackage,
            title = notificationExtras?.getCharSequence(Notification.EXTRA_TITLE),
            timestamp = timestamp
        )

    fun logEntryFromNotification(
        notificationPackage: String,
        defaultSmsPackage: String?,
        title: CharSequence?,
        timestamp: Long
    ): MessageNotificationLogEntity? =
        senderFromNotification(
            notificationPackage = notificationPackage,
            defaultSmsPackage = defaultSmsPackage,
            title = title
        )?.let { sender ->
            MessageNotificationLogEntity(
                sender = sender,
                timestamp = timestamp
            )
        }

    fun senderFromNotification(
        notificationPackage: String,
        defaultSmsPackage: String?,
        title: CharSequence?
    ): String? {
        if (defaultSmsPackage == null || notificationPackage != defaultSmsPackage) {
            return null
        }

        return senderFromTitle(title)
    }

    fun senderFromTitle(title: CharSequence?): String? =
        title?.toString()?.trim()?.takeIf { it.isNotEmpty() }
}
