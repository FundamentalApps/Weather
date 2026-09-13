package org.fundamentalos.weather.ui.sky

import org.junit.Assert.*
import org.junit.Test

class SkyStateTest {
    @Test fun pauseDoesNotAccumulateBackgroundTimeOrResetPhase() {
        val clock = SkyAnimationClock()
        clock.advance(1_000_000_000)
        assertEquals(500_000_000L, clock.advance(1_500_000_000))
        clock.pause()
        assertEquals(500_000_000L, clock.advance(50_000_000_000))
        assertEquals(750_000_000L, clock.advance(50_250_000_000))
    }
    @Test fun backwardFrameTimestampDoesNotReverseMotion() {
        val clock = SkyAnimationClock()
        clock.advance(100)
        assertEquals(0L, clock.advance(90))
    }
    @Test fun windFromWestMovesEastAndCalmDoesNotDrift() {
        val (x, y) = SkyState.wind(40, 270)
        assertEquals(1f, x, .001f)
        assertEquals(0f, y, .001f)
        assertEquals(0f, SkyState.wind(0, 90).first, 0f)
    }
    @Test fun weatherTransitionIsContinuousAndBounded() {
        val a = SkyState(cloudCover = 0f, precipitation = 0f)
        val b = SkyState(cloudCover = 1f, precipitation = 1f)
        assertEquals(.5f, a.interpolate(b, .5f).cloudCover, .001f)
        assertEquals(a, a.interpolate(b, -1f))
        assertEquals(b, a.interpolate(b, 2f))
    }
    @Test fun nightToDaySpendsItsMiddleOnTheHorizon() {
        // Deep night to high noon: mixed by height alone the horizon would pass in a fifth of
        // the change; mixed in the sky's terms, the middle half of it is spent there.
        val night = SkyState(sunAltitude = -45f)
        val noon = SkyState(sunAltitude = 65f)
        val quarter = night.interpolate(noon, .25f).sunAltitude
        val half = night.interpolate(noon, .5f).sunAltitude
        val threeQuarters = night.interpolate(noon, .75f).sunAltitude
        assertTrue("$quarter", quarter in -8f..14f)
        assertTrue("$half", half in -8f..14f)
        assertTrue("$threeQuarters", threeQuarters in -8f..14f)
        assertTrue(quarter < half && half < threeQuarters)
        assertEquals(-45f, night.interpolate(noon, 0f).sunAltitude, 0f)
        assertEquals(65f, night.interpolate(noon, 1f).sunAltitude, 0f)
    }
}
