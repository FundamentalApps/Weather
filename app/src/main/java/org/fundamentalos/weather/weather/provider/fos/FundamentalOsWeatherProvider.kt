package org.fundamentalos.weather.weather.provider.fos

import org.fundamentalos.weather.weather.domain.AirQuality
import org.fundamentalos.weather.weather.domain.CurrentWeather
import org.fundamentalos.weather.weather.domain.DailyForecast
import org.fundamentalos.weather.weather.domain.HourlyForecast
import org.fundamentalos.weather.weather.domain.MinutelyInterval
import org.fundamentalos.weather.weather.domain.MinutelyPrecipitation
import org.fundamentalos.weather.weather.domain.PrecipitationType
import org.fundamentalos.weather.weather.domain.WarningSeverity
import org.fundamentalos.weather.weather.domain.WeatherCondition
import org.fundamentalos.weather.weather.domain.WeatherLocation
import org.fundamentalos.weather.weather.domain.WeatherSnapshot
import org.fundamentalos.weather.weather.domain.WeatherWarning
import org.fundamentalos.weather.weather.provider.ProviderRegistry
import org.fundamentalos.weather.weather.provider.WeatherCapability
import org.fundamentalos.weather.weather.provider.WeatherProvider
import org.fundamentalos.weather.weather.provider.WeatherProviderException
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

/**
 * Default provider: the FundamentalOS server aggregates free sources (Open-Meteo, MET Norway, WAQI),
 * caches per grid cell and fails over between them, so the app makes exactly one request per refresh.
 */
class FundamentalOsWeatherProvider(
    private val api: FosApiClient,
) : WeatherProvider {
    override val id: String = ProviderRegistry.FundamentalOsProviderId
    override val name: String = "FundamentalOS"
    override val capabilities: Set<WeatherCapability> = setOf(
        WeatherCapability.ReverseGeocoding,
        WeatherCapability.CurrentWeather,
        WeatherCapability.DailyForecast,
        WeatherCapability.HourlyForecast,
        WeatherCapability.AirQuality,
        WeatherCapability.MinutelyPrecipitation,
    )

    override suspend fun getWeather(latitude: Double, longitude: Double): WeatherSnapshot {
        val snapshot = try {
            api.snapshot(latitude, longitude)
        } catch (e: FosApiException) {
            throw WeatherProviderException("FundamentalOS provider failed (${e.code}): ${e.message}", e)
        } catch (e: Exception) {
            throw WeatherProviderException("FundamentalOS provider failed", e)
        }
        return snapshot.toDomain(id)
    }
}

private fun FosSnapshot.toDomain(providerId: String): WeatherSnapshot = WeatherSnapshot(
    providerId = providerId,
    location = WeatherLocation(
        name = location.name,
        providerLocationId = null,
        latitude = location.latitude,
        longitude = location.longitude,
        city = location.city,
        province = location.province,
        country = location.country,
    ),
    current = current.toDomain(),
    daily = daily.map { it.toDomain() },
    hourly = hourly.map { it.toDomain() },
    airQuality = airQuality?.let { AirQuality(aqi = it.aqi, level = it.level, category = it.category, effect = it.effect) },
    minutelyPrecipitation = minutelyPrecipitation?.toDomain(),
    warnings = warnings.map { it.toDomain() },
)

private fun FosCondition.toDomain(): WeatherCondition = WeatherCondition(iconCode = icon, text = text, isDay = isDay)

private fun FosCurrent.toDomain(): CurrentWeather = CurrentWeather(
    observedAt = observedAt,
    tempCelsius = tempC,
    feelsLikeCelsius = feelsLikeC,
    condition = condition.toDomain(),
    windDegree = windDegree,
    windDirection = windDirection,
    windScale = windScale,
    windSpeedKph = windSpeedKph,
    windGustKph = windGustKph,
    humidityPercent = humidityPercent,
    precipMillimeters = precipMm,
    pressureHpa = pressureHpa,
    visibilityKm = visibilityKm,
    cloudPercent = cloudPercent,
    dewPointCelsius = dewPointC,
)

private fun FosDaily.toDomain(): DailyForecast = DailyForecast(
    date = LocalDate.parse(date),
    dayCondition = day.toDomain(),
    nightCondition = night.toDomain(),
    tempMinCelsius = tempMinC,
    tempMaxCelsius = tempMaxC,
    precipMillimeters = precipMm,
    precipitationProbabilityPercent = precipProbabilityPercent,
    humidityPercent = humidityPercent,
    pressureHpa = pressureHpa,
    uvIndex = uvIndex,
    sunrise = sunrise,
    sunset = sunset,
)

private fun FosHourly.toDomain(): HourlyForecast = HourlyForecast(
    time = Instant.parse(time),
    tempCelsius = tempC,
    condition = condition.toDomain(),
    precipitationProbabilityPercent = precipProbabilityPercent,
)

private fun FosMinutely.toDomain(): MinutelyPrecipitation? {
    if (intervals.isEmpty()) return null
    return MinutelyPrecipitation(
        summary = summary,
        intervals = intervals.map {
            MinutelyInterval(
                time = Instant.parse(it.time),
                precipMillimeters = it.precipMm,
                type = when (it.type) {
                    "rain" -> PrecipitationType.Rain
                    "snow" -> PrecipitationType.Snow
                    else -> PrecipitationType.None
                },
            )
        },
    )
}

private fun FosWarning.toDomain(): WeatherWarning = WeatherWarning(
    id = id,
    title = title,
    text = text,
    defenseGuide = null,
    typeName = typeName,
    severity = WarningSeverity.fromQWeather(severity),
    severityColor = null,
    sender = sender,
    status = null,
    publishedAt = publishedAt?.toInstantOrNull(),
    startTime = startTime?.toInstantOrNull(),
    endTime = endTime?.toInstantOrNull(),
)

private fun String.toInstantOrNull(): Instant? = runCatching { Instant.parse(this) }.getOrNull()
