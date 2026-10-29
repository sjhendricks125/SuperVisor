package com.example.supervisor_client.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BatteryPingDao {
    @Insert
    suspend fun insertPing(ping: BatteryPingEntity): Long

    @Query("SELECT * FROM battery_pings WHERE isSynced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedPings(): List<BatteryPingEntity>

    @Query("UPDATE battery_pings SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markPingsAsSynced(ids: List<Long>)

    @Query("SELECT * FROM battery_pings ORDER BY timestamp DESC")
    fun getAllPingsFlow(): Flow<List<BatteryPingEntity>>

    @Query("DELETE FROM battery_pings WHERE timestamp < :cutoffTimestamp")
    suspend fun purgePingsOlderThan(cutoffTimestamp: Long): Int
}