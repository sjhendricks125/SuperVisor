package com.example.supervisor_client.collectors.location

import com.example.supervisor_client.data.local.LocationPingEntity

object LocationSignalParser {
    const val SIGNAL_HIGH = "HIGH"
    const val SIGNAL_MODERATE = "MODERATE"
    const val SIGNAL_POOR = "POOR"
    const val SIGNAL_NONE = "NONE"

    const val STATUS_SUCCESS = "SUCCESS"
    const val STATUS_LOCATION_DISABLED = "UNREACHABLE_LOCATION_DISABLED"
    const val STATUS_PERMISSION_DENIED = "UNREACHABLE_PERMISSION_DENIED"
    const val STATUS_TIMEOUT = "UNREACHABLE_TIMEOUT"
    const val STATUS_ERROR = "UNREACHABLE_ERROR"

    fun parseSignalQuality(accuracyMeters: Float?): String = when {
        accuracyMeters == null || accuracyMeters < 0f -> SIGNAL_NONE
        accuracyMeters < 10f -> SIGNAL_HIGH
        accuracyMeters <= 50f -> SIGNAL_MODERATE
        else -> SIGNAL_POOR
    }

    fun createSuccessEntity(
        latitude: Double,
        longitude: Double,
        altitude: Double?,
        accuracyRadius: Float?,
        timestampMillis: Long
    ): LocationPingEntity = LocationPingEntity(
        timestamp = timestampMillis,
        latitude = latitude,
        longitude = longitude,
        altitude = altitude,
        accuracyRadius = accuracyRadius,
        signalQuality = parseSignalQuality(accuracyRadius),
        status = STATUS_SUCCESS
    )

    fun createUnreachableEntity(
        status: String,
        timestampMillis: Long = System.currentTimeMillis()
    ): LocationPingEntity = LocationPingEntity(
        timestamp = timestampMillis,
        latitude = null,
        longitude = null,
        altitude = null,
        accuracyRadius = null,
        signalQuality = SIGNAL_NONE,
        status = status,
        isSynced = false
    )
}
