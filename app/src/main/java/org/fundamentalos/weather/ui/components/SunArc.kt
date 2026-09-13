package org.fundamentalos.weather.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fundamentalos.weather.ui.theme.PreviewThemeWithBg
import kotlin.math.PI
import kotlin.math.sin

private const val MinutesPerDay = 24 * 60

/** Share of the tile's height given to the daylight lobe; the rest is night. */
private const val HorizonFraction = 0.68f

/** Breathing room kept at the very top and bottom so the curve's peaks are not cut. */
private val EdgeMargin = 2.dp

/** How many segments the curve is sampled into. */
private const val Segments = 72

/**
 * The sun's path across the local day: a lobe above the horizon between sunrise and sunset and a
 * matching one below it overnight, with a marker at the current time.
 *
 * The two lobes are built separately rather than as one sine wave, so the curve crosses the horizon
 * exactly at sunrise and sunset whatever the day length happens to be.
 */
@Composable
fun SunArc(
    sunriseMinutes: Int,
    sunsetMinutes: Int,
    nowMinutes: Int,
    modifier: Modifier = Modifier,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val dayColor = onSurface.copy(alpha = 0.45f)
    val nightColor = onSurface.copy(alpha = 0.18f)
    val horizonColor = onSurface.copy(alpha = 0.22f)
    val markerFill = MaterialTheme.colorScheme.surface
    val dayLength = (sunsetMinutes - sunriseMinutes).coerceAtLeast(1)
    val nightLength = (MinutesPerDay - dayLength).coerceAtLeast(1)

    /** Sun height at [minute] of the day, 1 at midday and -1 at solar midnight. */
    fun altitude(minute: Int): Float {
        val m = ((minute % MinutesPerDay) + MinutesPerDay) % MinutesPerDay
        return if (m in sunriseMinutes until sunsetMinutes) {
            sin(PI * (m - sunriseMinutes) / dayLength).toFloat()
        } else {
            val since = if (m >= sunsetMinutes) m - sunsetMinutes else m + MinutesPerDay - sunsetMinutes
            -sin(PI * since / nightLength).toFloat()
        }
    }

    Canvas(modifier.fillMaxSize().cardForegroundBlend()) {
        val horizonY = size.height * HorizonFraction
        val upAmplitude = horizonY - EdgeMargin.toPx()
        val downAmplitude = size.height - horizonY - EdgeMargin.toPx()

        fun pointAt(minute: Int, x: Float): Offset {
            val a = altitude(minute)
            return Offset(x, horizonY - a * if (a >= 0f) upAmplitude else downAmplitude)
        }

        // Two paths so the part below the horizon can read as dimmer without a gradient.
        val above = Path()
        val below = Path()
        var abovePenDown = false
        var belowPenDown = false
        for (i in 0..Segments) {
            val x = size.width * i / Segments
            val minute = MinutesPerDay * i / Segments
            val point = pointAt(minute, x)
            if (altitude(minute) >= 0f) {
                if (abovePenDown) above.lineTo(point.x, point.y) else above.moveTo(point.x, point.y)
                abovePenDown = true
                belowPenDown = false
            } else {
                if (belowPenDown) below.lineTo(point.x, point.y) else below.moveTo(point.x, point.y)
                belowPenDown = true
                abovePenDown = false
            }
        }

        drawLine(
            color = horizonColor,
            start = Offset(0f, horizonY),
            end = Offset(size.width, horizonY),
            strokeWidth = 1.dp.toPx(),
        )
        val stroke = Stroke(width = 2.5f.dp.toPx(), cap = StrokeCap.Round)
        drawPath(below, nightColor, style = stroke)
        drawPath(above, dayColor, style = stroke)

        val markerX = size.width * (nowMinutes.coerceIn(0, MinutesPerDay).toFloat() / MinutesPerDay)
        val marker = pointAt(nowMinutes, markerX)
        val radius = 5.dp.toPx()
        drawCircle(markerFill, radius, marker)
        drawCircle(onSurface.copy(alpha = 0.55f), radius, marker, style = Stroke(1.5f.dp.toPx()))
    }
}

/** "HH:MM" to minutes since local midnight, or null if it is not a time. */
fun parseClockMinutes(value: String?): Int? {
    val parts = value?.trim()?.split(":") ?: return null
    if (parts.size < 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].take(2).toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

@Preview
@Composable
private fun Preview() {
    PreviewThemeWithBg {
        SunArc(
            sunriseMinutes = 5 * 60 + 46,
            sunsetMinutes = 18 * 60 + 17,
            nowMinutes = 21 * 60 + 31,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
