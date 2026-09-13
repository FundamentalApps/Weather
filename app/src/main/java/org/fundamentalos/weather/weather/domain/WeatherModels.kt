package org.fundamentalos.weather.weather.domain

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

data class WeatherSnapshot(
    val providerId: String,
    val location: WeatherLocation,
    val current: CurrentWeather,
    val daily: List<DailyForecast>,
    val hourly: List<HourlyForecast>,
    val airQuality: AirQuality?,
    val minutelyPrecipitation: MinutelyPrecipitation? = null,
    val warnings: List<WeatherWarning> = emptyList(),
)

data class WeatherLocation(
    val name: String,
    val providerLocationId: String?,
    val latitude: Double,
    val longitude: Double,
    val city: String? = null,
    val province: String? = null,
    val country: String? = null,
)

data class CurrentWeather(
    val observedAt: String,
    val tempCelsius: Int,
    val feelsLikeCelsius: Int,
    val condition: WeatherCondition,
    val windDegree: Int?,
    val windDirection: String,
    val windScale: String,
    val windSpeedKph: Int?,
    val windGustKph: Int? = null,
    val humidityPercent: Int?,
    val precipMillimeters: Double?,
    val pressureHpa: Int?,
    val visibilityKm: Int?,
    val cloudPercent: Int?,
    val dewPointCelsius: Int?,
)

data class WeatherCondition(
    val iconCode: String,
    val text: String,
    val isDay: Boolean? = null,
)

data class DailyForecast(
    val date: LocalDate,
    val dayCondition: WeatherCondition,
    val nightCondition: WeatherCondition,
    val tempMinCelsius: Int,
    val tempMaxCelsius: Int,
    val precipMillimeters: Double?,
    val precipitationProbabilityPercent: Int?,
    val humidityPercent: Int?,
    val pressureHpa: Int?,
    val uvIndex: Int?,
    val sunrise: String? = null,
    val sunset: String? = null,
)

data class HourlyForecast(
    val time: Instant,
    val tempCelsius: Int,
    val condition: WeatherCondition,
    val precipitationProbabilityPercent: Int?,
)

data class AirQuality(
    val aqi: Int,
    val level: Int,
    val category: String?,
    val effect: String?,
)

data class MinutelyPrecipitation(
    val summary: String,
    val intervals: List<MinutelyInterval>,
) {
    val willPrecipitateSoon: Boolean
        get() = intervals.any { it.precipMillimeters > 0.0 }
}

data class MinutelyInterval(
    val time: Instant,
    val precipMillimeters: Double,
    val type: PrecipitationType,
)

enum class PrecipitationType { Rain, Snow, None }

data class WeatherWarning(
    val id: String,
    val title: String,
    val text: String,
    val defenseGuide: String?,
    val typeName: String?,
    val severity: WarningSeverity,
    val severityColor: String?,
    val sender: String?,
    val status: String?,
    val publishedAt: Instant?,
    val startTime: Instant?,
    val endTime: Instant?,
)

enum class WarningSeverity {
    Cancel, None, Unknown, Standard, Minor, Moderate, Major, Severe, Extreme;

    companion object {
        fun fromSeverityColor(value: String?): WarningSeverity {
            if (value.isNullOrBlank()) return Unknown
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: Unknown
        }
    }
}
