package com.example.supervisor_client.collectors.battery

import com.example.supervisor_client.data.local.AppDatabase
import com.example.supervisor_client.data.local.BatteryPingEntity
import com.example.supervisor_client.data.local.InMemoryBatteryPingDao
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BatteryCollectorStubTest {
    @Test
    fun storesReturnedReadingAndTimestamp() = runTest {
        val source = FakeBatteryDataSource(
            reading = reading(
                levelPercent = 15,
                isCharging = true,
                plugType = "USB",
                temperatureCelsius = 30.5f,
                voltageMillivolts = 3800
            )
        )
        val dao = InMemoryBatteryPingDao()

        val result = collector(source, dao).collectAndStorePing()

        assertEquals(1, source.readCount)
        assertEquals(15, result.levelPercent)
        assertEquals(true, result.isCharging)
        assertEquals("USB", result.plugType)
        assertEquals(30.5f, result.temperatureCelsius!!, 0f)
        assertEquals(3800, result.voltageMillivolts)
        assertEquals(BatterySignalParser.LEVEL_LOW, result.levelCategory)
        assertEquals(BatterySignalParser.STATUS_SUCCESS, result.status)
        assertEquals(NOW, result.timestamp)
        assertFalse(result.isSynced)
        assertEquals(result, dao.pings.single().copy(id = result.id))
    }

    @Test
    fun storesMissingOptionalReadingFieldsAsNull() = runTest {
        val source = FakeBatteryDataSource(
            reading = reading(
                levelPercent = null,
                temperatureCelsius = null,
                voltageMillivolts = null
            )
        )

        val result = collector(source, InMemoryBatteryPingDao()).collectAndStorePing()

        assertNull(result.levelPercent)
        assertNull(result.temperatureCelsius)
        assertNull(result.voltageMillivolts)
        assertEquals(BatterySignalParser.LEVEL_NONE, result.levelCategory)
        assertEquals(BatterySignalParser.STATUS_SUCCESS, result.status)
    }

    @Test
    fun recordsUnavailableWhenSystemHasNoBatteryInformation() = runTest {
        val source = FakeBatteryDataSource(reading = null)
        val dao = InMemoryBatteryPingDao()

        val result = collector(source, dao).collectAndStorePing()

        assertEquals(BatterySignalParser.STATUS_UNAVAILABLE, result.status)
        assertEquals(1, source.readCount)
        assertStoredUnreachable(dao, result)
    }

    @Test
    fun mapsDataSourceFailureToErrorRecord() = runTest {
        val source = FakeBatteryDataSource()
        source.read = { throw IllegalStateException("battery service failed") }
        val dao = InMemoryBatteryPingDao()

        val result = collector(source, dao).collectAndStorePing()

        assertEquals(BatterySignalParser.STATUS_ERROR, result.status)
        assertStoredUnreachable(dao, result)
    }

    @Test
    fun propagatesCancellationInsteadOfStoringAnError() = runTest {
        val source = FakeBatteryDataSource()
        source.read = { throw CancellationException("cancelled") }
        val dao = InMemoryBatteryPingDao()

        var cancelled = false
        try {
            collector(source, dao).collectAndStorePing()
        } catch (_: CancellationException) {
            cancelled = true
        }

        assertTrue(cancelled)
        assertTrue(dao.pings.isEmpty())
        assertNull(dao.lastPurgeCutoff)
    }

    @Test
    fun storesEveryPingAsUnsyncedUntilMarkedSynced() = runTest {
        val dao = InMemoryBatteryPingDao()
        var clock = NOW
        val collector = BatteryCollector(
            dataSource = FakeBatteryDataSource(reading = reading()),
            dao = dao,
            nowMillis = { clock }
        )

        val first = collector.collectAndStorePing()
        clock += 60_000L
        val second = collector.collectAndStorePing()

        assertEquals(listOf(first.timestamp, second.timestamp), dao.getUnsyncedPings().map { it.timestamp })

        dao.markPingsAsSynced(listOf(dao.pings.first().id))

        assertEquals(listOf(second.timestamp), dao.getUnsyncedPings().map { it.timestamp })
    }

    @Test
    fun purgesRowsUsingTheThirtyDayCutoffAfterCollection() = runTest {
        val dao = InMemoryBatteryPingDao()
        val source = FakeBatteryDataSource(reading = reading())

        collector(source, dao).collectAndStorePing()

        assertEquals(
            NOW - AppDatabase.RETENTION_PERIOD_MILLIS,
            dao.lastPurgeCutoff
        )
    }

    private fun collector(
        source: FakeBatteryDataSource,
        dao: InMemoryBatteryPingDao
    ) = BatteryCollector(
        dataSource = source,
        dao = dao,
        nowMillis = { NOW }
    )

    private fun reading(
        levelPercent: Int? = 50,
        isCharging: Boolean = false,
        plugType: String = "NONE",
        health: String = "GOOD",
        temperatureCelsius: Float? = 25f,
        voltageMillivolts: Int? = 4000,
        isPowerSaveMode: Boolean = false
    ) = BatteryReading(
        levelPercent = levelPercent,
        isCharging = isCharging,
        plugType = plugType,
        health = health,
        temperatureCelsius = temperatureCelsius,
        voltageMillivolts = voltageMillivolts,
        isPowerSaveMode = isPowerSaveMode
    )

    private fun assertStoredUnreachable(
        dao: InMemoryBatteryPingDao,
        result: BatteryPingEntity
    ) {
        assertEquals(result, dao.pings.single().copy(id = result.id))
        assertEquals(NOW, result.timestamp)
        assertNull(result.levelPercent)
        assertNull(result.isCharging)
        assertNull(result.plugType)
        assertNull(result.health)
        assertNull(result.temperatureCelsius)
        assertNull(result.voltageMillivolts)
        assertNull(result.isPowerSaveMode)
        assertEquals(BatterySignalParser.LEVEL_NONE, result.levelCategory)
        assertFalse(result.isSynced)
    }

    private class FakeBatteryDataSource(
        private val reading: BatteryReading? = null
    ) : BatteryDataSource {
        var readCount = 0
            private set
        var read: () -> BatteryReading? = { reading }

        override fun getCurrentReading(): BatteryReading? {
            readCount++
            return read()
        }
    }

    private companion object {
        const val NOW = 1_700_000_000_000L
    }
}