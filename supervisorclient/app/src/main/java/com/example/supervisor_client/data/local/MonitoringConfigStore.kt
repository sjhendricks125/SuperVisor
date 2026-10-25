package com.example.supervisor_client.data.local

import android.content.Context

object MonitoringConfigStore {
    private const val PREFERENCES_NAME = "supervisor_config"
    private const val KEY_PING_INTERVAL_MINUTES = "ping_interval_minutes"
    private const val DEFAULT_PING_INTERVAL_MINUTES = 15L

    fun getPingIntervalMinutes(context: Context): Long =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_PING_INTERVAL_MINUTES, DEFAULT_PING_INTERVAL_MINUTES)
            .coerceAtLeast(1L)

    fun setPingIntervalMinutes(context: Context, minutes: Long) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_PING_INTERVAL_MINUTES, minutes.coerceAtLeast(1L))
            .apply()
    }
}
