package org.fundamentalos.weather.weather.provider.qweather

import org.fundamentalos.weather.api.exception.CaulumApiClientException
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import java.util.Locale
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

class QWeatherApiClient(
    @PublishedApi internal val auth: QWeatherAuth = QWeatherAuth(),
    @PublishedApi internal val locale: () -> Locale = { Locale.getDefault() },
) {
    @PublishedApi
    internal val client: HttpClient = HttpClient(Android) {
        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
            socketTimeoutMillis = SOCKET_TIMEOUT_MS
        }
    }

    @PublishedApi
    internal val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    fun buildUrl(host: String, path: String): String {
        val normalizedHost = host.trim().removeSuffix("/")
        val normalizedPath = path.removePrefix("/")
        return if (normalizedHost.startsWith("http://") || normalizedHost.startsWith("https://")) {
            "$normalizedHost/$normalizedPath"
        } else {
            "https://$normalizedHost/$normalizedPath"
        }
    }

    internal suspend inline fun <reified T> get(
        credentials: QWeatherCredentials,
        path: String,
        block: HttpRequestBuilder.() -> Unit = {},
    ): T {
        val response = try {
            client.get(buildUrl(credentials.host, path)) {
                bearerAuth(auth.createJwt(credentials))
                parameter("lang", preferredLanguage(locale()))
                block()
            }
        } catch (e: Exception) {
            throw CaulumApiClientException("QWeather request failed", e)
        }

        val content = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw CaulumApiClientException("QWeather response error, status code: ${response.status}, content: $content")
        }

        return try {
            json.decodeFromString<T>(content)
        } catch (e: Exception) {
            throw CaulumApiClientException("Failed to parse QWeather response, content: $content", e)
        }
    }

    companion object {
        /** https://dev.qweather.com/en/docs/resource/language/ */
        fun preferredLanguage(locale: Locale): String = when (val language = locale.language) {
            "de", "en", "fr", "pl", "ru" -> language
            "zh" -> if (locale.script == "Hant" || locale.country in setOf("TW", "HK", "MO")) "zh-hant" else "zh"
            else -> "en"
        }

        private const val CONNECT_TIMEOUT_MS = 10_000L
        private const val SOCKET_TIMEOUT_MS = 15_000L
        private const val REQUEST_TIMEOUT_MS = 20_000L
    }
}
