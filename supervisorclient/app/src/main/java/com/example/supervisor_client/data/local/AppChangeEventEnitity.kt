package com.example.supervisor_client.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_change_events")
data class AppChangeEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val timestamp: Long,
    val packageName: String,
    val appName: String?,
    val versionName: String?,
    val isSystemApp: Boolean?,
    val eventType: String
)