package com.example.supervisor_client.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BatteryPingDaoTest {
    private lateinit var context: Context
    private lateinit var database: AppDatabase

    @Before
    fun createDatabase() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun closeDatabase() {
        database.close()
        context.deleteDatabase(RESTART_TEST_DATABASE_NAME)
    }

    @Test
    fun purgeRemovesOnlyRowsStrictlyOlderThanCutoff() = runBlocking {
        val dao = database.batteryPingDao()
        val cutoff = 30_000L
        dao.insertPing(ping(timestamp = cutoff - 1L))
        dao.insertPing(ping(timestamp = cutoff))
        dao.insertPing(ping(timestamp = cutoff + 1L))

        assertEquals(1, dao.purgePingsOlderThan(cutoff))

        assertEquals(
            listOf(cutoff + 1L, cutoff),
            dao.getAllPingsFlow().first().map { it.timestamp }
        )
    }

    @Test
    fun unsyncedRowsAreOrderedAndOnlySelectedRowsAreMarkedSynced() = runBlocking {
        val dao = database.batteryPingDao()
        dao.insertPing(ping(timestamp = 20L))
        dao.insertPing(ping(timestamp = 10L))
        dao.insertPing(ping(timestamp = 30L, isSynced = true))

        val unsyncedBeforeUpdate = dao.getUnsyncedPings()
        assertEquals(listOf(10L, 20L), unsyncedBeforeUpdate.map { it.timestamp })

        dao.markPingsAsSynced(listOf(unsyncedBeforeUpdate.first().id))

        assertEquals(listOf(20L), dao.getUnsyncedPings().map { it.timestamp })
        assertEquals(
            listOf(30L, 20L, 10L),
            dao.getAllPingsFlow().first().map { it.timestamp }
        )
    }

    @Test
    fun insertedBatteryFieldsRoundTripUnchanged() = runBlocking {
        val dao = database.batteryPingDao()
        val original = BatteryPingEntity(
            timestamp = 500L,
            levelPercent = 73,
            isCharging = true,
            plugType = "USB",
            health = "GOOD",
            temperatureCelsius = 31.5f,
            voltageMillivolts = 4100,
            isPowerSaveMode = false,
            levelCategory = "HIGH",
            status = "SUCCESS"
        )

        val id = dao.insertPing(original)

        val stored = dao.getAllPingsFlow().first().single()
        assertEquals(original.copy(id = id), stored)
    }

    @Test
    fun committedUnsyncedRowsSurviveDatabaseCloseAndReopen() = runBlocking {
        context.deleteDatabase(RESTART_TEST_DATABASE_NAME)
        val firstDatabase = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            RESTART_TEST_DATABASE_NAME
        ).allowMainThreadQueries().build()

        firstDatabase.batteryPingDao().insertPing(ping(timestamp = 123L))
        firstDatabase.close()

        val reopenedDatabase = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            RESTART_TEST_DATABASE_NAME
        ).allowMainThreadQueries().build()

        try {
            val stored = reopenedDatabase.batteryPingDao().getUnsyncedPings()
            assertEquals(listOf(123L), stored.map { it.timestamp })
            assertEquals(1, stored.size)
        } finally {
            reopenedDatabase.close()
        }
    }

    private fun ping(
        timestamp: Long,
        isSynced: Boolean = false
    ) = BatteryPingEntity(
        timestamp = timestamp,
        levelPercent = null,
        isCharging = null,
        plugType = null,
        health = null,
        temperatureCelsius = null,
        voltageMillivolts = null,
        isPowerSaveMode = null,
        levelCategory = "NONE",
        status = "UNREACHABLE_ERROR",
        isSynced = isSynced
    )

    private companion object {
        const val RESTART_TEST_DATABASE_NAME = "battery-pings-restart-test.db"
    }
}
