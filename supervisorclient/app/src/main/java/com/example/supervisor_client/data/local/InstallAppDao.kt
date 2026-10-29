package com.example.supervisor_client.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface InstalledAppDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateApp(app: InstalledAppEntity)

    @Query("SELECT * FROM installed_apps ORDER BY appName ASC")
    suspend fun getAllApps(): List<InstalledAppEntity>

    @Query("SELECT * FROM installed_apps ORDER BY appName ASC")
    fun getAllAppsFlow(): Flow<List<InstalledAppEntity>>

    @Query("SELECT * FROM installed_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getApp(packageName: String): InstalledAppEntity?

    @Query("DELETE FROM installed_apps WHERE packageName = :packageName")
    suspend fun deleteApp(packageName: String)

    @Query("UPDATE installed_apps SET lastSeen = :timestamp WHERE packageName = :packageName")
    suspend fun updateLastSeen(packageName: String, timestamp: Long)
}