package com.example.supervisor_client.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class MonitoringConfigStoreTest {
    @Test
    fun normalizesPingIntervalsToAtLeastOneMinute() {
        assertEquals(1L, MonitoringConfigStore.normalizePingIntervalMinutes(0L))
        assertEquals(1L, MonitoringConfigStore.normalizePingIntervalMinutes(-5L))
        assertEquals(7L, MonitoringConfigStore.normalizePingIntervalMinutes(7L))
    }
}
