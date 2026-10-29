package com.example.supervisor_client.collectors.location

import android.content.Context
import com.example.supervisor_client.collectors.DataCollector
import com.example.supervisor_client.data.local.AppDatabase
import com.example.supervisor_client.data.local.LocationPingDao
import com.example.supervisor_client.data.local.LocationPingEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

class LocationCollectorStub internal constructor(
    private val locationDataSource: LocationDataSource,
    private val dao: LocationPingDao,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val locationTimeoutMillis: Long = LOCATION_TIMEOUT_MILLIS
) : DataCollector {
    constructor(context: Context, dao: LocationPingDao) : this(
        AndroidLocationDataSource(context),
        dao
    )

    init {
        require(locationTimeoutMillis > 0L)
    }

    suspend fun collectAndStorePing(): LocationPingEntity {
        val entity = when {
            !locationDataSource.hasLocationPermission() ->
                LocationSignalParser.createUnreachableEntity(
                    LocationSignalParser.STATUS_PERMISSION_DENIED,
                    nowMillis()
                )

            !locationDataSource.isLocationEnabled() ->
                LocationSignalParser.createUnreachableEntity(
                    LocationSignalParser.STATUS_LOCATION_DISABLED,
                    nowMillis()
                )

            else -> {
                try {
                    val location = withTimeoutOrNull(locationTimeoutMillis) {
                        locationDataSource.getCurrentLocation()
                    }

                    if (location == null) {
                        LocationSignalParser.createUnreachableEntity(
                            LocationSignalParser.STATUS_TIMEOUT,
                            nowMillis()
                        )
                    } else {
                        LocationSignalParser.createSuccessEntity(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            altitude = location.altitude,
                            accuracyRadius = location.accuracyRadius,
                            timestampMillis = nowMillis()
                        )
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: SecurityException) {
                    LocationSignalParser.createUnreachableEntity(
                        LocationSignalParser.STATUS_PERMISSION_DENIED,
                        nowMillis()
                    )
                } catch (_: Exception) {
                    LocationSignalParser.createUnreachableEntity(
                        LocationSignalParser.STATUS_ERROR,
                        nowMillis()
                    )
                }
            }
        }

        try {
            dao.insertPing(entity)
        } finally {
            dao.purgePingsOlderThan(
                AppDatabase.thirtyDayCutoffMillis(nowMillis())
            )
        }
        return entity
    }

    private companion object {
        const val LOCATION_TIMEOUT_MILLIS = 15_000L
    }
}
