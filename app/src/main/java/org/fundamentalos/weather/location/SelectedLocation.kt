package org.fundamentalos.weather.location

import android.content.Context
import androidx.core.content.edit
import kotlinx.serialization.json.Json

/**
 * The one place the user is currently showing, persisted so it survives an app restart. A saved
 * place means "keep showing this"; empty means "follow the device's own location". This is the
 * single source of truth shared by the app UI, the lock-screen smartspace push, and the background
 * refresh worker, so all three stay on the place the user picked.
 */
class SelectedLocation(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences("weather_selected", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    /** The selected place, or null when following the device's own location. */
    fun load(): SavedPlace? {
        val stored = preferences.getString(KeySelected, null) ?: return null
        return runCatching { json.decodeFromString<SavedPlace>(stored) }.getOrNull()
    }

    fun save(place: SavedPlace) {
        preferences.edit { putString(KeySelected, json.encodeToString(place)) }
    }

    /** Hand back to the device's own location. */
    fun clear() {
        preferences.edit { remove(KeySelected) }
    }

    private companion object {
        const val KeySelected = "selected"
    }
}
