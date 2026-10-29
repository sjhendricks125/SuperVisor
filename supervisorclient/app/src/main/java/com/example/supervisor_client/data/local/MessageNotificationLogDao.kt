package com.example.supervisor_client.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageNotificationLogDao {
    @Insert
    suspend fun insertLog(log: MessageNotificationLogEntity): Long

    @Query(
        "SELECT * FROM message_notification_logs " +
            "ORDER BY timestamp DESC LIMIT 100"
    )
    fun getRecentLogsFlow(): Flow<List<MessageNotificationLogEntity>>

    @Query("DELETE FROM message_notification_logs WHERE timestamp < :cutoffTimestamp")
    suspend fun purgeLogsOlderThan(cutoffTimestamp: Long): Int
}
