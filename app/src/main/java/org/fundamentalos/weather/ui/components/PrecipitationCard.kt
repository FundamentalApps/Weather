package org.fundamentalos.weather.ui.components

import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fundamentalos.weather.R
import androidx.compose.ui.res.painterResource
import org.fundamentalos.weather.ui.theme.PreviewTheme
import org.fundamentalos.weather.ui.theme.harmonized
import org.fundamentalos.weather.ui.theme.rain
import org.fundamentalos.weather.weather.domain.MinutelyInterval
import org.fundamentalos.weather.weather.domain.MinutelyPrecipitation
import org.fundamentalos.weather.weather.domain.PrecipitationType
import kotlin.time.Clock
import kotlin.math.max

// 3 gridlines divide the chart into 4 bands (小 / 中 / 大 / 暴雨).
// The top band has no gridline — bars that reach it signal storm-level rain.
// Thresholds from Chinese national standard hourly levels scaled to 5-min intervals:
//   小→中: 2.5 mm/h = 0.21 mm/5min  →  1/4 height (1st gridline)
//   中→大: 8.0 mm/h = 0.67 mm/5min  →  2/4 height (2nd gridline)
//   大→暴: 16  mm/h = 1.33 mm/5min  →  3/4 height (3rd gridline)
//   暴雨上限: 32 mm/h = 2.67 mm/5min  →  chart ceiling
private val PRECIP_SCALE = Pchip(
    xs = doubleArrayOf(0.0, 0.21, 0.67, 1.33, 2.67, 10.0),
    ys = doubleArrayOf(0.0, 0.25, 0.50, 0.75, 1.00, 1.00),
)

@Composable
fun PrecipitationCard(data: MinutelyPrecipitation) {
    val hasSnow = data.intervals.any { it.type == PrecipitationType.Snow && it.precipMillimeters > 0 }
    InfoCard(
        icon = painterResource(
            if (hasSnow) R.drawable.ic_precipitation_snow_24dp else R.drawable.ic_precipitation_rain_24dp
        ),
        category = stringResource(R.string.precipitation),
        title = titleFor(data),
        subtitle = data.summary.takeIf { it.isNotBlank() },
    ) {
        Chart(
            intervals = data.intervals,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
        )
    }
}

@Composable
private fun titleFor(data: MinutelyPrecipitation): String {
    val hasSnow = data.intervals.any { it.type == PrecipitationType.Snow && it.precipMillimeters > 0 }
    val hasRain = data.intervals.any { it.type == PrecipitationType.Rain && it.precipMillimeters > 0 }
    return when {
        hasSnow && hasRain -> stringResource(R.string.rain_snow_expected)
        hasSnow -> stringResource(R.string.snow_expected)
        hasRain -> stringResource(R.string.rain_expected)
        else -> stringResource(R.string.no_precipitation)
    }
}

private val LineThickness = 1.dp
private val LevelHeight = 21.dp
private val TickHeight = 5.dp
private val BarGap = 4.dp
private const val ChartLevels = 4

@Composable
private fun Chart(intervals: List<MinutelyInterval>, modifier: Modifier) {
    val density = LocalDensity.current
    // Ink derived from the card's own foreground rather than from container roles: a light card
    // turns those into near-white lines, which is what made the axis vanish while the guides stayed.
    val onCard = MaterialTheme.colorScheme.onSurface
    val gridColor = onCard.copy(alpha = 0.10f)
    val axisColor = onCard.copy(alpha = 0.28f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val barColor = rain.harmonized()

    val textMeasurer = rememberTextMeasurer()
    val textStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
    val text0 = stringResource(R.string.now)
    val label0 = textMeasurer.measure(text0, textStyle)
    val text30 = stringResource(R.string.in_30_minutes)
    val label30 = textMeasurer.measure(text30, textStyle)
    val text60 = stringResource(R.string.in_1_hour)
    val label60 = textMeasurer.measure(text60, textStyle)
    val text120 = stringResource(R.string.in_2_hours)
    val label120 = textMeasurer.measure(text120, textStyle)

    val textHeight = with(density) {
        maxOf(label0.size.height, label30.size.height, label60.size.height, label120.size.height).toDp()
    }
    val height = LineThickness + (LevelHeight * ChartLevels) + TickHeight + textHeight

    // Pad to at least 24 slots so a short response still spans the full 2-hour axis.
    val slots = max(24, intervals.size)

    Canvas(
        modifier.height(height)
    ) {
        val levelHeight = LevelHeight.toPx()
        val p = LineThickness.toPx() / 2
        val chartTopY = p
        val bottomLineY = chartTopY + levelHeight * ChartLevels
        val maxBarHeight = bottomLineY - chartTopY

        val stride = size.width / slots
        val barWidth = (stride - BarGap.toPx()).coerceAtLeast(2.dp.toPx())
        val cornerR = 2.5.dp.toPx()

        // Three fixed threshold gridlines: 小雨 / 中雨 / 大雨.
        // Bar scaling shares the same four-band chart height, so 0.25/0.50/0.75
        // lands exactly on these background guide lines.
        (1 until ChartLevels).forEach { levelFromBottom ->
            val y = bottomLineY - levelHeight * levelFromBottom
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = LineThickness.toPx(),
            )
        }

        intervals.forEachIndexed { index, interval ->
            val weight = PRECIP_SCALE.evaluate(interval.precipMillimeters).coerceIn(0.0, 1.0).toFloat()
            if (weight <= 0f) return@forEachIndexed

            val barHeight = maxBarHeight * weight
            val centerX = stride * index + stride / 2
            val left = centerX - barWidth / 2
            val right = left + barWidth
            val topY = (bottomLineY - barHeight).coerceAtLeast(chartTopY)
            val r = cornerR.coerceAtMost((bottomLineY - topY) / 2).coerceAtMost(barWidth / 2)

            val path = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(left, topY, right, bottomLineY),
                        topLeft = CornerRadius(r),
                        topRight = CornerRadius(r),
                    )
                )
            }
            drawPath(path, barColor)
        }

        // Ticks + bottom line
        val tickHeight = TickHeight.toPx()
        val tickNowX = stride / 2
        val tick30X = stride * 6 + stride / 2
        val tick60X = stride * 12 + stride / 2
        val tick120X = size.width - stride / 2

        drawLine(axisColor, Offset(0f, bottomLineY), Offset(size.width, bottomLineY), LineThickness.toPx())
        listOf(tickNowX, tick30X, tick60X, tick120X).forEach { x ->
            drawLine(
                color = axisColor,
                start = Offset(x, bottomLineY),
                end = Offset(x, bottomLineY + tickHeight),
                strokeWidth = LineThickness.toPx(),
            )
        }

        val textTop = bottomLineY + tickHeight
        drawText(textMeasurer, text0, style = textStyle, topLeft = Offset(0f, textTop))
        drawText(
            textMeasurer, text30, style = textStyle,
            topLeft = Offset(tick30X - label30.size.width / 2f, textTop),
        )
        drawText(
            textMeasurer, text60, style = textStyle,
            topLeft = Offset(tick60X - label60.size.width / 2f, textTop),
        )
        drawText(
            textMeasurer, text120, style = textStyle,
            topLeft = Offset(size.width - label120.size.width, textTop),
        )
    }
}

@Preview
@Composable
private fun Preview() {
    val now = Clock.System.now()
    val intervals = (0 until 24).map { i ->
        val weight = when {
            i < 4 -> 1.0 - i * 0.15
            i < 8 -> 0.3
            else -> 0.15
        }
        MinutelyInterval(
            time = now,
            precipMillimeters = weight.coerceAtLeast(0.05),
            type = PrecipitationType.Rain,
        )
    }
    PreviewTheme {
        PrecipitationCard(
            MinutelyPrecipitation(
                summary = "未来两小时持续降雨。",
                intervals = intervals,
            ),
        )
    }
}
