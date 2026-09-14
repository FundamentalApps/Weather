package org.fundamentalos.weather.ipc

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Bundle
import org.fundamentalos.weather.MainActivity
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import org.fundamentalos.weather.weather.domain.WeatherSnapshot as DomainWeatherSnapshot

/** Builds the values crossing the process boundary from the app's own weather data. */
internal object WeatherSnapshotFactory {

    private const val VALID_FOR_MILLIS = 2L * 60L * 60L * 1000L // suggested freshness window: 2h

    /** Reduce a freshly fetched domain snapshot to the scalars we persist. */
    fun toCached(domain: DomainWeatherSnapshot): CachedWeather {
        val current = domain.current
        val code = current.condition.iconCode
        val now = System.currentTimeMillis()
        return CachedWeather(
            // The app is Celsius-only; it has no temperature-unit setting, so useCelsius is always true.
            temperature = current.tempCelsius.toDouble(),
            useCelsius = true,
            wmoCode = WeatherCodeMapping.toWmoCode(code),
            isDay = current.condition.isDay ?: true,
            description = current.condition.text,
            conditionCode = code,
            locationName = domain.location.name,
            observationTimeMillis = parseTimeMillis(current.observedAt) ?: now,
            validUntilMillis = now + VALID_FOR_MILLIS,
        )
    }

    /**
     * Rebuild the cross-process weather values as a plain [Bundle] -- the contract the consumer
     * (FundamentalIntelligence) reads -- creating the system Parcelables against [context]. This is
     * the single place the cached scalars are turned into the wire Bundle; both getCurrent() and the
     * callback push go through here. The keys and their value types are the frozen IPC contract.
     */
    fun toBundle(context: Context, cached: CachedWeather): Bundle {
        val app = context.applicationContext
        val iconRes = WeatherCodeMapping.toIconRes(cached.conditionCode)
        return Bundle().apply {
            putDouble("temperature", cached.temperature)
            putBoolean("useCelsius", cached.useCelsius)
            putInt("wmoCode", cached.wmoCode)
            putBoolean("isDay", cached.isDay)
            putString("description", cached.description)
            // Addressed by this app's package so a consumer in another process can load it.
            putParcelable("conditionIcon", Icon.createWithResource(app.packageName, iconRes))
            putString("locationName", cached.locationName)
            putLong("observationTimeMillis", cached.observationTimeMillis)
            putLong("validUntilMillis", cached.validUntilMillis)
            putParcelable("tapIntent", launchIntent(app))
        }
    }

    private fun launchIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    /** The backend stamps observedAt as an ISO string; try the shapes it may take. */
    private fun parseTimeMillis(value: String): Long? {
        if (value.isBlank()) return null
        return runCatching { Instant.parse(value).toEpochMilli() }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }.getOrNull()
            ?: runCatching {
                LocalDateTime.parse(value).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }.getOrNull()
    }
}
