package org.fundamentalos.weather.location

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.fundamentalos.weather.weather.provider.fos.FosPlace
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot

/** A place the user keeps, in the shape the rows show it: name, then the chain above it. */
@Serializable
data class SavedPlace(
    val name: String,
    val city: String? = null,
    val province: String? = null,
    val country: String? = null,
    val latitude: Double,
    val longitude: Double,
) {
    /** Coordinates from search and from the device never match exactly; a kilometre is the same city. */
    fun isSameSpot(other: SavedPlace): Boolean =
        abs(latitude - other.latitude) < SameSpotDegrees && abs(longitude - other.longitude) < SameSpotDegrees

    /** Whether the device is in this place rather than merely near it: city centres are coarse. */
    fun isAround(other: SavedPlace): Boolean {
        val northKm = (latitude - other.latitude) * KmPerDegree
        val eastKm = (longitude - other.longitude) * KmPerDegree *
            cos(((latitude + other.latitude) / 2).toRadians())
        return hypot(northKm, eastKm) <= HereRadiusKm
    }

    private fun Double.toRadians(): Double = this / 180.0 * PI

    companion object {
        private const val SameSpotDegrees = 0.01
        private const val KmPerDegree = 111.0
        private const val HereRadiusKm = 25.0

        fun of(place: FosPlace): SavedPlace = SavedPlace(
            name = place.name,
            city = place.city,
            province = place.province,
            country = place.country,
            latitude = place.latitude,
            longitude = place.longitude,
        )
    }
}

/** The saved places, kept in preferences and exposed as Compose state so a change repaints at once. */
@Stable
class SavedPlaces(context: Context) {
    private val preferences = context.getSharedPreferences("weather_places", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    var places: List<SavedPlace> by mutableStateOf(read())
        private set

    fun add(place: SavedPlace) {
        if (contains(place)) return
        places = places + place
        write()
    }

    fun remove(place: SavedPlace) {
        places = places.filterNot { it.isSameSpot(place) }
        write()
    }

    fun contains(place: SavedPlace): Boolean = places.any { it.isSameSpot(place) }

    private fun read(): List<SavedPlace> {
        val stored = preferences.getString(KeyPlaces, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<SavedPlace>>(stored) }.getOrDefault(emptyList())
    }

    private fun write() {
        preferences.edit().putString(KeyPlaces, json.encodeToString(places)).apply()
    }

    private companion object {
        const val KeyPlaces = "places"
    }
}
