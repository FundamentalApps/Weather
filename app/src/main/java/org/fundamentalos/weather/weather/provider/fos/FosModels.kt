package org.fundamentalos.weather.weather.provider.fos

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire models for the FundamentalOS weather API (the `/v1/weather` routes).
 * Keys are snake_case on the wire; [FosApiClient] applies [kotlinx.serialization.json.JsonNamingStrategy.SnakeCase].
 */
@Serializable
data class FosCondition(
    val icon: String,
    val text: String,
    val isDay: Boolean = true,
)

@Serializable
data class FosLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val city: String? = null,
    val province: String? = null,
    val country: String? = null,
    val timezone: String? = null,
)

@Serializable
data class FosCurrent(
    val observedAt: String,
    val tempC: Int,
    val feelsLikeC: Int,
    val condition: FosCondition,
    val windDegree: Int? = null,
    val windDirection: String = "",
    val windScale: String = "",
    val windSpeedKph: Int? = null,
    val windGustKph: Int? = null,
    val humidityPercent: Int? = null,
    val precipMm: Double? = null,
    val pressureHpa: Int? = null,
    val visibilityKm: Int? = null,
    val cloudPercent: Int? = null,
    val dewPointC: Int? = null,
)

@Serializable
data class FosDaily(
    val date: String,
    val day: FosCondition,
    val night: FosCondition,
    val tempMinC: Int,
    val tempMaxC: Int,
    val precipMm: Double? = null,
    val precipProbabilityPercent: Int? = null,
    val humidityPercent: Int? = null,
    val pressureHpa: Int? = null,
    val uvIndex: Int? = null,
    val sunrise: String? = null,
    val sunset: String? = null,
)

@Serializable
data class FosHourly(
    val time: String,
    val tempC: Int,
    val condition: FosCondition,
    val precipProbabilityPercent: Int? = null,
    val precipMm: Double? = null,
)

@Serializable
data class FosAirQuality(
    val aqi: Int,
    val level: Int,
    val category: String,
    val effect: String,
    val primaryPollutant: String? = null,
    val scale: String = "CN",
    val observedAt: String? = null,
    val station: String? = null,
    val source: String = "",
    @SerialName("pm2_5") val pm25: Double? = null,
    val pm10: Double? = null,
    val o3: Double? = null,
    val no2: Double? = null,
    val so2: Double? = null,
    val co: Double? = null,
)

@Serializable
data class FosMinutelyInterval(
    val time: String,
    val precipMm: Double,
    val type: String,
)

@Serializable
data class FosMinutely(
    val summary: String,
    val intervals: List<FosMinutelyInterval>,
    val willPrecipitateSoon: Boolean = false,
    /** "15min" where a high-resolution model exists, "interpolated" elsewhere (including China). */
    val resolution: String = "interpolated",
    val source: String = "",
)

@Serializable
data class FosWarning(
    val id: String,
    val title: String,
    val text: String,
    val severity: String,
    val typeName: String? = null,
    val sender: String? = null,
    val publishedAt: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
)

@Serializable
data class FosSources(
    val forecast: String,
    val airQuality: String? = null,
    val geocoding: String? = null,
)

@Serializable
data class FosSnapshot(
    val provider: String,
    val sources: FosSources,
    val fetchedAt: String,
    val cache: String = "",
    val location: FosLocation,
    val current: FosCurrent,
    val daily: List<FosDaily>,
    val hourly: List<FosHourly>,
    val airQuality: FosAirQuality? = null,
    val minutelyPrecipitation: FosMinutely? = null,
    val warnings: List<FosWarning> = emptyList(),
)

@Serializable
data class FosPlace(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val city: String? = null,
    val province: String? = null,
    val country: String? = null,
    val timezone: String? = null,
    val distanceKm: Double? = null,
)

@Serializable
data class FosIpLocation(
    val ip: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: String = "city",
    val place: FosPlace? = null,
    val source: String = "",
)

@Serializable
data class FosErrorDetail(
    val code: String = "unknown",
    val message: String = "",
)

@Serializable
data class FosErrorBody(
    val detail: FosErrorDetail? = null,
)


/**
 * What to draw over the base map. The server resolves the sources — RainViewer's frame path moves
 * every ten minutes — so the app only has to fetch tiles and paint the legend it is handed.
 */
@Serializable
data class FosMapLayers(val layers: List<FosMapLayer> = emptyList())

@Serializable
data class FosMapLayer(
    val id: String,
    val name: String,
    /** `xyz` fills {z}/{x}/{y}; `wms3857` fills {bbox} with the tile's square in mercator metres. */
    val scheme: String,
    val urlTemplate: String,
    val tileSize: Int = 256,
    val maxZoom: Int = 7,
    val opacity: Float = 1f,
    /** Whether the layer starts on, until the user says otherwise. */
    val defaultOn: Boolean = true,
    val attribution: String = "",
    val unit: String? = null,
    val observedAt: Long? = null,
    val legend: List<FosMapLegendStop> = emptyList(),
)

@Serializable
data class FosMapLegendStop(val value: Float, val color: String)
