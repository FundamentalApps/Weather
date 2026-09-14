package org.fundamentalos.weather.ipc

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Icon
import android.os.Bundle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import org.fundamentalos.weather.MainActivity
import org.fundamentalos.weather.ui.components.MultilayerIcon
import org.fundamentalos.weather.ui.components.weatherIconFor
import org.fundamentalos.weather.ui.theme.cloud
import org.fundamentalos.weather.ui.theme.dust
import org.fundamentalos.weather.ui.theme.hot
import org.fundamentalos.weather.ui.theme.moon
import org.fundamentalos.weather.ui.theme.rain
import org.fundamentalos.weather.ui.theme.sun
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import org.fundamentalos.weather.weather.domain.WeatherSnapshot as DomainWeatherSnapshot

/** Builds the values crossing the process boundary from the app's own weather data. */
internal object WeatherSnapshotFactory {

    private const val VALID_FOR_MILLIS = 2L * 60L * 60L * 1000L // suggested freshness window: 2h

    /** Square edge, in px, of the composited multi-layer condition bitmap handed across the IPC. */
    private const val ICON_SIZE_PX = 108

    /**
     * Fixed ARGB for a [MultilayerIcon.ColorTag]. The app resolves these through Compose theme and
     * a runtime harmonize toward the dynamic primary; neither is available here (this runs off the
     * main/Compose path, for another process to draw). We take the app's light-card base color for
     * each tag as a stable literal so the lock-screen glyph reads the same multi-colour shape.
     */
    private fun tagArgb(tag: MultilayerIcon.ColorTag): Int = when (tag) {
        MultilayerIcon.ColorTag.SUN -> sun
        MultilayerIcon.ColorTag.MOON -> moon
        MultilayerIcon.ColorTag.RAIN -> rain
        // Smartspace icon only: render cloud elements white (brighter on the dark lock
        // screen). The in-app icon keeps the theme grey via Compose, untouched.
        MultilayerIcon.ColorTag.CLOUD -> Color.White
        MultilayerIcon.ColorTag.DUST -> dust
        MultilayerIcon.ColorTag.HOT -> hot
    }.toArgb()

    /** Resolve one layer's tint to an ARGB without a Compose context. */
    private fun layerArgb(color: MultilayerIcon.IconColor): Int = when (color) {
        is MultilayerIcon.IconColor.Normal -> color.color.toArgb()
        is MultilayerIcon.IconColor.Harmonize -> color.color.toArgb()
        is MultilayerIcon.IconColor.Tag -> tagArgb(color.tag)
        // Only WeatherIcons.Unknown uses a @Composable color (onSurface); we cannot invoke it here,
        // so fall back to the neutral cloud grey. Unknown is itself a last-resort glyph.
        is MultilayerIcon.IconColor.Compute -> cloud.toArgb()
    }

    /**
     * Draw the app's own multi-layer condition icon into a single coloured [Icon]. Each layer is a
     * monochrome vector glyph tinted to its tag colour and stacked in list order (first at the back),
     * exactly as the in-app [org.fundamentalos.weather.ui.components.MultilayerIcon] composable does.
     * Returns null if nothing could be drawn, so the caller can fall back to the single-glyph icon.
     */
    private fun composeConditionIcon(context: Context, cached: CachedWeather): Icon? {
        val layers = weatherIconFor(cached.conditionCode, cached.isDay).layers
        if (layers.isEmpty()) return null
        val bitmap = Bitmap.createBitmap(ICON_SIZE_PX, ICON_SIZE_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        var drewAny = false
        for (layer in layers) {
            val drawable = ContextCompat.getDrawable(context, layer.resId)?.mutate() ?: continue
            DrawableCompat.setTint(drawable, layerArgb(layer.color))
            drawable.setBounds(0, 0, ICON_SIZE_PX, ICON_SIZE_PX)
            drawable.draw(canvas)
            drewAny = true
        }
        return if (drewAny) Icon.createWithBitmap(bitmap) else null
    }

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
        // Prefer the app's full colour multi-layer glyph (drawn to a bitmap); fall back to the
        // single monochrome resource glyph if compositing yields nothing. The consumer
        // (WeatherSmartspaceView) draws the icon un-tinted, so the colours survive to the lock screen.
        val conditionIcon = runCatching { composeConditionIcon(app, cached) }.getOrNull()
            ?: Icon.createWithResource(app.packageName, WeatherCodeMapping.toIconRes(cached.conditionCode))
        return Bundle().apply {
            putDouble("temperature", cached.temperature)
            putBoolean("useCelsius", cached.useCelsius)
            putInt("wmoCode", cached.wmoCode)
            putBoolean("isDay", cached.isDay)
            putString("description", cached.description)
            putParcelable("conditionIcon", conditionIcon)
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
