package com.example.supervisor_client.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [LocationPingEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun locationPingDao(): LocationPingDao

    companion object {
        const val RETENTION_PERIOD_MILLIS: Long = 30L * 24 * 60 * 60 * 1000

        internal val MIGRATION_1_2 = object : Migration(1, 2) {
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

        internal fun migrationToVersion3(messageDatabasePath: String) =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ATTACH DATABASE ? AS message_logs",
                        arrayOf(messageDatabasePath)
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS message_logs.message_notification_logs (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            sender TEXT NOT NULL,
                            timestamp INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        INSERT INTO message_logs.message_notification_logs
                            (sender, timestamp)
                        SELECT sender, timestamp
                        FROM main.message_notification_logs
                        """.trimIndent()
                    )
                    db.execSQL("DROP TABLE IF EXISTS main.message_notification_logs")
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
                ).addMigrations(
                    MIGRATION_1_2,
                    migrationToVersion3(
                        context.getDatabasePath(MessageLogDatabase.DATABASE_NAME).absolutePath
                    )
                )
                    .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { instance = it }
            }
    }
}
