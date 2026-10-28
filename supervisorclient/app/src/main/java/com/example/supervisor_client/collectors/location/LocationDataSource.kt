package com.example.supervisor_client.collectors.location

internal data class LocationReading(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
    val accuracyRadius: Float?
)

internal interface LocationDataSource {
    fun hasLocationPermission(): Boolean

    fun isLocationEnabled(): Boolean

    suspend fun getCurrentLocation(): LocationReading?
}

internal fun hasAnyLocationPermission(
    hasFineLocationPermission: Boolean,
    hasCoarseLocationPermission: Boolean
): Boolean = hasFineLocationPermission || hasCoarseLocationPermission
