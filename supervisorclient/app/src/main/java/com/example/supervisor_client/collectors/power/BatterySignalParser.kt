package com.example.supervisor_client.collectors.battery

import android.content.Intent
import android.os.BatteryManager
import com.example.supervisor_client.data.local.BatteryPingEntity

object BatterySignalParser {
    const val LEVEL_HIGH = "HIGH"          // > 50%
    const val LEVEL_MODERATE = "MODERATE"  // 21-50%
    const val LEVEL_LOW = "LOW"            // 6-20%
    const val LEVEL_CRITICAL = "CRITICAL"  // <= 5%
    const val LEVEL_NONE = "NONE"

    const val STATUS_SUCCESS = "SUCCESS"
    const val STATUS_UNAVAILABLE = "UNREACHABLE_UNAVAILABLE"
    const val STATUS_ERROR = "UNREACHABLE_ERROR"

    fun parseLevelCategory(percent: Int?): String = when {
        percent == null || percent < 0 -> LEVEL_NONE
        percent > 50 -> LEVEL_HIGH
        percent > 20 -> LEVEL_MODERATE
        percent > 5 -> LEVEL_LOW
        else -> LEVEL_CRITICAL
    }

    fun createSuccessEntity(
        intent: Intent,
        isPowerSaveMode: Boolean,
        timestampMillis: Long = System.currentTimeMillis()
    ): BatteryPingEntity {
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

        return BatteryPingEntity(
            timestamp = timestampMillis,
            levelPercent = percent,
            isCharging = charging,
            plugType = plug,
            health = health,
            temperatureCelsius = if (tempTenths != Int.MIN_VALUE) tempTenths / 10f else null,
            voltageMillivolts = if (voltage >= 0) voltage else null,
            isPowerSaveMode = isPowerSaveMode,
            levelCategory = parseLevelCategory(percent),
            status = STATUS_SUCCESS
        )
    }

    fun createUnreachableEntity(
        status: String,
        timestampMillis: Long = System.currentTimeMillis()
    ): BatteryPingEntity = BatteryPingEntity(
        timestamp = timestampMillis,
        levelPercent = null,
        isCharging = null,
        plugType = null,
        health = null,
        temperatureCelsius = null,
        voltageMillivolts = null,
        isPowerSaveMode = null,
        levelCategory = LEVEL_NONE,
        status = status,
        isSynced = false
    )
}