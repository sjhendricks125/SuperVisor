package com.example.supervisor_client.data.local

import android.app.Notification
import android.content.Context
import android.os.Bundle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.supervisor_client.collectors.messages.MessageNotificationParser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MessageNotificationLogDaoTest {
    private lateinit var database: MessageLogDatabase

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            MessageLogDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun storesSenderAndTimestampAndPurgesRowsOlderThanCutoff() = runBlocking {
        val dao = database.messageNotificationLogDao()
        val cutoff = 30_000L
        dao.insertLog(MessageNotificationLogEntity(sender = "Alex", timestamp = cutoff - 1L))
        dao.insertLog(MessageNotificationLogEntity(sender = "Sam", timestamp = cutoff))
        dao.insertLog(MessageNotificationLogEntity(sender = "Jordan", timestamp = cutoff + 1L))

        assertEquals(1, dao.purgeLogsOlderThan(cutoff))

        assertEquals(
            listOf("Jordan" to cutoff + 1L, "Sam" to cutoff),
            dao.getRecentLogsFlow().first().map { it.sender to it.timestamp }
        )
    }

    @Test
    fun returnsOnlyTheOneHundredMostRecentLogsInDescendingOrder() = runBlocking {
        val dao = database.messageNotificationLogDao()
        (1L..105L).forEach { timestamp ->
            dao.insertLog(
                MessageNotificationLogEntity(
                    sender = "Sender $timestamp",
                    timestamp = timestamp
                )
            )
        }

        val recentLogs = dao.getRecentLogsFlow().first()

        assertEquals(100, recentLogs.size)
        assertEquals((6L..105L).reversed().toList(), recentLogs.map { it.timestamp })
    }

    @Test
    fun parsesDefaultSmsNotificationAndPersistsOnlySenderAndTimestamp() = runBlocking {
        val notification = Notification().apply {
            extras = Bundle().apply {
                putCharSequence(Notification.EXTRA_TITLE, "Jordan Smith")
                putCharSequence(Notification.EXTRA_TEXT, "Private message body")
            }
        }
        val parsedLog = MessageNotificationParser.logEntryFromNotification(
            notificationPackage = "com.example.default.sms",
            defaultSmsPackage = "com.example.default.sms",
            notificationExtras = notification.extras,
            timestamp = 123_456L
        )

        assertEquals(
            MessageNotificationLogEntity(
                id = 0,
                sender = "Jordan Smith",
                timestamp = 123_456L
            ),
            parsedLog
        )

        database.messageNotificationLogDao().insertLog(requireNotNull(parsedLog))

        val savedLogs = database.messageNotificationLogDao()
            .getRecentLogsFlow()
            .first()
        assertEquals(1, savedLogs.size)
        assertEquals("Jordan Smith", savedLogs.single().sender)
        assertEquals(123_456L, savedLogs.single().timestamp)
    }
}
