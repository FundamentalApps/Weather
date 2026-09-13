package org.fundamentalos.weather.ui.sky

import androidx.compose.runtime.Immutable
import kotlin.math.cos
import kotlin.math.sin

/** Normalized atmosphere shared by every renderer. Wind is velocity, not a changing random seed. */
@Immutable
data class SkyState(
    val sunAltitude: Float = 30f,
    val sunProgress: Float = 0.4f,
    val cloudCover: Float = 0.12f,
    val precipitation: Float = 0f,
    val frozen: Float = 0f,
    val haze: Float = 0f,
    val dust: Float = 0f,
    val windX: Float = 0.3f,
    val windY: Float = 0f,
    val moonIllumination: Float = 0.5f,
    val cumulus: Float = 0f,
    val storm: Float = 0f,
) {
    /**
     * The sky part way from this one to [other]. The sun's height is mixed in the sky's own
     * terms: what shows of it is the band about the horizon where day becomes night, so a
     * change that crosses the band — a night here to a day there — spends most of its time in
     * it, and the easing on [fraction] is seen, rather than spending most of its time high in
     * the day or deep in the night where nothing changes and crossing the band in a moment.
     */
    fun interpolate(other: SkyState, fraction: Float): SkyState {
        val t = fraction.coerceIn(0f, 1f)
        // The ends exactly: the sun's warp and its inverse do not quite cancel in floats.
        if (t <= 0f) return this
        if (t >= 1f) return other
        fun mix(a: Float, b: Float) = a + (b - a) * t
        val sunAltitude = unwarp(mix(warp(sunAltitude), warp(other.sunAltitude)))
        return SkyState(sunAltitude, mix(sunProgress, other.sunProgress),
            mix(cloudCover, other.cloudCover), mix(precipitation, other.precipitation),
            mix(frozen, other.frozen), mix(haze, other.haze), mix(dust, other.dust),
            mix(windX, other.windX), mix(windY, other.windY), mix(moonIllumination, other.moonIllumination),
            mix(cumulus, other.cumulus), mix(storm, other.storm))
    }

    companion object {
        /** The band of sun heights, in degrees, over which the renderers turn night into day. */
        private const val BandLow = -8f
        private const val BandHigh = 14f

        /** How much a degree outside the band counts against one inside it. */
        private const val Tail = 0.15f

        /** Sun height in the sky's terms: the band at full size, the rest compressed. */
        private fun warp(altitude: Float): Float = when {
            altitude < BandLow -> BandLow + (altitude - BandLow) * Tail
            altitude > BandHigh -> BandHigh + (altitude - BandHigh) * Tail
            else -> altitude
        }

        private fun unwarp(warped: Float): Float = when {
            warped < BandLow -> BandLow + (warped - BandLow) / Tail
            warped > BandHigh -> BandHigh + (warped - BandHigh) / Tail
            else -> warped
        }

        /** Meteorological bearings describe where wind comes FROM. */
        fun wind(speedKph: Int?, bearing: Int?): Pair<Float, Float> {
            val speed = ((speedKph ?: 8).coerceIn(0, 100) / 40f)
            val radians = Math.toRadians((bearing ?: 270).toDouble())
            return (-sin(radians).toFloat() * speed) to (cos(radians).toFloat() * speed)
        }
    }
}

/** Active time is accumulated across pauses; a toggle never resets the cloud phase. */
class SkyAnimationClock {
    private var lastFrame: Long? = null
    var elapsedNanos: Long = 0L
        private set

    fun pause() { lastFrame = null }
    fun advance(frameNanos: Long): Long {
        lastFrame?.let { elapsedNanos += (frameNanos - it).coerceAtLeast(0L) }
        lastFrame = frameNanos
        return elapsedNanos
    }
}
