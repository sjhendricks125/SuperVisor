package com.example.supervisor_client.collectors.location

import com.example.supervisor_client.data.local.AppDatabase
import com.example.supervisor_client.data.local.InMemoryLocationPingDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocationCollectorStubTest {
    @Test
    fun doesNotRequestLocationWhenPermissionsAreDenied() = runTest {
        val source = FakeLocationDataSource(hasPermission = false)
        val dao = InMemoryLocationPingDao()

        val result = collector(source, dao).collectAndStorePing()

        assertEquals(LocationSignalParser.STATUS_PERMISSION_DENIED, result.status)
        assertEquals(0, source.requestCount)
        assertStoredUnreachable(dao, result)
    }

    @Test
    fun coarseLocationPermissionIsSufficientToRequestLocation() = runTest {
        assertFalse(hasAnyLocationPermission(false, false))
        assertTrue(hasAnyLocationPermission(false, true))
        assertTrue(hasAnyLocationPermission(true, false))

        val source = FakeLocationDataSource(
            hasPermission = hasAnyLocationPermission(false, true),
            location = LocationReading(1.0, 2.0, null, 5f)
        )
        val dao = InMemoryLocationPingDao()

        val result = collector(source, dao).collectAndStorePing()

        assertEquals(1, source.requestCount)
        assertEquals(LocationSignalParser.STATUS_SUCCESS, result.status)
    }

    @Test
    fun recordsDisabledLocationServicesWithoutRequestingLocation() = runTest {
        val source = FakeLocationDataSource(locationEnabled = false)
        val dao = InMemoryLocationPingDao()

        val result = collector(source, dao).collectAndStorePing()

        assertEquals(LocationSignalParser.STATUS_LOCATION_DISABLED, result.status)
        assertEquals(0, source.requestCount)
        assertStoredUnreachable(dao, result)
    }

    @Test
    fun storesReturnedLocationAndTimestamp() = runTest {
        val source = FakeLocationDataSource(
            location = LocationReading(
                latitude = 47.6062,
                longitude = -122.3321,
                altitude = 55.5,
                accuracyRadius = 10f
            )
        )
        val dao = InMemoryLocationPingDao()

        val result = collector(source, dao).collectAndStorePing()

        assertEquals(47.6062, result.latitude!!, 0.0)
        assertEquals(-122.3321, result.longitude!!, 0.0)
        assertEquals(55.5, result.altitude!!, 0.0)
        assertEquals(10f, result.accuracyRadius!!, 0f)
        assertEquals(LocationSignalParser.SIGNAL_MODERATE, result.signalQuality)
        assertEquals(LocationSignalParser.STATUS_SUCCESS, result.status)
        assertEquals(NOW, result.timestamp)
        assertFalse(result.isSynced)
        assertEquals(result, dao.pings.single().copy(id = result.id))
    }

    @Test
    fun storesMissingOptionalLocationFieldsAsNull() = runTest {
        val source = FakeLocationDataSource(
            location = LocationReading(1.0, 2.0, null, null)
        )
        val result = collector(source, InMemoryLocationPingDao()).collectAndStorePing()

        assertNull(result.altitude)
        assertNull(result.accuracyRadius)
        assertEquals(LocationSignalParser.SIGNAL_NONE, result.signalQuality)
    }

    @Test
    fun recordsTimeoutAndCancelsTheLocationRequest() = runTest {
        val source = FakeLocationDataSource()
        source.request = { awaitCancellation() }
        val dao = InMemoryLocationPingDao()

        val result = collector(source, dao, timeoutMillis = 1L).collectAndStorePing()

        assertEquals(LocationSignalParser.STATUS_TIMEOUT, result.status)
        assertEquals(1, source.requestCount)
        assertTrue(source.requestWasCancelled)
        assertStoredUnreachable(dao, result)
    }

    @Test
    fun mapsPermissionRaceAndProviderFailureToUnreachableRecords() = runTest {
        val permissionSource = FakeLocationDataSource()
        permissionSource.request = { throw SecurityException("permission revoked") }
        val permissionResult = collector(
            permissionSource,
            InMemoryLocationPingDao()
        ).collectAndStorePing()

        assertEquals(LocationSignalParser.STATUS_PERMISSION_DENIED, permissionResult.status)

        val errorSource = FakeLocationDataSource()
        errorSource.request = { throw IllegalStateException("provider failed") }
        val errorDao = InMemoryLocationPingDao()
        val errorResult = collector(errorSource, errorDao).collectAndStorePing()

        assertEquals(LocationSignalParser.STATUS_ERROR, errorResult.status)
        assertStoredUnreachable(errorDao, errorResult)
    }

    @Test
    fun propagatesExternalCancellationInsteadOfStoringAnError() = runTest {
        val source = FakeLocationDataSource()
        source.request = { awaitCancellation() }
        val dao = InMemoryLocationPingDao()

        val job = launch {
            collector(source, dao).collectAndStorePing()
        }
        runCurrent()
        job.cancel()
        job.join()

        assertTrue(job.isCancelled)
        assertTrue(source.requestWasCancelled)
        assertTrue(dao.pings.isEmpty())
        assertNull(dao.lastPurgeCutoff)
    }

    @Test
    fun cachesLocationAsUnsyncedWhenNetworkIsUnavailable() = runTest {
        val source = FakeLocationDataSource(
            networkAvailable = false,
            location = LocationReading(3.0, 4.0, 10.0, 8f)
        )
        val dao = InMemoryLocationPingDao()

        val result = collector(source, dao).collectAndStorePing()

        assertFalse(source.networkAvailable)
        assertEquals(LocationSignalParser.STATUS_SUCCESS, result.status)
        assertFalse(result.isSynced)
        assertEquals(result, dao.pings.single().copy(id = result.id))
    }

    @Test
    fun purgesRowsUsingTheThirtyDayCutoffAfterCollection() = runTest {
        val dao = InMemoryLocationPingDao()
        val source = FakeLocationDataSource(
            location = LocationReading(1.0, 2.0, null, 4f)
        )

        collector(source, dao).collectAndStorePing()

        assertEquals(
            NOW - AppDatabase.RETENTION_PERIOD_MILLIS,
            dao.lastPurgeCutoff
        )
    }

    private fun collector(
        source: FakeLocationDataSource,
        dao: InMemoryLocationPingDao,
        timeoutMillis: Long = 15_000L
    ) = LocationCollectorStub(
        locationDataSource = source,
        dao = dao,
        nowMillis = { NOW },
        locationTimeoutMillis = timeoutMillis
    )

    private fun assertStoredUnreachable(
        dao: InMemoryLocationPingDao,
        result: com.example.supervisor_client.data.local.LocationPingEntity
    ) {
        assertEquals(result, dao.pings.single().copy(id = result.id))
        assertEquals(NOW, result.timestamp)
        assertNull(result.latitude)
        assertNull(result.longitude)
        assertNull(result.altitude)
        assertNull(result.accuracyRadius)
        assertEquals(LocationSignalParser.SIGNAL_NONE, result.signalQuality)
        assertFalse(result.isSynced)
    }

    private class FakeLocationDataSource(
        private val hasPermission: Boolean = true,
        private val locationEnabled: Boolean = true,
        val networkAvailable: Boolean = true,
        private val location: LocationReading? = null
    ) : LocationDataSource {
        var requestCount = 0
            private set
        var requestWasCancelled = false
            private set
        var request: suspend () -> LocationReading? = { location }

        override fun hasLocationPermission(): Boolean = hasPermission

        override fun isLocationEnabled(): Boolean = locationEnabled

        override suspend fun getCurrentLocation(): LocationReading? {
            requestCount++
            return try {
                request()
            } finally {
                if (!currentCoroutineContext().isActive) {
                    requestWasCancelled = true
                }
            }
        }
    }

    private companion object {
        const val NOW = 1_700_000_000_000L
    }
}
