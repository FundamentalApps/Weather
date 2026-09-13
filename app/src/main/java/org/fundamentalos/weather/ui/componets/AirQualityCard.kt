package org.fundamentalos.weather.ui.componets

import androidx.compose.ui.res.stringResource
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fundamentalos.weather.R
import org.fundamentalos.weather.ui.theme.PreviewTheme
import org.fundamentalos.weather.ui.theme.aqi1
import org.fundamentalos.weather.ui.theme.aqi2
import org.fundamentalos.weather.ui.theme.aqi3
import org.fundamentalos.weather.ui.theme.aqi4
import org.fundamentalos.weather.ui.theme.aqi5
import org.fundamentalos.weather.ui.theme.aqi6
import org.fundamentalos.weather.ui.theme.harmonized

/**
 * 0 to 50, 51 to 100, 101 to 150, 151 to 200, 201 to 300, 301 to 500
 */
private val AQILevelP = listOf(0f, 0.1f, 0.2f, 0.3f, 0.4f, 0.6f, 1f)
private const val MaxAQI = 500

private val LevelText = mapOf(
    1 to R.string.aqi_good,
    2 to R.string.moderate,
    3 to R.string.aqi_sensitive,
    4 to R.string.aqi_unhealthy,
    5 to R.string.aqi_very_unhealthy,
    6 to R.string.aqi_hazardous
)

@Composable
fun AirQualityCard(
    aqi: Int,
    level: Int,
    effect: String
) {
    val aqiIcon = when (level) {
        1 -> R.drawable.ic_aqi_low_20dp
        2 -> R.drawable.ic_aqi_medium_20dp
        else -> R.drawable.ic_aqi_high_20dp
    }
    InfoCard(
        icon = painterResource(aqiIcon),
        category = stringResource(R.string.air_quality),
        title = aqi.toString(),
        titleAlt = LevelText[level]?.let { stringResource(it) },
        subtitle = effect
    ) {
        Box(Modifier.padding(horizontal = 16.dp)) {
            val aqi1Color = aqi1.harmonized()
            val aqi2Color = aqi2.harmonized()
            val aqi3Color = aqi3.harmonized()
            val aqi4Color = aqi4.harmonized()
            val aqi5Color = aqi5.harmonized()
            val aqi6Color = aqi6.harmonized()
            val indicatorColor = MaterialTheme.colorScheme.onSurface
            val colors = listOf(aqi1Color, aqi2Color, aqi3Color, aqi4Color, aqi5Color, aqi6Color)

            Canvas(
                Modifier
                    .cardForegroundBlend()
                    .padding(0.dp, 12.dp)
                    .height(20.dp)
                    .fillMaxWidth()
            ) {
                val indicatorRect = drawIndicator(aqi, indicatorColor)

                repeat(6) { i ->
                    val paddingL = if (i == 0) 0f else 2.dp.toPx()
                    val paddingR = if (i == 5) 0f else 2.dp.toPx()

                    val start = size.width * AQILevelP[i] + paddingL
                    val end = size.width * AQILevelP[i + 1] - paddingR
                    val lineHeight = 12.dp.toPx()
                    val top = (size.height - lineHeight) / 2

                    drawNotchedRect(
                        offset = Offset(start, top),
                        size = Size(end - start, lineHeight),
                        notchOffset = indicatorRect.topLeft,
                        notchSize = indicatorRect.size,
                        color = colors[i],
                        first = i == 0,
                        last = i == 5
                    )
                }
            }
        }
    }
}


private fun DrawScope.drawIndicator(aqi: Int, color: Color): Rect {
    val indicatorF = aqi.toFloat() / MaxAQI

    val indicatorPadding = 2.dp.toPx()
    val indicatorW = 2.dp.toPx()
    val indicatorH = 20.dp.toPx()
    val indicatorX = indicatorF * (size.width - indicatorW)

    val rect = Rect(offset = Offset(indicatorX, 0f), size = Size(indicatorW, indicatorH))
    drawRoundRect(color, rect.topLeft, rect.size, CornerRadius(indicatorW / 2))
    return Rect(
        left = rect.left - indicatorPadding,
        right = rect.right + indicatorPadding,
        top = rect.top,
        bottom = rect.bottom
    )
}

private val CornerRadiusEnd = 6.dp
private val CornerRadiusNormal = 4.dp
private val CornerRadiusCut = 2.dp
private val PaddingToNotch = 2.dp

private fun DrawScope.drawNotchedRect(
    offset: Offset,
    size: Size,
    notchOffset: Offset,
    notchSize: Size,
    color: Color,
    first: Boolean,
    last: Boolean
) {
    val path = Path().apply {
        // notch-l in range || notch-r in range
        val notchRect = Rect(notchOffset, notchSize)
        val rect = Rect(offset, size)
        val cutout = (notchRect.left >= rect.left && notchRect.left <= rect.right) ||
                (notchRect.right >= rect.left && notchRect.right <= rect.right)
        val circleRadius = size.height / 2
        val lCornerRadius = if (first) circleRadius else CornerRadiusNormal.toPx()
        val rCornerRadius = if (last) circleRadius else CornerRadiusNormal.toPx()

        if (cutout) {
            // If notch-l > l + threshold
            val showLPart = notchOffset.x > offset.x + PaddingToNotch.toPx()
            val lPartWidth = notchOffset.x - offset.x

            // If notch-r < r - threshold
            val showRPart = notchOffset.x + notchSize.width < offset.x + size.width - PaddingToNotch.toPx()
            val rPartWidth = offset.x + size.width - (notchOffset.x + notchSize.width)

            if (showLPart) addRoundRect(
                RoundRect(
                    rect = Rect(
                        Offset(offset.x, offset.y),
                        Size(lPartWidth, size.height)
                    ),
                    topLeft = CornerRadius(lCornerRadius),
                    bottomLeft = CornerRadius(lCornerRadius),
                    topRight = CornerRadius(CornerRadiusCut.toPx()),
                    bottomRight = CornerRadius(CornerRadiusCut.toPx())
                )
            )
            if (showRPart) addRoundRect(
                RoundRect(
                    rect = Rect(
                        Offset(offset.x + size.width - rPartWidth, offset.y),
                        Size(rPartWidth, size.height)
                    ),
                    topLeft = CornerRadius(CornerRadiusCut.toPx()),
                    bottomLeft = CornerRadius(CornerRadiusCut.toPx()),
                    topRight = CornerRadius(rCornerRadius),
                    bottomRight = CornerRadius(rCornerRadius)
                )
            )
        } else {
            addRoundRect(
                RoundRect(
                    rect = Rect(offset, size),
                    topLeft = CornerRadius(lCornerRadius),
                    bottomLeft = CornerRadius(lCornerRadius),
                    topRight = CornerRadius(rCornerRadius),
                    bottomRight = CornerRadius(rCornerRadius)
                )
            )
        }
    }
    drawPath(path, color)
}

@Preview
@Composable
private fun Preview() {
    PreviewTheme {
        val infiniteTransition = rememberInfiniteTransition()
        val anim = infiniteTransition.animateFloat(
            0f, 600f,
            infiniteRepeatable(
                tween(durationMillis = 1000, easing = LinearEasing),
                RepeatMode.Restart
            )
        )
        AirQualityCard(anim.value.toInt(), 2, "与昨天同一时间类似")
    }
}
