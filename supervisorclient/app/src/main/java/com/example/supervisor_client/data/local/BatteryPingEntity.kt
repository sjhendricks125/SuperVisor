package com.example.supervisor_client.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "battery_pings")
data class BatteryPingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val levelPercent: Int?,
    val isCharging: Boolean?,
    val plugType: String?,          // AC, USB, WIRELESS, NONE
    val health: String?,            // GOOD, OVERHEAT, DEAD, ...
    val temperatureCelsius: Float?,
    val voltageMillivolts: Int?,
    val isPowerSaveMode: Boolean?,
    val levelCategory: String,      // analogous to signalQuality
    val status: String,
    val isSynced: Boolean = false
)