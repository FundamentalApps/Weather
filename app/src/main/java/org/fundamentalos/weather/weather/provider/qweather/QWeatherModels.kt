package org.fundamentalos.weather.weather.provider.qweather

import kotlinx.serialization.Serializable

@Serializable
data class QWeatherCityLookupResponse(
    val code: String,
    val location: List<QWeatherLocationItem> = emptyList(),
)

@Serializable
data class QWeatherLocationItem(
    val name: String,
    val id: String,
    val lat: String,
    val lon: String,
    val adm2: String,
    val adm1: String,
    val country: String,
)

@Serializable
data class QWeatherNowResponse(
    val code: String,
    val now: QWeatherNow,
)

@Serializable
data class QWeatherNow(
    val obsTime: String,
    val temp: String,
    val feelsLike: String? = null,
    val icon: String,
    val text: String,
    val wind360: String,
    val windDir: String,
    val windScale: String,
    val windSpeed: String,
    val humidity: String,
    val precip: String,
    val pressure: String,
    val vis: String? = null,
    val cloud: String? = null,
    val dew: String? = null,
)

@Serializable
data class QWeatherDailyResponse(
    val code: String,
    val daily: List<QWeatherDailyItem> = emptyList(),
)

@Serializable
data class QWeatherDailyItem(
    val fxDate: String,
    val sunrise: String? = null,
    val sunset: String? = null,
    val tempMax: String,
    val tempMin: String,
    val iconDay: String,
    val textDay: String,
    val iconNight: String,
    val textNight: String,
    val precip: String,
    val uvIndex: String? = null,
    val humidity: String,
    val pressure: String,
)

@Serializable
data class QWeatherHourlyResponse(
    val code: String,
    val hourly: List<QWeatherHourlyItem> = emptyList(),
)

@Serializable
data class QWeatherHourlyItem(
    val fxTime: String,
    val temp: String,
    val icon: String,
    val text: String,
    val pop: String? = null,
)

@Serializable
data class QWeatherAirQualityNowResponse(
    val indexes: List<QWeatherAirQualityIndex> = emptyList(),
)

@Serializable
data class QWeatherAirQualityIndex(
    val aqi: Int,
    val level: String? = null,
    val category: String? = null,
    val health: QWeatherAirQualityHealth? = null,
)

@Serializable
data class QWeatherAirQualityHealth(
    val effect: String? = null,
)

@Serializable
data class QWeatherMinutelyResponse(
    val code: String,
    val summary: String = "",
    val minutely: List<QWeatherMinutelyItem> = emptyList(),
)

@Serializable
data class QWeatherMinutelyItem(
    val fxTime: String,
    val precip: String,
    val type: String,
)

@Serializable
data class QWeatherWarningResponse(
    val code: String,
    val warning: List<QWeatherWarningItem> = emptyList(),
)

@Serializable
data class QWeatherWarningItem(
    val id: String,
    val sender: String? = null,
    val pubTime: String? = null,
    val title: String,
    val startTime: String? = null,
    val endTime: String? = null,
    val status: String? = null,
    val severity: String? = null,
    val severityColor: String? = null,
    val typeName: String? = null,
    val text: String,
)
