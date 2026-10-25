package com.example.supervisor_client.collectors.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.example.supervisor_client.collectors.DataCollector
import com.example.supervisor_client.data.local.AppDatabase
import com.example.supervisor_client.data.local.LocationPingDao
import com.example.supervisor_client.data.local.LocationPingEntity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class LocationCollectorStub(
    private val context: Context,
    private val dao: LocationPingDao
) : DataCollector {
    private val fusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context.applicationContext)
    }

    suspend fun collectAndStorePing(): LocationPingEntity {
        val hasFineLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val entity = when {
            !hasFineLocationPermission && !hasCoarseLocationPermission ->
                LocationSignalParser.createUnreachableEntity(
                    LocationSignalParser.STATUS_PERMISSION_DENIED
                )

            else -> {
                val locationManager =
                    context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                if (!LocationManagerCompat.isLocationEnabled(locationManager)) {
                    LocationSignalParser.createUnreachableEntity(
                        LocationSignalParser.STATUS_LOCATION_DISABLED
                    )
                } else {
                    try {
                        val cancellationTokenSource = CancellationTokenSource()
                        val location = try {
                            withTimeoutOrNull(15_000L) {
                                fusedLocationProviderClient.getCurrentLocation(
                                    Priority.PRIORITY_HIGH_ACCURACY,
                                    cancellationTokenSource.token
                                ).await()
                            }
                        } finally {
                            cancellationTokenSource.cancel()
                        }

                        if (location == null) {
                            LocationSignalParser.createUnreachableEntity(
                                LocationSignalParser.STATUS_TIMEOUT
                            )
                        } else {
                            LocationSignalParser.createSuccessEntity(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                altitude = if (location.hasAltitude()) location.altitude else null,
                                accuracyRadius =
                                    if (location.hasAccuracy()) location.accuracy else null,
                                timestampMillis = System.currentTimeMillis()
                            )
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: SecurityException) {
                        LocationSignalParser.createUnreachableEntity(
                            LocationSignalParser.STATUS_PERMISSION_DENIED
                        )
                    } catch (_: Exception) {
                        LocationSignalParser.createUnreachableEntity(
                            LocationSignalParser.STATUS_ERROR
                        )
                    }
                }
            }
        }

        try {
            dao.insertPing(entity)
        } finally {
            dao.purgePingsOlderThan(AppDatabase.thirtyDayCutoffMillis())
        }
        return entity
    }
}
