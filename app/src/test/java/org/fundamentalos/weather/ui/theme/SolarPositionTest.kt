package org.fundamentalos.weather.ui.theme

import java.time.OffsetDateTime
import java.time.ZoneOffset
import org.junit.Assert.*
import org.junit.Test

class SolarPositionTest {
    @Test fun sameInstantAndCoordinatesIgnoreDeviceTimezone() {
        val utc = OffsetDateTime.parse("2026-06-21T12:00:00Z")
        val local = utc.withOffsetSameInstant(ZoneOffset.ofHours(8))
        assertEquals(solarPosition(utc, 37.3, -122.0), solarPosition(local, 37.3, -122.0))
    }
    @Test fun longitudeChangesSolarAltitudeAndDayProgress() {
        val instant = OffsetDateTime.parse("2026-03-20T12:00:00Z")
        val west = solarPosition(instant, 0.0, -90.0)
        val noon = solarPosition(instant, 0.0, 0.0)
        val east = solarPosition(instant, 0.0, 90.0)
        assertTrue(noon.altitude > 85.0)
        assertTrue(kotlin.math.abs(west.altitude) < 5.0)
        assertTrue(kotlin.math.abs(east.altitude) < 5.0)
        assertTrue(west.progress < noon.progress)
        assertTrue(noon.progress < east.progress)
    }
    @Test fun polarDayAndNightRemainFinite() {
        for (date in listOf("2026-06-21T12:00:00Z", "2026-12-21T12:00:00Z")) {
            val position = solarPosition(OffsetDateTime.parse(date), 89.0, 0.0)
            assertTrue(position.altitude.isFinite())
            assertTrue(position.progress in 0.0..1.0)
        }
    }
}
