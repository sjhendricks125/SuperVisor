package com.example.supervisor_client.collectors.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager

internal class AndroidBatteryDataSource(context: Context) : BatteryDataSource {
    private val context = context.applicationContext

    override fun getCurrentReading(): BatteryReading? {
        // null receiver = just return the current sticky broadcast
        val intent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        ) ?: return null
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return parseBatteryIntent(intent, powerManager.isPowerSaveMode)
    }
}

internal fun parseBatteryIntent(intent: Intent, isPowerSaveMode: Boolean): BatteryReading {
    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    val percent = if (level >= 0 && scale > 0) level * 100 / scale else null

    val chargeStatus = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    val charging = chargeStatus == BatteryManager.BATTERY_STATUS_CHARGING ||
            chargeStatus == BatteryManager.BATTERY_STATUS_FULL

    val plug = when (intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)) {
        BatteryManager.BATTERY_PLUGGED_AC -> "AC"
        BatteryManager.BATTERY_PLUGGED_USB -> "USB"
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> "WIRELESS"
        else -> "NONE"
    }

    val health = when (intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
        BatteryManager.BATTERY_HEALTH_GOOD -> "GOOD"
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> "OVERHEAT"
        BatteryManager.BATTERY_HEALTH_DEAD -> "DEAD"
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "OVER_VOLTAGE"
        BatteryManager.BATTERY_HEALTH_COLD -> "COLD"
        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "FAILURE"
        else -> "UNKNOWN"
    }

    val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
    val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)

    return BatteryReading(
        levelPercent = percent,
        isCharging = charging,
        plugType = plug,
        health = health,
        temperatureCelsius = if (tempTenths != Int.MIN_VALUE) tempTenths / 10f else null,
        voltageMillivolts = if (voltage >= 0) voltage else null,
        isPowerSaveMode = isPowerSaveMode
    )
}