package com.example.supervisor_client.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        LocationPingEntity::class,
        MessageNotificationLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun locationPingDao(): LocationPingDao
    abstract fun messageNotificationLogDao(): MessageNotificationLogDao

    companion object {
        const val RETENTION_PERIOD_MILLIS: Long = 30L * 24 * 60 * 60 * 1000

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS message_notification_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sender TEXT NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

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
                ).addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { instance = it }
            }
    }
}
