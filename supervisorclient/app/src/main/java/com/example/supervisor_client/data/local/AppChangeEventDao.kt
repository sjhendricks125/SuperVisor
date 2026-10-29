package com.example.supervisor_client.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppChangeEventDao {

    @Insert
    suspend fun insertEvent(event: AppChangeEventEntity): Long

    @Query("SELECT * FROM app_change_events ORDER BY timestamp DESC")
    fun getAllEventsFlow(): Flow<List<AppChangeEventEntity>>

    @Query("SELECT * FROM app_change_events ORDER BY timestamp DESC")
    suspend fun getAllEvents(): List<AppChangeEventEntity>

    @Query("DELETE FROM app_change_events WHERE timestamp < :cutoffTimestamp")
    suspend fun purgeEventsOlderThan(cutoffTimestamp: Long): Int
}