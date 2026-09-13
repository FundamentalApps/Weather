package org.fundamentalos.weather.ui.components

import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fundamentalos.weather.R
import org.fundamentalos.weather.ui.theme.PreviewThemeWithBg
import kotlin.math.cos
import kotlin.math.sin

/** Room kept outside the tick ring so a cardinal label centred on it still fits the dial. */
private val RingInset = 11.dp

/** Grows by the inset rather than eating into it, so the tick ring keeps its 116dp diameter. */
private val DialSize = 116.dp + RingInset * 2
private val TickLength = 7.dp

/** Half-width of the clear gap left at north, east, south and west. */
private const val CardinalGapDegrees = 12.0

/** Wind readouts beside a compass dial showing where the wind is blowing from. */
@Composable
fun WindCard(
    speedKph: Int?,
    gustKph: Int?,
    direction: String,
    degree: Int?,
    modifier: Modifier = Modifier,
) {
    InfoCard(
        modifier = modifier,
        icon = painterResource(R.drawable.warning_gale_24dp),
        category = stringResource(R.string.wind),
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.width(0.dp).then(Modifier).weight(1f)) {
                DetailRow(stringResource(R.string.wind), speedKph.asSpeed())
                DetailRowDivider()
                DetailRow(stringResource(R.string.gusts), gustKph.asSpeed())
                DetailRowDivider()
                DetailRow(
                    stringResource(R.string.direction),
                    buildString {
                        append(direction.ifBlank { "--" })
                        degree?.let { append(" $it°") }
                    },
                )
            }
            Spacer(Modifier.width(16.dp))
            WindDial(speedKph, degree, Modifier.size(DialSize))
        }
    }
}

@Composable
private fun Int?.asSpeed(): String = this?.let { stringResource(R.string.unit_speed, it) } ?: "--"

@Composable
private fun WindDial(speedKph: Int?, degree: Int?, modifier: Modifier = Modifier) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val tickColor = onSurface.copy(alpha = 0.22f)
    val cardinalColor = onSurface.copy(alpha = 0.4f)

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().cardForegroundBlend()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f - RingInset.toPx()

            // Ticks every 5 degrees, with a gap at each cardinal point so the labels and the arrow
            // sit in clear space rather than on top of a tick.
            repeat(72) { i ->
                val bearing = i * 5.0
                val offCardinal = (bearing % 90.0).let { minOf(it, 90.0 - it) }
                if (offCardinal < CardinalGapDegrees) return@repeat
                val angle = Math.toRadians(bearing - 90.0)
                val dx = cos(angle).toFloat()
                val dy = sin(angle).toFloat()
                val inner = radius - TickLength.toPx()
                drawLine(
                    color = tickColor,
                    start = center + Offset(dx * inner, dy * inner),
                    end = center + Offset(dx * radius, dy * radius),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round,
                )
            }

            degree?.let { deg ->
                // Meteorological convention: the value is the direction the wind comes from, and the
                // arrow points where it is going. The middle is left clear for the speed readout.
                val angle = Math.toRadians(deg.toDouble() - 90.0)
                val u = Offset(cos(angle).toFloat(), sin(angle).toFloat())
                val outer = radius * 0.78f
                val inner = outer * 0.52f
                drawLine(
                    color = onSurface,
                    start = center + u * outer,
                    end = center + u * inner,
                    strokeWidth = 5f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = onSurface,
                    start = center - u * inner,
                    end = center - u * (outer - 9f),
                    strokeWidth = 5f,
                    cap = StrokeCap.Round,
                )
                val tip = center - u * outer
                val side = Offset(-u.y, u.x)
                drawPath(
                    Path().apply {
                        moveTo(tip.x, tip.y)
                        val base = center - u * (outer - 13f)
                        lineTo(base.x + side.x * 7f, base.y + side.y * 7f)
                        lineTo(base.x - side.x * 7f, base.y - side.y * 7f)
                        close()
                    },
                    color = onSurface,
                )
            }
        }

        val labelRadius = DialSize / 2 - RingInset - TickLength / 2
        Cardinal(stringResource(R.string.north), 0.dp, -labelRadius, cardinalColor)
        Cardinal(stringResource(R.string.east), labelRadius, 0.dp, cardinalColor)
        Cardinal(stringResource(R.string.south), 0.dp, labelRadius, cardinalColor)
        Cardinal(stringResource(R.string.west), -labelRadius, 0.dp, cardinalColor)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.cardForegroundBlend(),
        ) {
            Text(
                text = speedKph?.toString() ?: "--",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = onSurface,
            )
            Text(
                text = stringResource(R.string.speed_unit),
                style = MaterialTheme.typography.labelSmall,
                color = cardinalColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Centred on the tick band at [dx], [dy] from the middle, so it fills the gap in the ring. */
@Composable
private fun BoxScope.Cardinal(text: String, dx: Dp, dy: Dp, color: Color) {
    Text(
        text = text,
        modifier = Modifier
            .align(Alignment.Center)
            .offset(x = dx, y = dy)
            .cardForegroundBlend(),
        style = MaterialTheme.typography.labelMedium,
        color = color,
    )
}

@Preview
@Composable
private fun Preview() {
    PreviewThemeWithBg {
        WindCard(speedKph = 11, gustKph = 25, direction = "北风", degree = 359, modifier = Modifier.padding(16.dp))
    }
}
