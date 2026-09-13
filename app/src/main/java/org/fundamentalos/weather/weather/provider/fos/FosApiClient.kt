package org.fundamentalos.weather.weather.provider.fos

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import java.util.Locale

/** Error from the FundamentalOS API. [code] mirrors the server's `detail.code`, or a local `network_error` / `parse_error`. */
class FosApiException(
    val status: Int,
    val code: String,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * Thin client for `https://api.fundamentalos.org`. One snapshot call returns everything the app needs;
 * `/geo/ip` is the location fallback for devices that cannot get a fix (no Google location services).
 */
class FosApiClient(baseUrl: String, private val locale: () -> Locale = { Locale.getDefault() }) {
    @PublishedApi
    internal val baseUrl: String = baseUrl.trim().removeSuffix("/")

    @PublishedApi
    internal val client: HttpClient = HttpClient(Android) {
        install(HttpTimeout) {
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
            socketTimeoutMillis = SOCKET_TIMEOUT_MS
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
        }
        install(ContentEncoding) {
            gzip()
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @PublishedApi
    internal val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
        namingStrategy = JsonNamingStrategy.SnakeCase
    }

    suspend fun snapshot(latitude: Double, longitude: Double, lang: String = preferredLang(locale())): FosSnapshot =
        get("/v1/weather/snapshot") {
            parameter("lat", formatCoordinate(latitude))
            parameter("lon", formatCoordinate(longitude))
            parameter("lang", lang)
        }

    suspend fun locateByIp(lang: String = preferredLang(locale())): FosIpLocation =
        get("/v1/weather/geo/ip") {
            parameter("lang", lang)
        }

    suspend fun reverse(latitude: Double, longitude: Double, lang: String = preferredLang(locale())): FosPlace =
        get("/v1/weather/geo/reverse") {
            parameter("lat", formatCoordinate(latitude))
            parameter("lon", formatCoordinate(longitude))
            parameter("lang", lang)
        }

    suspend fun mapLayers(lang: String = preferredLang(locale())): FosMapLayers =
        get("/v1/weather/map/layers") {
            parameter("lang", lang)
        }

    suspend fun search(query: String, lang: String = preferredLang(locale()), limit: Int = 10): List<FosPlace> =
        get("/v1/weather/geo/search") {
            parameter("q", query)
            parameter("lang", lang)
            parameter("limit", limit)
        }

    @PublishedApi
    internal suspend inline fun <reified T> get(path: String, crossinline block: HttpRequestBuilder.() -> Unit): T {
        val response = try {
            client.get(baseUrl + path) { block() }
        } catch (e: Exception) {
            throw FosApiException(0, "network_error", "FundamentalOS request failed: ${e.message}", e)
        }
        val content = response.bodyAsText()
        if (!response.status.isSuccess()) {
            val detail = runCatching { json.decodeFromString<FosErrorBody>(content).detail }.getOrNull()
            throw FosApiException(
                status = response.status.value,
                code = detail?.code ?: "http_${response.status.value}",
                message = detail?.message ?: content.take(200),
            )
        }
        return try {
            json.decodeFromString<T>(content)
        } catch (e: Exception) {
            throw FosApiException(response.status.value, "parse_error", "Failed to parse FundamentalOS response", e)
        }
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 10_000L
        private const val SOCKET_TIMEOUT_MS = 15_000L
        private const val REQUEST_TIMEOUT_MS = 20_000L

        /** The server localizes condition text, wind names and AQI categories; only zh and en exist today. */
        fun preferredLang(locale: Locale = Locale.getDefault()): String =
            if (locale.language.equals("zh", ignoreCase = true)) "zh" else "en"

        fun formatCoordinate(value: Double): String = String.format(Locale.ROOT, "%.4f", value)
    }
}
