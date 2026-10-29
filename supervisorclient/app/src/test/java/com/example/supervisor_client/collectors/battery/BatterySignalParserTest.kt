package com.example.supervisor_client.collectors.battery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class BatterySignalParserTest {
    @Test
    fun parsesLevelCategoryAtEveryBoundary() {
        val cases = listOf(
            null to BatterySignalParser.LEVEL_NONE,
            -1 to BatterySignalParser.LEVEL_NONE,
            0 to BatterySignalParser.LEVEL_CRITICAL,
            5 to BatterySignalParser.LEVEL_CRITICAL,
            6 to BatterySignalParser.LEVEL_LOW,
            20 to BatterySignalParser.LEVEL_LOW,
            21 to BatterySignalParser.LEVEL_MODERATE,
            50 to BatterySignalParser.LEVEL_MODERATE,
            51 to BatterySignalParser.LEVEL_HIGH,
            100 to BatterySignalParser.LEVEL_HIGH
        )

        cases.forEach { (percent, expected) ->
            assertEquals(
                "Unexpected level category for percent=$percent",
                expected,
                BatterySignalParser.parseLevelCategory(percent)
            )
        }
    }

    @Test
    fun createsSuccessRecordWithProvidedBatteryData() {
        val entity = BatterySignalParser.createSuccessEntity(
            reading = BatteryReading(
                levelPercent = 80,
                isCharging = true,
                plugType = "AC",
                health = "GOOD",
                temperatureCelsius = 25.3f,
                voltageMillivolts = 4050,
                isPowerSaveMode = true
            ),
            timestampMillis = 1234L
        )

        assertEquals(1234L, entity.timestamp)
        assertEquals(80, entity.levelPercent)
        assertEquals(true, entity.isCharging)
        assertEquals("AC", entity.plugType)
        assertEquals("GOOD", entity.health)
        assertEquals(25.3f, entity.temperatureCelsius!!, 0f)
        assertEquals(4050, entity.voltageMillivolts)
        assertEquals(true, entity.isPowerSaveMode)
        assertEquals(BatterySignalParser.LEVEL_HIGH, entity.levelCategory)
        assertEquals(BatterySignalParser.STATUS_SUCCESS, entity.status)
        assertFalse(entity.isSynced)
    }

    @Test
    fun createsSuccessRecordWithBlankOptionalFieldsWhenReadingLacksThem() {
        val entity = BatterySignalParser.createSuccessEntity(
            reading = BatteryReading(
                levelPercent = null,
                isCharging = false,
                plugType = "NONE",
                health = "UNKNOWN",
                temperatureCelsius = null,
                voltageMillivolts = null,
                isPowerSaveMode = false
            ),
            timestampMillis = 99L
        )

        assertNull(entity.levelPercent)
        assertNull(entity.temperatureCelsius)
        assertNull(entity.voltageMillivolts)
        assertEquals(BatterySignalParser.LEVEL_NONE, entity.levelCategory)
        assertEquals(BatterySignalParser.STATUS_SUCCESS, entity.status)
    }

    @Test
    fun createsUnreachableRecordsWithBlankBatteryData() {
        val statuses = listOf(
            BatterySignalParser.STATUS_UNAVAILABLE,
            BatterySignalParser.STATUS_ERROR
        )

        statuses.forEach { status ->
            val entity = BatterySignalParser.createUnreachableEntity(
                status = status,
                timestampMillis = 5678L
            )

            assertEquals(status, entity.status)
            assertEquals(5678L, entity.timestamp)
            assertNull(entity.levelPercent)
            assertNull(entity.isCharging)
            assertNull(entity.plugType)
            assertNull(entity.health)
            assertNull(entity.temperatureCelsius)
            assertNull(entity.voltageMillivolts)
            assertNull(entity.isPowerSaveMode)
            assertEquals(BatterySignalParser.LEVEL_NONE, entity.levelCategory)
            assertFalse(entity.isSynced)
        }
    }
}