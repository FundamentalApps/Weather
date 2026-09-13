package org.fundamentalos.weather.ui.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import org.fundamentalos.weather.R
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Date
import kotlin.time.Instant

/** Resolve text at the UI boundary, using the activity locale (including per-app language). */
@Composable
fun localizedNumber(value: Number, decimals: Int = 0): String =
    NumberFormat.getNumberInstance(LocalConfiguration.current.locales[0]).apply {
        minimumFractionDigits = decimals
        maximumFractionDigits = decimals
        isGroupingUsed = false
    }.format(value)

@Composable
fun localizedDays(days: Int): String =
    pluralStringResource(R.plurals.duration_days, days, localizedNumber(days))

@Composable
fun localizedFractionalDays(days: Double): String {
    val locale = LocalConfiguration.current.locales[0]
    val number = android.icu.text.NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 1
    }
    return android.icu.text.MeasureFormat.getInstance(locale,
        android.icu.text.MeasureFormat.FormatWidth.WIDE, number)
        .formatMeasures(android.icu.util.Measure(days, android.icu.util.MeasureUnit.DAY))
}

@Composable
fun localizedForecastDate(isoDate: String): String {
    val date = runCatching { LocalDate.parse(isoDate) }.getOrNull() ?: return isoDate
    val today = LocalDate.now()
    return when (date) {
        today -> stringResource(R.string.today)
        today.plusDays(1) -> stringResource(R.string.tomorrow)
        else -> date.dayOfWeek.getDisplayName(TextStyle.SHORT, LocalConfiguration.current.locales[0])
    }
}

@Composable
fun localizedForecastTime(isoTime: String): String {
    val instant = runCatching { java.time.Instant.parse(isoTime) }.getOrNull() ?: return isoTime
    val zone = ZoneId.systemDefault()
    val dateTime = instant.atZone(zone)
    val now = java.time.ZonedDateTime.now(zone)
    if (dateTime.toLocalDate() == now.toLocalDate() && dateTime.hour == now.hour) {
        return stringResource(R.string.now)
    }
    return android.text.format.DateFormat.getTimeFormat(LocalContext.current).format(Date.from(instant))
}

@Composable
fun localizedClock(value: String): String {
    val time = runCatching { LocalTime.parse(value) }.getOrNull() ?: return value
    val instant = time.atDate(LocalDate.now()).atZone(ZoneId.systemDefault()).toInstant()
    return android.text.format.DateFormat.getTimeFormat(LocalContext.current).format(Date.from(instant))
}

@Composable
fun localizedTime(instant: Instant): String =
    android.text.format.DateFormat.getTimeFormat(LocalContext.current)
        .format(Date(instant.toEpochMilliseconds()))

@Composable
fun localizedWarningTime(instant: Instant): String {
    val locale = LocalConfiguration.current.locales[0]
    val javaInstant = java.time.Instant.ofEpochMilli(instant.toEpochMilliseconds())
    val date = javaInstant.atZone(ZoneId.systemDefault()).toLocalDate()
    val today = LocalDate.now()
    val day = when (date) {
        today -> stringResource(R.string.today)
        today.minusDays(1) -> stringResource(R.string.yesterday)
        else -> date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale))
    }
    val time = android.text.format.DateFormat.getTimeFormat(LocalContext.current).format(Date.from(javaInstant))
    return "$day $time"
}

@Composable
fun windDirectionText(degree: Int?, fallback: String = "--"): String {
    if (degree == null) return fallback
    val directions = listOf(R.string.north, R.string.northeast, R.string.east, R.string.southeast,
        R.string.south, R.string.southwest, R.string.west, R.string.northwest)
    val normalized = ((degree % 360) + 360) % 360
    return stringResource(directions[((normalized + 22.5) / 45).toInt() % 8])
}

/** Condition codes are shared by all providers, so labels do not depend on server language. */
@Composable
fun conditionText(code: String, fallback: String = ""): String {
    val range = conditionRanges[code]
    if (range != null) return stringResource(R.string.condition_range,
        stringResource(range.first), stringResource(range.second))
    val resource = conditionResources[code]
    return if (resource != null) stringResource(resource)
        else fallback.ifBlank { stringResource(R.string.condition_unknown) }
}

private val conditionResources = buildMap {
    fun codes(resource: Int, vararg values: String) { values.forEach { put(it, resource) } }
    codes(R.string.condition_clear, "100", "150")
    codes(R.string.condition_cloudy, "101", "151")
    codes(R.string.condition_partly_cloudy, "102", "103", "152", "153")
    codes(R.string.condition_overcast, "104")
    codes(R.string.condition_showers, "300", "350")
    codes(R.string.condition_heavy_showers, "301", "351")
    codes(R.string.condition_thunderstorm, "302")
    codes(R.string.condition_severe_thunderstorm, "303")
    codes(R.string.condition_hail, "304")
    codes(R.string.condition_light_rain, "305")
    codes(R.string.condition_moderate_rain, "306")
    codes(R.string.condition_heavy_rain, "307")
    codes(R.string.condition_extreme_rain, "308")
    codes(R.string.condition_drizzle, "309")
    codes(R.string.condition_torrential_rain, "310")
    codes(R.string.condition_severe_torrential_rain, "311")
    codes(R.string.condition_extremely_severe_rain, "312")
    codes(R.string.condition_freezing_rain, "313")
    codes(R.string.condition_rain, "399")
    codes(R.string.condition_light_snow, "400")
    codes(R.string.condition_moderate_snow, "401")
    codes(R.string.condition_heavy_snow, "402")
    codes(R.string.condition_blizzard, "403")
    codes(R.string.condition_sleet, "404", "405", "406", "456")
    codes(R.string.condition_snow_showers, "407", "457")
    codes(R.string.condition_snow, "499")
    codes(R.string.condition_mist, "500")
    codes(R.string.condition_fog, "501")
    codes(R.string.condition_haze, "502")
    codes(R.string.condition_dust, "503")
    codes(R.string.condition_floating_dust, "504")
    codes(R.string.condition_sandstorm, "507")
    codes(R.string.condition_severe_sandstorm, "508")
    codes(R.string.condition_dense_fog, "509")
    codes(R.string.condition_severe_fog, "510")
    codes(R.string.condition_moderate_haze, "511")
    codes(R.string.condition_heavy_haze, "512")
    codes(R.string.condition_severe_haze, "513")
    codes(R.string.condition_heavy_fog, "514")
    codes(R.string.condition_extreme_fog, "515")
    codes(R.string.condition_hot, "900")
    codes(R.string.condition_cold, "901")
    codes(R.string.condition_unknown, "999")
}

private val conditionRanges = mapOf(
    "314" to (R.string.condition_light_rain to R.string.condition_moderate_rain),
    "315" to (R.string.condition_moderate_rain to R.string.condition_heavy_rain),
    "316" to (R.string.condition_heavy_rain to R.string.condition_torrential_rain),
    "317" to (R.string.condition_torrential_rain to R.string.condition_severe_torrential_rain),
    "318" to (R.string.condition_severe_torrential_rain to R.string.condition_extremely_severe_rain),
    "408" to (R.string.condition_light_snow to R.string.condition_moderate_snow),
    "409" to (R.string.condition_moderate_snow to R.string.condition_heavy_snow),
    "410" to (R.string.condition_heavy_snow to R.string.condition_blizzard),
)

@Composable
fun mapLayerName(id: String, fallback: String): String = when (id) {
    "temperature" -> stringResource(R.string.map_temperature)
    "radar" -> stringResource(R.string.map_radar)
    else -> fallback
}
