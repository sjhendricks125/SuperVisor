package com.example.supervisor_client.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MessageLogDatabaseMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var legacyDatabase: AppDatabase
    private lateinit var messageDatabase: MessageLogDatabase

    @Before
    fun createLegacyDatabase() {
        context.deleteDatabase(LEGACY_DATABASE_NAME)
        context.deleteDatabase(MessageLogDatabase.DATABASE_NAME)

        val legacyFile = context.getDatabasePath(LEGACY_DATABASE_NAME)
        legacyFile.parentFile?.mkdirs()
        val database = SQLiteDatabase.openOrCreateDatabase(legacyFile, null)
        try {
            database.execSQL(
                """
                CREATE TABLE location_pings (
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
            database.execSQL(
                """
                CREATE TABLE message_notification_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    sender TEXT NOT NULL,
                    timestamp INTEGER NOT NULL
                )
                """.trimIndent()
            )
            database.execSQL(
                "INSERT INTO message_notification_logs (sender, timestamp) VALUES (?, ?)",
                arrayOf<Any?>("Alex", 123_456L)
            )
            database.execSQL("PRAGMA user_version = 2")
        } finally {
            database.close()
        }
    }

    @After
    fun closeDatabases() {
        if (::messageDatabase.isInitialized) {
            messageDatabase.close()
        }
        if (::legacyDatabase.isInitialized) {
            legacyDatabase.close()
        }
        context.deleteDatabase(LEGACY_DATABASE_NAME)
        context.deleteDatabase(MessageLogDatabase.DATABASE_NAME)
    }

    @Test
    fun migrationCreatesMessageDatabaseAndMovesExistingLogs() = runBlocking {
        legacyDatabase = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            LEGACY_DATABASE_NAME
        ).addMigrations(
            AppDatabase.MIGRATION_1_2,
            AppDatabase.migrationToVersion3(
                context.getDatabasePath(MessageLogDatabase.DATABASE_NAME).absolutePath
            )
        ).setJournalMode(RoomDatabase.JournalMode.TRUNCATE).build()

        legacyDatabase.openHelper.writableDatabase
        messageDatabase = Room.databaseBuilder(
            context,
            MessageLogDatabase::class.java,
            MessageLogDatabase.DATABASE_NAME
        ).build()

        assertEquals(
            listOf("Alex" to 123_456L),
            messageDatabase.messageNotificationLogDao()
                .getRecentLogsFlow()
                .first()
                .map { it.sender to it.timestamp }
        )

        val legacyLogsRemain = legacyDatabase.openHelper.writableDatabase.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' " +
                "AND name = 'message_notification_logs'"
        ).use { it.moveToFirst() }
        assertFalse(legacyLogsRemain)
    }

    @Test
    fun migrationPreservesNewLogsWhenTheMessageDatabaseAlreadyExists() = runBlocking {
        messageDatabase = Room.databaseBuilder(
            context,
            MessageLogDatabase::class.java,
            MessageLogDatabase.DATABASE_NAME
        ).build()
        messageDatabase.messageNotificationLogDao().insertLog(
            MessageNotificationLogEntity(sender = "Jordan", timestamp = 234_567L)
        )

        legacyDatabase = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            LEGACY_DATABASE_NAME
        ).addMigrations(
            AppDatabase.MIGRATION_1_2,
            AppDatabase.migrationToVersion3(
                context.getDatabasePath(MessageLogDatabase.DATABASE_NAME).absolutePath
            )
        ).setJournalMode(RoomDatabase.JournalMode.TRUNCATE).build()
        legacyDatabase.openHelper.writableDatabase

        assertEquals(
            listOf("Jordan" to 234_567L, "Alex" to 123_456L),
            messageDatabase.messageNotificationLogDao()
                .getRecentLogsFlow()
                .first()
                .map { it.sender to it.timestamp }
        )
    }

    private companion object {
        const val LEGACY_DATABASE_NAME = "message-log-migration-test.db"
    }
}
