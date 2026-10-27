package com.example.supervisor_client.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        LocationPingEntity::class,
        BatteryPingEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun locationPingDao(): LocationPingDao
    abstract fun batteryPingDao(): BatteryPingDao

    companion object {
        const val RETENTION_PERIOD_MILLIS: Long = 30L * 24 * 60 * 60 * 1000

        @Volatile
        private var instance: AppDatabase? = null

        fun thirtyDayCutoffMillis(
            nowMillis: Long = System.currentTimeMillis()
        ): Long = nowMillis - RETENTION_PERIOD_MILLIS

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "supervisor.db"
                ).fallbackToDestructiveMigration(true)
                    .build()
                    .also { instance = it }
            }
    }
}