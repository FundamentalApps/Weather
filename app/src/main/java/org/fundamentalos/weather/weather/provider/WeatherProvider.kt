package org.fundamentalos.weather.weather.provider

import org.fundamentalos.weather.weather.domain.WeatherSnapshot

enum class WeatherCapability {
    ReverseGeocoding,
    CurrentWeather,
    DailyForecast,
    HourlyForecast,
    AirQuality,
    MinutelyPrecipitation,
    WeatherWarnings,
}

interface WeatherProvider {
    val id: String
    val name: String
    val capabilities: Set<WeatherCapability>

    suspend fun getWeather(latitude: Double, longitude: Double): WeatherSnapshot
}

class WeatherProviderException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
