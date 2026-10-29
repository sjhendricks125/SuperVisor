package com.example.supervisor_client.collectors.battery

internal data class BatteryReading(
    val levelPercent: Int?,
    val isCharging: Boolean,
    val plugType: String,
    val health: String,
    val temperatureCelsius: Float?,
    val voltageMillivolts: Int?,
    val isPowerSaveMode: Boolean
)

internal interface BatteryDataSource {
    fun getCurrentReading(): BatteryReading?
}