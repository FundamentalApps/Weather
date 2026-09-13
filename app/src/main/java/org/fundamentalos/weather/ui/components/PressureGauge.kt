package org.fundamentalos.weather.ui.components

import org.fundamentalos.weather.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fundamentalos.weather.ui.theme.PreviewThemeWithBg
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Range the dial spans, in hPa: sea-level pressure essentially never leaves it. */
private const val MinPressure = 950f
private const val MaxPressure = 1050f

/** The dial opens at the bottom: it starts at the lower left and sweeps clockwise to the lower right. */
private const val StartAngle = 140f
private const val SweepAngle = 260f
private const val TickCount = 44

private val Trimmed = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both,
)

/**
 * Pressure as a tick dial: the ticks up to the current reading are lit, the rest are dim, and the
 * reading sits in the middle with 低 and 高 marking the ends of the range.
 */
@Composable
fun PressureGauge(pressureHpa: Int, modifier: Modifier = Modifier) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val litColor = onSurface.copy(alpha = 0.55f)
    val dimColor = onSurface.copy(alpha = 0.18f)
    val fraction = ((pressureHpa - MinPressure) / (MaxPressure - MinPressure)).coerceIn(0f, 1f)

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().cardForegroundBlend()) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val markerIndex = (fraction * TickCount).roundToInt()

            repeat(TickCount + 1) { i ->
                val angle = Math.toRadians((StartAngle + SweepAngle * i / TickCount).toDouble())
                val dx = cos(angle).toFloat()
                val dy = sin(angle).toFloat()
                val marker = i == markerIndex
                val length = if (marker) 15f.dp.toPx() else 9f.dp.toPx()
                drawLine(
                    color = when {
                        marker -> onSurface
                        i <= markerIndex -> litColor
                        else -> dimColor
                    },
                    start = center + Offset(dx * (radius - length), dy * (radius - length)),
                    end = center + Offset(dx * radius, dy * radius),
                    strokeWidth = if (marker) 4f.dp.toPx() else 2.5f.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = pressureHpa.toString(),
                modifier = Modifier.cardForegroundBlend(),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    lineHeightStyle = Trimmed,
                ),
                color = onSurface,
            )
            Text(
                text = stringResource(R.string.pressure_unit),
                modifier = Modifier.cardForegroundBlend(),
                style = MaterialTheme.typography.labelMedium.copy(lineHeightStyle = Trimmed),
                color = onSurface.copy(alpha = 0.4f),
            )
        }

        // The dial's gap is at the bottom, so the range labels sit in it.
        Text(
            text = stringResource(R.string.low),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 6.dp)
                .cardForegroundBlend(),
            style = MaterialTheme.typography.labelMedium,
            color = onSurface.copy(alpha = 0.4f),
        )
        Text(
            text = stringResource(R.string.high),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 6.dp)
                .cardForegroundBlend(),
            style = MaterialTheme.typography.labelMedium,
            color = onSurface.copy(alpha = 0.4f),
        )
    }
}

@Preview
@Composable
private fun Preview() {
    PreviewThemeWithBg {
        Box(Modifier.fillMaxWidth().padding(24.dp)) {
            PressureGauge(1022, Modifier.fillMaxSize())
        }
    }
}
