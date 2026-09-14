package org.fundamentalos.weather.ipc

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.content.getSystemService
import org.fundamentalos.weather.weather.provider.fos.FosApiClient

/**
 * A background (non-Compose) way to place the device for the refresh worker, reusing the same
 * channels the app's [org.fundamentalos.weather.ui.components.LocationProvider] uses and no others:
 * the platform [LocationManager] last-known fix when the location permission is held, then the last
 * real device fix we have seen, then the FundamentalOS server's IP geolocation. No Google Play
 * Services.
 *
 * The remembered fix matters at night: with the screen off and no foreground app requesting
 * location, every provider's last-known reads null (especially right after a reboot), so without it
 * [resolve] would drop straight to the IP fallback -- which, behind a proxy, can geolocate to a
 * different, daytime place and show the wrong condition on the lock screen.
 */
class WeatherLocationResolver(
    private val context: Context,
    private val fosApiClient: FosApiClient,
) {
    data class Fix(val latitude: Double, val longitude: Double)

    private val fixPreferences =
        context.applicationContext.getSharedPreferences("weather_last_fix", Context.MODE_PRIVATE)

    suspend fun resolve(): Fix? =
        lastKnownFix()?.also { recordDeviceFix(it.latitude, it.longitude) }
            ?: lastGoodFix()
            ?: ipFix()

    /**
     * Remember the device's own real position. The foreground app calls this whenever a GPS/network
     * fix comes in, and [resolve] refreshes it from any last-known fix, so the worker can reuse it
     * later instead of the IP fallback.
     */
    fun recordDeviceFix(latitude: Double, longitude: Double) {
        fixPreferences.edit {
            putLong(KeyLatBits, latitude.toRawBits())
            putLong(KeyLonBits, longitude.toRawBits())
            putLong(KeyTimeMillis, System.currentTimeMillis())
        }
    }

    private fun lastKnownFix(): Fix? {
        if (!hasLocationPermission()) return null
        val manager = context.getSystemService<LocationManager>() ?: return null
        val newest = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        ).mapNotNull { provider ->
            runCatching {
                @Suppress("MissingPermission")
                manager.getLastKnownLocation(provider)
            }.getOrNull()
        }.maxByOrNull { it.time } ?: return null
        return Fix(newest.latitude, newest.longitude)
    }

    /** The last real device fix, as long as it is recent enough to still describe where we are. */
    private fun lastGoodFix(): Fix? {
        val recordedAt = fixPreferences.getLong(KeyTimeMillis, 0L)
        if (recordedAt == 0L) return null
        val age = System.currentTimeMillis() - recordedAt
        if (age < 0L || age > MaxFixAgeMillis) return null
        return Fix(
            Double.fromBits(fixPreferences.getLong(KeyLatBits, 0L)),
            Double.fromBits(fixPreferences.getLong(KeyLonBits, 0L)),
        )
    }

    private suspend fun ipFix(): Fix? = runCatching {
        val ip = fosApiClient.locateByIp()
        Fix(ip.latitude, ip.longitude)
    }.getOrNull()

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    private companion object {
        const val KeyLatBits = "lat_bits"
        const val KeyLonBits = "lon_bits"
        const val KeyTimeMillis = "time_millis"

        /** A device fix older than this is treated as too stale to still place the device. */
        const val MaxFixAgeMillis = 24L * 60 * 60 * 1000
    }
}
