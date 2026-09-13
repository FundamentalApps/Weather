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
}
