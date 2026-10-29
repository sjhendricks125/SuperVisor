package com.example.supervisor_client.data.local

import android.content.Context

object BatteryConfigStore {
    private const val PREFS_NAME = "battery_monitoring_config"
    private const val KEY_INTERVAL_MINUTES = "ping_interval_minutes"
    private const val DEFAULT_INTERVAL_MINUTES = 15L

    fun normalizePingIntervalMinutes(minutes: Long): Long = minutes.coerceAtLeast(1L)

    fun getPingIntervalMinutes(context: Context): Long =
        normalizePingIntervalMinutes(
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getLong(KEY_INTERVAL_MINUTES, DEFAULT_INTERVAL_MINUTES)
        )

    fun setPingIntervalMinutes(context: Context, minutes: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_INTERVAL_MINUTES, normalizePingIntervalMinutes(minutes))
            .apply()
    }
}