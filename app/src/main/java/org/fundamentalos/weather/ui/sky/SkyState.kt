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
    fun interpolate(other: SkyState, fraction: Float): SkyState {
        val t = fraction.coerceIn(0f, 1f)
        fun mix(a: Float, b: Float) = a + (b - a) * t
        return SkyState(mix(sunAltitude, other.sunAltitude), mix(sunProgress, other.sunProgress),
            mix(cloudCover, other.cloudCover), mix(precipitation, other.precipitation),
            mix(frozen, other.frozen), mix(haze, other.haze), mix(dust, other.dust),
            mix(windX, other.windX), mix(windY, other.windY), mix(moonIllumination, other.moonIllumination),
            mix(cumulus, other.cumulus), mix(storm, other.storm))
    }

    companion object {
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
