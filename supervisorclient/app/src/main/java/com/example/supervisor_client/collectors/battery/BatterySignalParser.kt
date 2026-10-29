package com.example.supervisor_client.collectors.battery

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

    internal fun createSuccessEntity(
        reading: BatteryReading,
        timestampMillis: Long = System.currentTimeMillis()
    ): BatteryPingEntity = BatteryPingEntity(
        timestamp = timestampMillis,
        levelPercent = reading.levelPercent,
        isCharging = reading.isCharging,
        plugType = reading.plugType,
        health = reading.health,
        temperatureCelsius = reading.temperatureCelsius,
        voltageMillivolts = reading.voltageMillivolts,
        isPowerSaveMode = reading.isPowerSaveMode,
        levelCategory = parseLevelCategory(reading.levelPercent),
        status = STATUS_SUCCESS
    )

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