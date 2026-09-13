package org.fundamentalos.weather.ui.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.fundamentalos.weather.R
import org.fundamentalos.weather.ui.components.AirQualityCard
import org.fundamentalos.weather.ui.components.BannerScrollReserve
import org.fundamentalos.weather.ui.components.DailyWeatherCard
import org.fundamentalos.weather.ui.components.DetailGrid
import org.fundamentalos.weather.ui.components.DetailInfo
import org.fundamentalos.weather.ui.components.HourlyWeatherCard
import org.fundamentalos.weather.ui.components.LocationFooter
import org.fundamentalos.weather.ui.components.MoonCard
import org.fundamentalos.weather.ui.components.PrecipitationCard
import org.fundamentalos.weather.ui.components.PressureGauge
import org.fundamentalos.weather.ui.components.QuickInfoCard
import org.fundamentalos.weather.ui.components.SunArc
import org.fundamentalos.weather.ui.components.WeatherWarningsSection
import org.fundamentalos.weather.ui.components.WindCard
import org.fundamentalos.weather.ui.components.countryDisplayName
import org.fundamentalos.weather.ui.components.parseClockMinutes
import org.fundamentalos.weather.ui.text.localizedClock
import org.fundamentalos.weather.ui.text.localizedNumber
import org.fundamentalos.weather.ui.text.windDirectionText
import org.fundamentalos.weather.viewmodel.MainViewModel
import org.fundamentalos.weather.weather.domain.CurrentWeather
import org.fundamentalos.weather.weather.domain.DailyForecast
import kotlin.time.Clock

/** The gap between one card and the next. */
private val CardGap = 16.dp

/** Room above the first card upright: what the headline takes, and a breath below it. */
internal val PortraitCardsHeadroom = BannerScrollReserve + 28.dp

/**
 * The page's cards for one reading, top to bottom, each brought up in its turn by [enter], and
 * the place named at the foot. Laid in a column the caller scrolls, starting [headroom] down.
 */
@Composable
internal fun HomeCards(
    content: HomeContent,
    enter: Animatable<Float, AnimationVector1D>,
    location: MainViewModel.Location?,
    headroom: Dp = PortraitCardsHeadroom,
    /** Whether the quick-info chips lead the column; sideways they are pinned beside the headline. */
    quickInfo: Boolean = true,
) {
    val weather = content.weather
    val today = content.today
    var order = 0
    Spacer(Modifier.height(headroom))

    if (quickInfo) {
        Entering(enter, order++) { QuickInfoChips(content) }
        Spacer(Modifier.height(CardGap))
    }

    if (content.warnings.isNotEmpty()) {
        Entering(enter, order++) { WeatherWarningsSection(content.warnings) }
        Spacer(Modifier.height(CardGap))
    }

    content.minutely?.let {
        Entering(enter, order++) { PrecipitationCard(it) }
        Spacer(Modifier.height(CardGap))
    }

    content.aqi?.let {
        Entering(enter, order++) { AirQualityCard(it.aqi, it.level, it.effect ?: it.category.orEmpty()) }
    }
    Spacer(Modifier.height(CardGap))

    if (content.hourly.isNotEmpty()) {
        Entering(enter, order++) { HourlyWeatherCard(Modifier.fillMaxWidth(), content.hourly) }
    }
    Spacer(Modifier.height(CardGap))

    if (content.daily.isNotEmpty()) {
        Entering(enter, order++) { DailyWeatherCard(Modifier.fillMaxWidth(), content.daily) }
    }
    Spacer(Modifier.height(CardGap))

    Entering(enter, order++) {
        WindCard(
            speedKph = weather.windSpeedKph,
            gustKph = weather.windGustKph,
            direction = windDirectionText(weather.windDegree, weather.windDirection),
            degree = weather.windDegree,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    Spacer(Modifier.height(CardGap))

    today?.let {
        Entering(enter, order++) { MoonCard(it.date, Modifier.fillMaxWidth()) }
        Spacer(Modifier.height(CardGap))
    }

    Entering(enter, order++) {
        DetailGrid(items = detailItems(weather, today), modifier = Modifier.fillMaxWidth())
    }
    Spacer(Modifier.height(CardGap))

    location?.let {
        Entering(enter, order++) {
            LocationFooter(
                country = countryDisplayName(it.country),
                province = it.province,
                city = it.city,
                place = it.name,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            )
        }
    }
}

/** The reading at a glance: today's range, the feel, the wind. */
@Composable
internal fun QuickInfoChips(content: HomeContent) {
    val weather = content.weather
    QuickInfoCard(
        feelsLike = weather.feelsLikeCelsius.toString(),
        maxTemp = content.daily.firstOrNull()?.tempMax?.toString() ?: "--",
        minTemp = content.daily.firstOrNull()?.tempMin?.toString() ?: "--",
        windDirection = windDirectionText(weather.windDegree, weather.windDirection),
        windScale = weather.windScale,
    )
}

/** The detail grid's tiles for a reading: what the day has, in the order it reads best. */
@Composable
private fun detailItems(weather: CurrentWeather, today: DailyForecast?): List<DetailInfo> = buildList {
    // Whichever comes next leads, with the other underneath and the sun's path between them.
    val sunrise = parseClockMinutes(today?.sunrise)
    val sunset = parseClockMinutes(today?.sunset)
    if (sunrise != null && sunset != null && today != null) {
        val clock = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val nowMinutes = clock.hour * 60 + clock.minute
        val daytime = nowMinutes in sunrise until sunset
        add(
            DetailInfo(
                label = if (daytime) stringResource(R.string.sunset) else stringResource(R.string.sunrise),
                icon = R.drawable.ic_sun_horizon_24dp,
                value = localizedClock(if (daytime) today.sunset!! else today.sunrise!!),
                caption = if (daytime) {
                    stringResource(R.string.sunrise_at, localizedClock(today.sunrise!!))
                } else {
                    stringResource(R.string.sunset_at, localizedClock(today.sunset!!))
                },
                graphic = { SunArc(sunriseMinutes = sunrise, sunsetMinutes = sunset, nowMinutes = nowMinutes) },
            )
        )
    }
    today?.uvIndex?.let {
        add(DetailInfo(stringResource(R.string.uv_index), it.toString(), uvIndexLevel(it), icon = R.drawable.ic_uv_index_24dp))
    }
    add(
        DetailInfo(
            stringResource(R.string.feels_like), "${weather.feelsLikeCelsius}°",
            caption = feelsLikeCaption(weather.feelsLikeCelsius, weather.tempCelsius),
            icon = R.drawable.ic_feels_like_24dp,
        )
    )
    today?.let {
        val mean = (it.tempMinCelsius + it.tempMaxCelsius) / 2
        add(
            DetailInfo(
                stringResource(R.string.average_temperature), "$mean°", stringResource(R.string.average_temperature_description),
                icon = R.drawable.ic_temperature_24dp,
            )
        )
    }
    weather.precipMillimeters?.let {
        add(
            DetailInfo(
                stringResource(R.string.precipitation), formatMillimeters(it), stringResource(R.string.past_hour),
                icon = R.drawable.ic_precip_amount_24dp,
            )
        )
    }
    weather.visibilityKm?.let {
        add(
            DetailInfo(
                stringResource(R.string.visibility), stringResource(R.string.unit_km, it), visibilityCaption(it),
                icon = R.drawable.ic_visibility_24dp,
            )
        )
    }
    weather.humidityPercent?.let {
        add(
            DetailInfo(
                stringResource(R.string.humidity), "$it%",
                caption = weather.dewPointCelsius?.let { dew -> stringResource(R.string.dew_point, dew) },
                icon = R.drawable.ic_humidity_24dp,
            )
        )
    }
    weather.pressureHpa?.let {
        add(
            DetailInfo(
                label = stringResource(R.string.pressure),
                // Blank: the dial carries its own reading.
                value = "",
                icon = R.drawable.ic_pressure_24dp,
                graphic = { PressureGauge(it) },
            )
        )
    }
}

/** UV index bands as published by the WMO. */
@Composable
private fun uvIndexLevel(index: Int): String = when {
    index <= 2 -> stringResource(R.string.low)
    index <= 5 -> stringResource(R.string.moderate)
    index <= 7 -> stringResource(R.string.high)
    index <= 10 -> stringResource(R.string.very_high)
    else -> stringResource(R.string.extreme)
}

@Composable
private fun feelsLikeCaption(feelsLike: Int, actual: Int): String = when {
    feelsLike - actual >= 2 -> stringResource(R.string.feels_warmer)
    actual - feelsLike >= 2 -> stringResource(R.string.feels_colder)
    else -> stringResource(R.string.feels_similar)
}

@Composable
private fun visibilityCaption(km: Int): String = when {
    km < 1 -> stringResource(R.string.visibility_very_poor)
    km < 5 -> stringResource(R.string.visibility_poor)
    km < 10 -> stringResource(R.string.visibility_fair)
    km < 20 -> stringResource(R.string.visibility_good)
    else -> stringResource(R.string.visibility_clear)
}

@Composable
private fun formatMillimeters(value: Double): String =
    stringResource(R.string.unit_mm, localizedNumber(if (value < 0.05) 0.0 else value, if (value < 0.05) 0 else 1))
