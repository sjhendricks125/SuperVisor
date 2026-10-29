package com.example.supervisor_client.data.local

import android.content.Context

object MonitoringConfigStore {
    private const val PREFERENCES_NAME = "supervisor_config"
    private const val KEY_PING_INTERVAL_MINUTES = "ping_interval_minutes"
    private const val DEFAULT_PING_INTERVAL_MINUTES = 15L
    private const val KEY_MESSAGE_NOTIFICATION_LOGGING_ENABLED =
        "message_notification_logging_enabled"

    fun getPingIntervalMinutes(context: Context): Long =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_PING_INTERVAL_MINUTES, DEFAULT_PING_INTERVAL_MINUTES)
            .let(::normalizePingIntervalMinutes)

    fun setPingIntervalMinutes(context: Context, minutes: Long) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_PING_INTERVAL_MINUTES, normalizePingIntervalMinutes(minutes))
            .apply()
    }

    fun isMessageNotificationLoggingEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_MESSAGE_NOTIFICATION_LOGGING_ENABLED, false)

    fun setMessageNotificationLoggingEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_MESSAGE_NOTIFICATION_LOGGING_ENABLED, enabled)
            .apply()
    }

    internal fun normalizePingIntervalMinutes(minutes: Long): Long =
        minutes.coerceAtLeast(1L)
}
