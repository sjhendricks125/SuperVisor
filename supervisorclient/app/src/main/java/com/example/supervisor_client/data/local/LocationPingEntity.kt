package com.example.supervisor_client.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "location_pings")
data class LocationPingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val latitude: Double?,
    val longitude: Double?,
    val altitude: Double?,
    val accuracyRadius: Float?,
    val signalQuality: String,
    val status: String,
    val isSynced: Boolean = false
)
