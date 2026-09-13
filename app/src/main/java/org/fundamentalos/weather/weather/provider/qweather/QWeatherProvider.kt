package org.fundamentalos.weather.weather.provider.qweather

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
import org.fundamentalos.weather.weather.provider.orZero
import org.fundamentalos.weather.weather.provider.toDoubleOrNullSafe
import org.fundamentalos.weather.weather.provider.toIntOrNullSafe
import io.ktor.client.request.parameter
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import java.time.OffsetDateTime
import java.util.Locale

class QWeatherProvider(
    private val credentialStore: QWeatherCredentialStore,
    private val apiClient: QWeatherApiClient,
) : WeatherProvider {
    override val id: String = ProviderRegistry.QWeatherProviderId
    override val name: String = "QWeather"
    override val capabilities: Set<WeatherCapability> = setOf(
        WeatherCapability.ReverseGeocoding,
        WeatherCapability.CurrentWeather,
        WeatherCapability.DailyForecast,
        WeatherCapability.HourlyForecast,
        WeatherCapability.AirQuality,
        WeatherCapability.MinutelyPrecipitation,
        WeatherCapability.WeatherWarnings,
    )

    override suspend fun getWeather(latitude: Double, longitude: Double): WeatherSnapshot {
        val credentials = credentialStore.getCredentials()
            ?: throw WeatherProviderException("QWeather credentials are not configured")

        try {
            val cityResp = apiClient.get<QWeatherCityLookupResponse>(credentials, "/geo/v2/city/lookup") {
                parameter("location", coordinateParam(latitude, longitude))
            }
            ensureCodeOk(cityResp.code, "city lookup")
            val city = cityResp.location.firstOrNull()
                ?: throw WeatherProviderException("QWeather returned no city for location")
            val location = city.toWeatherLocation(latitude, longitude)

            val gridLocation = coordinateParam(latitude, longitude)

            val gridNow = apiClient.get<QWeatherNowResponse>(credentials, "/v7/grid-weather/now") {
                parameter("location", gridLocation)
            }
            ensureCodeOk(gridNow.code, "grid current weather")

            val cityNow = apiClient.get<QWeatherNowResponse>(credentials, "/v7/weather/now") {
                parameter("location", city.id)
            }
            ensureCodeOk(cityNow.code, "city current weather fallback")

            val gridDaily = apiClient.get<QWeatherDailyResponse>(credentials, "/v7/grid-weather/7d") {
                parameter("location", gridLocation)
            }
            ensureCodeOk(gridDaily.code, "grid daily forecast")

            val cityDaily = apiClient.get<QWeatherDailyResponse>(credentials, "/v7/weather/10d") {
                parameter("location", city.id)
            }
            ensureCodeOk(cityDaily.code, "city daily forecast fallback")

            val gridHourly = apiClient.get<QWeatherHourlyResponse>(credentials, "/v7/grid-weather/24h") {
                parameter("location", gridLocation)
            }
            ensureCodeOk(gridHourly.code, "grid hourly forecast")

            val cityHourly = apiClient.get<QWeatherHourlyResponse>(credentials, "/v7/weather/24h") {
                parameter("location", city.id)
            }
            ensureCodeOk(cityHourly.code, "city hourly forecast fallback")

            val airQuality = runCatching {
                apiClient.get<QWeatherAirQualityNowResponse>(
                    credentials,
                    "/airquality/v1/current/${formatCoordinate(latitude)}/${formatCoordinate(longitude)}",
                )
            }.getOrNull()

            val minutely = runCatching {
                apiClient.get<QWeatherMinutelyResponse>(credentials, "/v7/minutely/5m") {
                    parameter("location", coordinateParam(latitude, longitude))
                }
            }.getOrNull()?.takeIf { it.code == "200" }

            val warnings = runCatching {
                apiClient.get<QWeatherWarningResponse>(credentials, "/v7/warning/now") {
                    parameter("location", city.id)
                }
            }.getOrNull()?.takeIf { it.code == "200" }

            return WeatherSnapshot(
                providerId = id,
                location = location,
                current = gridNow.now.toCurrentWeather(cityNow.now),
                daily = mergeDailyForecasts(gridDaily.daily, cityDaily.daily),
                hourly = mergeHourlyForecasts(gridHourly.hourly, cityHourly.hourly),
                airQuality = airQuality?.indexes?.firstOrNull()?.toAirQuality(),
                minutelyPrecipitation = minutely?.toMinutelyPrecipitation(),
                warnings = warnings?.warning?.map { it.toWeatherWarning() } ?: emptyList(),
            )
        } catch (e: WeatherProviderException) {
            throw e
        } catch (e: Throwable) {
            throw WeatherProviderException("QWeather provider failed", e)
        }
    }

    suspend fun testConnection() {
        getWeather(31.82, 117.22)
    }

    private fun ensureCodeOk(code: String, action: String) {
        if (code != "200") {
            throw WeatherProviderException("QWeather $action failed with code $code")
        }
    }
}

private fun QWeatherLocationItem.toWeatherLocation(
    fallbackLatitude: Double,
    fallbackLongitude: Double,
): WeatherLocation {
    return WeatherLocation(
        name = name,
        providerLocationId = id,
        latitude = lat.toDoubleOrNull() ?: fallbackLatitude,
        longitude = lon.toDoubleOrNull() ?: fallbackLongitude,
        city = adm2,
        province = adm1,
        country = country,
    )
}

private fun QWeatherNow.toCurrentWeather(fallback: QWeatherNow? = null): CurrentWeather {
    return CurrentWeather(
        observedAt = obsTime,
        tempCelsius = temp.toIntOrNullSafe().orZero(),
        feelsLikeCelsius = feelsLike.toIntOrNullSafe()
            ?: fallback?.feelsLike.toIntOrNullSafe()
            ?: temp.toIntOrNullSafe().orZero(),
        condition = WeatherCondition(iconCode = icon, text = text),
        windDegree = wind360.toIntOrNullSafe(),
        windDirection = windDir,
        windScale = windScale,
        windSpeedKph = windSpeed.toIntOrNullSafe(),
        humidityPercent = humidity.toIntOrNullSafe(),
        precipMillimeters = precip.toDoubleOrNullSafe(),
        pressureHpa = pressure.toIntOrNullSafe(),
        visibilityKm = vis.toIntOrNullSafe() ?: fallback?.vis.toIntOrNullSafe(),
        cloudPercent = cloud.toIntOrNullSafe() ?: fallback?.cloud.toIntOrNullSafe(),
        dewPointCelsius = dew.toIntOrNullSafe() ?: fallback?.dew.toIntOrNullSafe(),
    )
}

private fun mergeDailyForecasts(
    gridDaily: List<QWeatherDailyItem>,
    cityDaily: List<QWeatherDailyItem>,
): List<DailyForecast> {
    val cityByDate = cityDaily.associateBy { it.fxDate }
    val gridDates = gridDaily.mapTo(mutableSetOf()) { it.fxDate }
    return gridDaily.map { gridItem ->
        gridItem.toDailyForecast(cityByDate[gridItem.fxDate])
    } + cityDaily.filter { it.fxDate !in gridDates }.map { cityItem ->
        cityItem.toDailyForecast()
    }
}

private fun QWeatherDailyItem.toDailyForecast(fallback: QWeatherDailyItem? = null): DailyForecast {
    return DailyForecast(
        date = LocalDate.parse(fxDate),
        dayCondition = WeatherCondition(iconCode = iconDay, text = textDay, isDay = true),
        nightCondition = WeatherCondition(iconCode = iconNight, text = textNight, isDay = false),
        tempMinCelsius = tempMin.toIntOrNullSafe().orZero(),
        tempMaxCelsius = tempMax.toIntOrNullSafe().orZero(),
        precipMillimeters = precip.toDoubleOrNullSafe(),
        precipitationProbabilityPercent = null,
        humidityPercent = humidity.toIntOrNullSafe(),
        pressureHpa = pressure.toIntOrNullSafe(),
        uvIndex = uvIndex.toIntOrNullSafe() ?: fallback?.uvIndex.toIntOrNullSafe(),
        sunrise = sunrise ?: fallback?.sunrise,
        sunset = sunset ?: fallback?.sunset,
    )
}

private fun mergeHourlyForecasts(
    gridHourly: List<QWeatherHourlyItem>,
    cityHourly: List<QWeatherHourlyItem>,
): List<HourlyForecast> {
    val cityByTime = cityHourly.associateBy { it.fxTime }
    return gridHourly.map { gridItem ->
        gridItem.toHourlyForecast(cityByTime[gridItem.fxTime])
    }
}

private fun QWeatherHourlyItem.toHourlyForecast(fallback: QWeatherHourlyItem? = null): HourlyForecast {
    return HourlyForecast(
        time = parseQWeatherTime(fxTime),
        tempCelsius = temp.toIntOrNullSafe().orZero(),
        condition = WeatherCondition(iconCode = icon, text = text),
        precipitationProbabilityPercent = pop.toIntOrNullSafe() ?: fallback?.pop.toIntOrNullSafe(),
    )
}

private fun QWeatherAirQualityIndex.toAirQuality(): AirQuality {
    return AirQuality(
        aqi = aqi,
        level = level.toIntOrNullSafe().orZero(),
        category = category,
        effect = health?.effect,
    )
}

private fun QWeatherMinutelyResponse.toMinutelyPrecipitation(): MinutelyPrecipitation? {
    val intervals = minutely.mapNotNull { item ->
        val time = parseQWeatherTimeOrNull(item.fxTime) ?: return@mapNotNull null
        MinutelyInterval(
            time = time,
            precipMillimeters = item.precip.toDoubleOrNullSafe() ?: 0.0,
            type = when (item.type.lowercase(Locale.ROOT)) {
                "rain" -> PrecipitationType.Rain
                "snow" -> PrecipitationType.Snow
                else -> PrecipitationType.None
            },
        )
    }
    if (intervals.isEmpty() && summary.isBlank()) return null
    return MinutelyPrecipitation(summary = summary, intervals = intervals)
}

private fun QWeatherWarningItem.toWeatherWarning(): WeatherWarning {
    val normalized = normalizeQWeatherWarning(title, text)
    return WeatherWarning(
        id = id,
        title = normalized.title,
        text = normalized.body,
        defenseGuide = normalized.defenseGuide,
        typeName = typeName,
        severity = WarningSeverity.fromQWeather(severity),
        severityColor = severityColor,
        sender = sender,
        status = status,
        publishedAt = pubTime?.let { parseQWeatherTimeOrNull(it) },
        startTime = startTime?.let { parseQWeatherTimeOrNull(it) },
        endTime = endTime?.let { parseQWeatherTimeOrNull(it) },
    )
}

private fun parseQWeatherTime(input: String): Instant {
    return parseQWeatherTimeOrNull(input)
        ?: throw IllegalArgumentException("Unrecognized QWeather time: $input")
}

private fun parseQWeatherTimeOrNull(input: String): Instant? {
    val normalized = if (input.substringAfter("T").takeWhile { it != '+' && it != '-' }.count { it == ':' } == 1) {
        input.replace("+", ":00+").replace(Regex("-(\\d\\d):(\\d\\d)$"), ":00-$1:$2")
    } else {
        input
    }
    return runCatching {
        Instant.fromEpochMilliseconds(OffsetDateTime.parse(normalized).toInstant().toEpochMilli())
    }.getOrNull()
}

private fun formatCoordinate(value: Double): String = String.format(Locale.US, "%.2f", value)

private fun coordinateParam(latitude: Double, longitude: Double): String =
    "${formatCoordinate(longitude)},${formatCoordinate(latitude)}"
