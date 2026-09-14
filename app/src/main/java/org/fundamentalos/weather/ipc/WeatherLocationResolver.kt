package org.fundamentalos.weather.ipc

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import org.fundamentalos.weather.weather.provider.fos.FosApiClient

/**
 * A background (non-Compose) way to place the device for the refresh worker, reusing the same
 * channels the app's [org.fundamentalos.weather.ui.components.LocationProvider] uses and no others:
 * the platform [LocationManager] last-known fix when the location permission is held, then the
 * FundamentalOS server's IP geolocation. No Google Play Services.
 */
class WeatherLocationResolver(
    private val context: Context,
    private val fosApiClient: FosApiClient,
) {
    data class Fix(val latitude: Double, val longitude: Double)

    suspend fun resolve(): Fix? = lastKnownFix() ?: ipFix()

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

    private suspend fun ipFix(): Fix? = runCatching {
        val ip = fosApiClient.locateByIp()
        Fix(ip.latitude, ip.longitude)
    }.getOrNull()

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }
}
