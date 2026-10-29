package com.example.supervisor_client.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationPingDao {
    @Insert
    suspend fun insertPing(ping: LocationPingEntity): Long

    @Query("SELECT * FROM location_pings WHERE isSynced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedPings(): List<LocationPingEntity>

    @Query("UPDATE location_pings SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markPingsAsSynced(ids: List<Long>)

    @Query("SELECT * FROM location_pings ORDER BY timestamp DESC")
    fun getAllPingsFlow(): Flow<List<LocationPingEntity>>

    @Query("DELETE FROM location_pings WHERE timestamp < :cutoffTimestamp")
    suspend fun purgePingsOlderThan(cutoffTimestamp: Long): Int
}
