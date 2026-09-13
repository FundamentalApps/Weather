package org.fundamentalos.weather.ui.componets

import org.fundamentalos.weather.ui.text.localizedForecastDate
import org.fundamentalos.weather.ui.text.conditionText
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.kyant.shapes.RoundedRectangle
import org.fundamentalos.weather.R
import org.fundamentalos.weather.ui.theme.PreviewTheme
import org.fundamentalos.weather.ui.theme.PreviewThemeWithBg
import org.fundamentalos.weather.ui.theme.harmonized
import org.fundamentalos.weather.ui.theme.rain
import org.fundamentalos.weather.ui.theme.rainDark
import org.fundamentalos.weather.ui.theme.temperature0
import org.fundamentalos.weather.ui.theme.temperature10
import org.fundamentalos.weather.ui.theme.temperature20
import org.fundamentalos.weather.ui.theme.temperature30
import org.fundamentalos.weather.ui.theme.temperature40
import org.fundamentalos.weather.ui.theme.temperature50
import org.fundamentalos.weather.ui.theme.temperatureMinor10
import org.fundamentalos.weather.ui.theme.temperatureMinor20
import org.fundamentalos.weather.ui.theme.temperatureMinor40

data class DailyWeatherInfo(
    val date: String,
    val icon: MultilayerIcon,
    val description: String,
    val probability: String?,
    val tempMin: Int,
    val tempMax: Int,
    val conditionCode: String? = null,
)

@Composable
fun DailyWeatherCard(modifier: Modifier, data: List<DailyWeatherInfo>) {
    InfoCard(
        modifier = modifier,
        icon = painterResource(R.drawable.ic_calendar_month_20px),
        category = stringResource(R.string.forecast_title),
    ) {
        // The header carries the gap below itself now, so no extra top inset here.
        Column(Modifier.padding(start = 8.dp, end = 8.dp, bottom = 6.dp)) {
            val maxTemp = remember(data) { data.maxOf { it.tempMax } }
            val minTemp = remember(data) { data.minOf { it.tempMin } }

            data.fastForEachIndexed { index, it ->
                DailyWeatherItem(
                    label = localizedForecastDate(it.date),
                    weatherIcon = it.icon,
                    weatherDescription = it.conditionCode?.let { code -> conditionText(code, it.description) } ?: it.description,
                    tempRangeMin = minTemp,
                    tempRangeMax = maxTemp,
                    tempMin = it.tempMin,
                    tempMax = it.tempMax,
                    highlight = index == 0,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}


@Composable
private fun DailyWeatherItem(
    label: String,
    weatherIcon: MultilayerIcon,
    weatherDescription: String,
    tempRangeMin: Int,
    tempRangeMax: Int,
    tempMin: Int,
    tempMax: Int,
    highlight: Boolean,
    modifier: Modifier = Modifier,
    probability: String? = null
) {
    Box(modifier) {
        if (highlight) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedRectangle(12.dp)
                    )
            )
        }
        Row(
            Modifier
                .cardForegroundBlend()
                .padding(start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        val tempTextStyle = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum")
        // For fixed width
        val tempTextWidth = measureTempTextWidth(tempTextStyle)

        Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(0.dp, 12.dp))
        Spacer(Modifier.width(16.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MultilayerIcon(weatherIcon, weatherDescription)
            probability?.let {
                Text(
                    text = it,
                    color = if (LocalUseDarkCards.current) rainDark.harmonized() else rain.harmonized(),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Text(
            modifier = Modifier.width(tempTextWidth),
            text = "$tempMin°",
            color = LocalContentColor.current.copy(0.4f),
            style = tempTextStyle,
            textAlign = TextAlign.End
        )
        Spacer(Modifier.width(16.dp))
        TempIndicator(tempRangeMin, tempRangeMax, tempMin, tempMax, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(16.dp))
        Text(
            modifier = Modifier.width(tempTextWidth),
            text = "$tempMax°",
            style = tempTextStyle,
            textAlign = TextAlign.End
        )
    }
    }
}

@Composable
private fun measureTempTextWidth(style: TextStyle): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    // The text ("-00°") and style are fixed, so the measurement is constant. Cache it: without
    // this, every recomposition re-measured it once per day row (~10x), a hotspot in profiles.
    return remember(measurer, density, style) {
        with(density) { measurer.measure(text = "-00°", style = style).size.width.toDp() }
    }
}

private val brushColors = listOf(temperatureMinor40, temperatureMinor20, temperatureMinor10, temperature0, temperature10, temperature20, temperature30, temperature40, temperature50)

@Composable
private fun TempIndicator(
    rangeMin: Int,
    rangeMax: Int,
    currentMin: Int,
    currentMax: Int,
    modifier: Modifier
) {
    val trackColor = MaterialTheme.colorScheme.primary.copy(0.08f)
    val brushColors = brushColors.map { it.harmonized() }

    Canvas(modifier.requiredHeight(4.dp)) {
        val brushRangeMax = 50
        val brushRangeMin = -40

        val brushStartX = (brushRangeMin - rangeMin).toFloat() / (rangeMax - rangeMin).toFloat() * size.width
        val brushWidth = (brushRangeMax - brushRangeMin).toFloat() / (rangeMax - rangeMin).toFloat() * size.width

        val indicatorBrush = Brush.horizontalGradient(
            0f to brushColors[0],
            0.22f to brushColors[1],
            0.33f to brushColors[2],
            0.44f to brushColors[3],
            0.55f to brushColors[4],
            0.66f to brushColors[5],
            0.77f to brushColors[6],
            0.88f to brushColors[7],
            1f to brushColors[8],
            startX = brushStartX,
            endX = brushStartX + brushWidth
        )

        drawRoundRect(trackColor, size = size, cornerRadius = CornerRadius(size.height / 2))
        val startX =
            (currentMin - rangeMin).toFloat() / (rangeMax - rangeMin).toFloat() * size.width
        val width =
            (currentMax - currentMin).toFloat() / (rangeMax - rangeMin).toFloat() * size.width
        drawRoundRect(
            indicatorBrush,
            size = Size(width, size.height),
            topLeft = Offset(startX, 0f),
            cornerRadius = CornerRadius(size.height / 2)
        )
    }
}

@Preview
@Composable
private fun Preview() {
    PreviewTheme {
        val testData = remember {
            listOf(
                DailyWeatherInfo(
                    date = java.time.LocalDate.now().toString(),
                    icon = WeatherIcons.Rain,
                    description = "Rain",
                    probability = null,
                    tempMin = 8,
                    tempMax = 24
                ),
                DailyWeatherInfo(
                    date = java.time.LocalDate.now().plusDays(1).toString(),
                    icon = WeatherIcons.TorrentialRain,
                    description = "Clear",
                    probability = "40%",
                    tempMin = 11,
                    tempMax = 23
                ),
                DailyWeatherInfo(
                    date = java.time.LocalDate.now().plusDays(2).toString(),
                    icon = WeatherIcons.Clear,
                    description = "Clear",
                    probability = null,
                    tempMin = 12,
                    tempMax = 16
                ),
            )
        }
        DailyWeatherCard(modifier = Modifier.fillMaxWidth(), testData)
    }
}

@Composable
@Preview
private fun PreviewDailyTempItemCommon() {
    PreviewThemeWithBg {
        DailyWeatherItem(
            stringResource(R.string.today), WeatherIcons.Rain, "TorrentialRain",
            10, 30,
            14, 24,
            false,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
@Preview
private fun PreviewDailyTempItemHighlight() {
    PreviewTheme {
        DailyWeatherItem(
            stringResource(R.string.tomorrow), WeatherIcons.TorrentialRain, "TorrentialRain",
            10, 30,
            14, 24,
            true,
            modifier = Modifier.fillMaxWidth(),
            "40%"
        )
    }
}

@Composable
@Preview
private fun PreviewIndicator() {
    PreviewTheme {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            TempIndicator(-40, 50, -40, 50, Modifier.fillMaxWidth())
            TempIndicator(-40, 50, 15, 35, Modifier.fillMaxWidth())
            TempIndicator(9, 42, 15, 42, Modifier.fillMaxWidth())
            TempIndicator(9, 42, 12, 20, Modifier.fillMaxWidth())
        }
    }
}
