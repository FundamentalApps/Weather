package org.fundamentalos.weather.ui.componets

import org.fundamentalos.weather.ui.text.localizedForecastTime
import org.fundamentalos.weather.ui.text.conditionText
import org.fundamentalos.weather.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.kyant.shapes.RoundedRectangle
import org.fundamentalos.weather.ui.theme.PreviewTheme

data class HourlyWeatherInfo(
    val time: String,
    val temp: Int,
    val icon: MultilayerIcon,
    val description: String,
    val conditionCode: String? = null,
)

/** Matches the inset the other cards use for their content. */
private val ContentInset = 24.dp

/** Breathing room the highlight keeps around each item's text. */
private val ItemInset = 10.dp

@Composable
fun HourlyWeatherCard(modifier: Modifier = Modifier, data: List<HourlyWeatherInfo>) {
    InfoCard(modifier) {
        val hourlyScroll = rememberScrollState()
        val hourlyOverscroll = rememberIosOverscrollState()
        Row(
            modifier = Modifier
                .iosOverscroll(hourlyOverscroll, isVertical = false)
                .horizontalScroll(
                    state = hourlyScroll,
                    flingBehavior = rememberIosFlingBehavior(hourlyScroll),
                )
                // ItemInset short of the card's 16dp content inset, so the first and last labels land
                // exactly on it while each highlight still has room to breathe around its text.
                .padding(horizontal = ContentInset - ItemInset, vertical = 16.dp)
        ) {
            data.fastForEachIndexed { i, item ->
                HourlyWeatherItem(
                    localizedForecastTime(item.time),
                    item.temp,
                    item.icon,
                    item.conditionCode?.let { conditionText(it, item.description) } ?: item.description,
                    highlight = i == 0
                )
            }
        }
    }
}

@Composable
private fun HourlyWeatherItem(
    time: String,
    temp: Int,
    weatherIcon: MultilayerIcon,
    weatherLabel: String,
    highlight: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier.widthIn(min = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        if (highlight) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedRectangle(8.dp)
                    )
            )
        }
        Column(
            Modifier
                .cardForegroundBlend()
                .padding(horizontal = ItemInset, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("$temp°", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(16.dp))
            MultilayerIcon(weatherIcon, weatherLabel)
            Spacer(Modifier.height(16.dp))
            Text(
                time,
                style = MaterialTheme.typography.titleSmall,
                color = LocalContentColor.current.copy(0.4f)
            )
        }
    }
}

@Preview
@Composable
private fun Preview() {
    PreviewTheme {
        HourlyWeatherCard(
            data = listOf(
                HourlyWeatherInfo(stringResource(R.string.now), 24, WeatherIcons.Rain, "Rain"),
                HourlyWeatherInfo("10 时", 24, WeatherIcons.TorrentialRain, "TorrentialRain"),
                HourlyWeatherInfo("11 时", 25, WeatherIcons.Clear, "Clear"),
                HourlyWeatherInfo("12 时", 25, WeatherIcons.Clear, "Clear"),
                HourlyWeatherInfo("下午 1 时", 26, WeatherIcons.Clear, "Clear")
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview
@Composable
private fun PreviewItem() {
    PreviewTheme {
        HourlyWeatherItem(
            stringResource(R.string.now),
            24,
            WeatherIcons.Clear,
            "Clear",
            true
        )
    }
}
