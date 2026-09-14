package org.fundamentalos.weather.ipc

import android.content.Context
import androidx.core.content.edit
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The scalar weather values behind the IPC snapshot, persisted so that a fresh bind can answer
 * getCurrent() without waiting for a refresh. The system Parcelables (Icon, PendingIntent) are not
 * stored: they are rebuilt from [conditionCode] and the app package when a snapshot is handed out.
 */
@Serializable
data class CachedWeather(
    val temperature: Double,
    val useCelsius: Boolean,
    val wmoCode: Int,
    val isDay: Boolean,
    val description: String,
    val conditionCode: String,
    val locationName: String?,
    val observationTimeMillis: Long,
    val validUntilMillis: Long,
)

/** Persists the latest [CachedWeather] in its own shared-preferences file, as JSON. */
class WeatherProviderCache(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences("weather_ipc_cache", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): CachedWeather? {
        val stored = preferences.getString(KeyLatest, null) ?: return null
        return runCatching { json.decodeFromString<CachedWeather>(stored) }.getOrNull()
    }

    fun save(value: CachedWeather) {
        preferences.edit { putString(KeyLatest, json.encodeToString(value)) }
    }

    private companion object {
        const val KeyLatest = "latest"
    }
}
