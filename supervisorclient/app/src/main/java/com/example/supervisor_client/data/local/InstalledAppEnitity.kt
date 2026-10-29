package com.example.supervisor_client.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "installed_apps")
data class InstalledAppEntity(
    @PrimaryKey
    val packageName: String,

    val appName: String,
    val versionName: String,
    val isSystemApp: Boolean,
    val firstSeen: Long,
    val lastSeen: Long
)