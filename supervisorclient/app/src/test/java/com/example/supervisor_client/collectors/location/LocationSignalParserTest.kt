package com.example.supervisor_client.collectors.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class LocationSignalParserTest {
    @Test
    fun parsesSignalQualityAtEveryBoundary() {
        val cases = listOf(
            null to LocationSignalParser.SIGNAL_NONE,
            -0.01f to LocationSignalParser.SIGNAL_NONE,
            0f to LocationSignalParser.SIGNAL_HIGH,
            9.999f to LocationSignalParser.SIGNAL_HIGH,
            10f to LocationSignalParser.SIGNAL_MODERATE,
            50f to LocationSignalParser.SIGNAL_MODERATE,
            50.001f to LocationSignalParser.SIGNAL_POOR
        )

        cases.forEach { (accuracy, expected) ->
            assertEquals(
                "Unexpected signal quality for accuracy=$accuracy",
                expected,
                LocationSignalParser.parseSignalQuality(accuracy)
            )
        }
    }

    @Test
    fun createsSuccessRecordWithProvidedLocationData() {
        val entity = LocationSignalParser.createSuccessEntity(
            latitude = 47.6062,
            longitude = -122.3321,
            altitude = 55.5,
            accuracyRadius = 12f,
            timestampMillis = 1234L
        )

        assertEquals(1234L, entity.timestamp)
        assertEquals(47.6062, entity.latitude!!, 0.0)
        assertEquals(-122.3321, entity.longitude!!, 0.0)
        assertEquals(55.5, entity.altitude!!, 0.0)
        assertEquals(12f, entity.accuracyRadius!!, 0f)
        assertEquals(LocationSignalParser.SIGNAL_MODERATE, entity.signalQuality)
        assertEquals(LocationSignalParser.STATUS_SUCCESS, entity.status)
        assertFalse(entity.isSynced)
    }

    @Test
    fun createsUnreachableRecordsWithBlankLocationData() {
        val statuses = listOf(
            LocationSignalParser.STATUS_PERMISSION_DENIED,
            LocationSignalParser.STATUS_LOCATION_DISABLED,
            LocationSignalParser.STATUS_TIMEOUT,
            LocationSignalParser.STATUS_ERROR
        )

        statuses.forEach { status ->
            val entity = LocationSignalParser.createUnreachableEntity(
                status = status,
                timestampMillis = 5678L
            )

            assertEquals(status, entity.status)
            assertEquals(5678L, entity.timestamp)
            assertNull(entity.latitude)
            assertNull(entity.longitude)
            assertNull(entity.altitude)
            assertNull(entity.accuracyRadius)
            assertEquals(LocationSignalParser.SIGNAL_NONE, entity.signalQuality)
            assertFalse(entity.isSynced)
        }
    }
}
