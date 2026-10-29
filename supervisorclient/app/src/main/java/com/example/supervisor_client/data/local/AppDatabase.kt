package com.example.supervisor_client.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        InstalledAppEntity::class,
        AppChangeEventEntity::class,
        LocationPingEntity::class,
        BatteryPingEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun installedAppDao(): InstalledAppDao

    abstract fun appChangeEventDao(): AppChangeEventDao

    abstract fun locationPingDao(): LocationPingDao

    abstract fun batteryPingDao(): BatteryPingDao

    companion object {

        const val RETENTION_PERIOD_MILLIS: Long =
            30L * 24 * 60 * 60 * 1000

        @Volatile
        private var instance: AppDatabase? = null

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

        internal fun migrationToVersion3(
            messageDatabasePath: String
        ) = object : Migration(2, 3) {

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

                db.execSQL(
                    "DROP TABLE IF EXISTS main.message_notification_logs"
                )
            }
        }

        internal val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS installed_apps (
                        packageName TEXT NOT NULL,
                        appName TEXT NOT NULL,
                        versionName TEXT NOT NULL,
                        isSystemApp INTEGER NOT NULL,
                        firstSeen INTEGER NOT NULL,
                        lastSeen INTEGER NOT NULL,
                        PRIMARY KEY(packageName)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS app_change_events (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        packageName TEXT NOT NULL,
                        appName TEXT,
                        versionName TEXT,
                        isSystemApp INTEGER,
                        eventType TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS location_pings (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        latitude REAL,
                        longitude REAL,
                        altitude REAL,
                        accuracyRadius REAL,
                        signalQuality TEXT NOT NULL,
                        status TEXT NOT NULL,
                        isSynced INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS battery_pings (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        levelPercent INTEGER,
                        isCharging INTEGER,
                        plugType TEXT,
                        health TEXT,
                        temperatureCelsius REAL,
                        voltageMillivolts INTEGER,
                        isPowerSaveMode INTEGER,
                        levelCategory TEXT NOT NULL,
                        status TEXT NOT NULL,
                        isSynced INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun thirtyDayCutoffMillis(
            nowMillis: Long = System.currentTimeMillis()
        ): Long =
            nowMillis - RETENTION_PERIOD_MILLIS

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {

                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "supervisor.db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        migrationToVersion3(
                            context.getDatabasePath(
                                MessageLogDatabase.DATABASE_NAME
                            ).absolutePath
                        ),
                        MIGRATION_3_4
                    )
                    .setJournalMode(
                        RoomDatabase.JournalMode.TRUNCATE
                    )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also {
                        instance = it
                    }
            }
    }
}