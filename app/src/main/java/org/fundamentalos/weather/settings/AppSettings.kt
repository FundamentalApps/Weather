package org.fundamentalos.weather.settings

import android.content.Context
import androidx.core.content.edit
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * The handful of switches the settings screen offers, backed by shared preferences and exposed as
 * Compose state so a change repaints straight away.
 */
class AppSettings(context: Context) {
    private val preferences = context.getSharedPreferences("weather_settings", Context.MODE_PRIVATE)

    /** Whether the background gradient keeps drifting; off freezes it and stops its redraws. */
    var animatedBackground: Boolean by persisted(KeyAnimatedBackground, true)

    /** Whether text on the background is cut from the blurred backdrop instead of being plain. */
    var glassText: Boolean by persisted(KeyGlassText, true)

    /** Whether to ask the server to place the device by IP when the system providers come up empty. */
    var ipLocationFallback: Boolean by persisted(KeyIpFallback, true)

    /** Which map layers are on. Null until the user touches a switch: the server picks until then. */
    var mapLayers: Set<String>?
        get() = mapLayersState
        set(value) {
            mapLayersState = value
            preferences.edit {
                if (value == null) remove(KeyMapLayers) else putStringSet(KeyMapLayers, value)
            }
        }

    private var mapLayersState by mutableStateOf<Set<String>?>(preferences.getStringSet(KeyMapLayers, null)?.toSet())

    /** Compose state that writes through to preferences whenever it is assigned. */
    private fun persisted(key: String, default: Boolean) =
        object : ReadWriteProperty<Any?, Boolean> {
            private var state by mutableStateOf(preferences.getBoolean(key, default))

            override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean = state

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
                state = value
                preferences.edit { putBoolean(key, value) }
            }
        }

    private companion object {
        const val KeyMapLayers = "map_layers"
        const val KeyAnimatedBackground = "animated_background"
        const val KeyGlassText = "glass_text"
        const val KeyIpFallback = "ip_location_fallback"
    }
}
