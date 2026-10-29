package com.example.supervisor_client.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "message_notification_logs")
data class MessageNotificationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String,
    val timestamp: Long
)
