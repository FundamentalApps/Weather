package org.fundamentalos.weather.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.fundamentalos.weather.weather.domain.AirQuality
import org.fundamentalos.weather.weather.domain.CurrentWeather
import org.fundamentalos.weather.weather.domain.DailyForecast
import org.fundamentalos.weather.weather.domain.HourlyForecast
import org.fundamentalos.weather.weather.domain.MinutelyPrecipitation
import org.fundamentalos.weather.weather.domain.WeatherWarning
import org.fundamentalos.weather.ipc.WeatherLocationResolver
import org.fundamentalos.weather.ipc.WeatherProviderCache
import org.fundamentalos.weather.ipc.WeatherSnapshotFactory
import org.fundamentalos.weather.ipc.WeatherUpdateBus
import org.fundamentalos.weather.settings.AppSettings
import org.fundamentalos.weather.weather.provider.WeatherService
import org.fundamentalos.weather.weather.provider.fos.FosApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlin.time.Clock

private const val TAG = "MainViewModel"

class MainViewModel(
    private val weatherService: WeatherService,
    private val fosApiClient: FosApiClient,
    private val settings: AppSettings,
    // Shared with the IPC provider/refresh worker: a foreground fetch updates it too, so the
    // lock-screen smartspace and the app read a single, latest weather source.
    private val weatherProviderCache: WeatherProviderCache,
    // Same resolver the refresh worker uses: a foreground fix is remembered here so the worker can
    // reuse the device's real position at night instead of falling back to a proxy-skewed IP fix.
    private val weatherLocationResolver: WeatherLocationResolver,
): ViewModel() {
    val neverShowPermissionDialog = mutableStateOf(false)

    val locationStatus = mutableStateOf(LocationStatus.new())
    val currentLocation = mutableStateOf<Location?>(null)

    /** The last place the device itself reported, whatever is being shown. */
    val deviceLocation = mutableStateOf<Location?>(null)

    val cityStatus = mutableStateOf(CityStatus.Idle)

    val weatherStatus = mutableStateOf(WeatherStatus.Init)
    val weather = mutableStateOf<CurrentWeather?>(null)

    val dailyForecast = mutableStateOf<List<DailyForecast>>(emptyList())
    val hourlyForecast = mutableStateOf<List<HourlyForecast>>(emptyList())

    val aqi = mutableStateOf<AirQuality?>(null)

    val minutelyPrecipitation = mutableStateOf<MinutelyPrecipitation?>(null)
    val warnings = mutableStateOf<List<WeatherWarning>>(emptyList())

    data class LocationStatus(
        val gpsStatus: GpsStatus,
        val netStatus: GpsStatus,
        val cacheStatus: GpsStatus,
        val passiveStatus: GpsStatus,
        val lastKnownStatus: GpsStatus,
        val ipStatus: GpsStatus = GpsStatus.Idle,
    ) {
        companion object {
            fun new(): LocationStatus {
                return LocationStatus(
                    gpsStatus = GpsStatus.Idle,
                    netStatus = GpsStatus.Idle,
                    cacheStatus = GpsStatus.Idle,
                    passiveStatus = GpsStatus.Idle,
                    lastKnownStatus = GpsStatus.Idle,
                    ipStatus = GpsStatus.Idle,
                )
            }
        }

        fun anyOk(): Boolean {
            return gpsStatus == GpsStatus.Ok ||
                    netStatus == GpsStatus.Ok ||
                    cacheStatus == GpsStatus.Ok ||
                    passiveStatus == GpsStatus.Ok ||
                    lastKnownStatus == GpsStatus.Ok ||
                    ipStatus == GpsStatus.Ok
        }

        fun anyPending(): Boolean {
            return gpsStatus == GpsStatus.Pending ||
                    netStatus == GpsStatus.Pending ||
                    cacheStatus == GpsStatus.Pending ||
                    passiveStatus == GpsStatus.Pending ||
                    lastKnownStatus == GpsStatus.Pending ||
                    ipStatus == GpsStatus.Pending
        }

        fun permissionDenied(): Boolean {
            return gpsStatus == GpsStatus.PermissionDenied && netStatus == GpsStatus.PermissionDenied
        }

        fun anyError(): Boolean {
            return gpsStatus == GpsStatus.Error ||
                    netStatus == GpsStatus.Error ||
                    cacheStatus == GpsStatus.Error ||
                    passiveStatus == GpsStatus.Error ||
                    lastKnownStatus == GpsStatus.Error ||
                    ipStatus == GpsStatus.Error
        }

        fun allError(): Boolean {
            return gpsStatus == GpsStatus.Error &&
                    netStatus == GpsStatus.Error &&
                    cacheStatus == GpsStatus.Error &&
                    passiveStatus == GpsStatus.Error &&
                    lastKnownStatus == GpsStatus.Error &&
                    ipStatus == GpsStatus.Error
        }

        /** Every channel has finished (Ok, Error or PermissionDenied) and none succeeded. */
        fun allFailed(): Boolean {
            val all = listOf(gpsStatus, netStatus, passiveStatus, lastKnownStatus, ipStatus)
            return all.none { it == GpsStatus.Ok } && all.none { it == GpsStatus.Idle || it == GpsStatus.Pending }
        }
    }

    enum class GpsStatus {
        Idle, Pending, Ok, Error,
        PermissionDenied
    }

    enum class CityStatus {
        Idle, Pending, Ok, Error
    }

    enum class WeatherStatus {
        Init, Requesting, Ok, Error
    }


    data class Location(
        val name: String,
        val city: String?,
        val province: String?,
        val country: String?,
        val cityId: String,
        val latitude: Double,
        val longitude: Double,
        val lastUpdateTime: Instant,
        val type: LocationType
    )

    /** Ordinal is priority: a result only replaces the current one if its type is later in this list. */
    enum class LocationType {
        Ip, Cache, LastKnow, Passive, Network, GPS,

        /** A place the user picked themselves; nothing the device reports may replace it. */
        Saved
    }

    fun updateLocationStatus(status: GpsStatus, locationType: LocationType) {
        when (locationType) {
            LocationType.GPS -> {
                locationStatus.value = locationStatus.value.copy(gpsStatus = status)
            }
            LocationType.Network -> {
                locationStatus.value = locationStatus.value.copy(netStatus = status)
            }
            LocationType.Passive -> {
                locationStatus.value = locationStatus.value.copy(passiveStatus = status)
            }
            LocationType.LastKnow -> {
                locationStatus.value = locationStatus.value.copy(lastKnownStatus = status)
            }
            LocationType.Cache -> {
                locationStatus.value = locationStatus.value.copy(cacheStatus = status)
            }
            LocationType.Ip -> {
                locationStatus.value = locationStatus.value.copy(ipStatus = status)
            }
            // A place the user picked has no channel of its own to report on.
            LocationType.Saved -> Unit
        }
    }

    /**
     * Text for the location slot while there is no resolved location yet.
     * Pending beats failure so the UI never flashes an error while a slower channel is still running.
     */
    val locationLabel: Int
        get() {
            val status = locationStatus.value
            return when {
                cityStatus.value == CityStatus.Error -> org.fundamentalos.weather.R.string.request_failed
                cityStatus.value == CityStatus.Pending -> org.fundamentalos.weather.R.string.requesting
                status.anyPending() -> org.fundamentalos.weather.R.string.locating
                status.permissionDenied() && status.ipStatus != GpsStatus.Ok -> org.fundamentalos.weather.R.string.permission_denied
                status.allFailed() -> org.fundamentalos.weather.R.string.location_failed
                else -> org.fundamentalos.weather.R.string.locating
            }
        }

    private var languageTag: String? = null

    /** Retained ViewModels refresh provider text when the activity is recreated in another language. */
    fun refreshForLanguage(tag: String) {
        val previous = languageTag
        languageTag = tag
        if (previous != null && previous != tag) {
            currentLocation.value?.let {
                updateLocationAndRefresh(it.latitude, it.longitude, it.type, force = true)
            }
        }
    }

    private var ipLocateStarted = false

    /**
     * City-level location from the caller's IP via the FundamentalOS server. Started alongside the
     * system providers: it usually answers first and paints a first snapshot, then GPS/network
     * results (higher [LocationType]) replace it. It is also the only channel that works when
     * Google location services are unreachable or the permission was denied.
     */
    fun locateByIp() {
        if (!settings.ipLocationFallback) return
        if (ipLocateStarted) return
        ipLocateStarted = true
        updateLocationStatus(GpsStatus.Pending, LocationType.Ip)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = fosApiClient.locateByIp()
                Log.d(TAG, "locateByIp: ${result.place?.name} (${result.latitude}, ${result.longitude})")
                updateLocationStatus(GpsStatus.Ok, LocationType.Ip)
                updateLocationAndRefresh(result.latitude, result.longitude, LocationType.Ip)
            } catch (e: Exception) {
                Log.w(TAG, "locateByIp: failed", e)
                updateLocationStatus(GpsStatus.Error, LocationType.Ip)
            }
        }
    }

    private fun LocationType.betterThan(type: LocationType): Boolean {
        return this.ordinal > type.ordinal
    }

    /** Bumped when the user asks for the device's own location again; re-runs every provider. */
    var locateRequestId by mutableIntStateOf(0)
        private set

    /** Show a place the user saved, and keep showing it until they choose otherwise. */
    fun selectPlace(latitude: Double, longitude: Double) {
        updateLocationAndRefresh(latitude, longitude, LocationType.Saved, force = true)
    }

    /** Hand the screen back to the device's own location. */
    fun useDeviceLocation() {
        currentLocation.value = null
        locationStatus.value = LocationStatus.new()
        ipLocateStarted = false
        locateRequestId++
    }

    /**
     * [force] is for a place the user picked: it replaces whatever is shown, including another
     * place of the same kind, which the priority check alone would drop.
     */
    fun updateLocationAndRefresh(
        latitude: Double,
        longitude: Double,
        type: LocationType,
        force: Boolean = false,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!force) currentLocation.value?.let {
                if (!type.betterThan(it.type)) {
                    return@launch
                }
            }

            Log.d(TAG, "updateLocation: lat $latitude, lon: $longitude")
            cityStatus.value = CityStatus.Pending
            weatherStatus.value = WeatherStatus.Requesting
            try {
                val snapshot = weatherService.getWeather(latitude, longitude)
                if (!force) currentLocation.value?.let {
                    if (!type.betterThan(it.type)) {
                        Log.d(TAG, "updateLocation: dropping stale $type result, already have ${it.type}")
                        return@launch
                    }
                }
                // Publish every field in one snapshot: written one by one, Compose can render a
                // frame where, say, the current weather is the new place's and the forecast the old.
                val location = Location(
                    name = snapshot.location.name,
                    city = snapshot.location.city,
                    province = snapshot.location.province,
                    country = snapshot.location.country,
                    cityId = snapshot.location.providerLocationId ?: snapshot.providerId,
                    latitude = snapshot.location.latitude,
                    longitude = snapshot.location.longitude,
                    lastUpdateTime = Clock.System.now(),
                    type = type,
                )
                Snapshot.withMutableSnapshot {
                    currentLocation.value = location
                    // Kept apart from what is on screen: the locations page offers this one as
                    // "your location" even while a saved place is being shown.
                    if (type != LocationType.Saved) deviceLocation.value = location
                    cityStatus.value = CityStatus.Ok

                    weather.value = snapshot.current
                    weatherStatus.value = WeatherStatus.Ok

                    dailyForecast.value = snapshot.daily
                    hourlyForecast.value = snapshot.hourly

                    aqi.value = snapshot.airQuality
                    minutelyPrecipitation.value = snapshot.minutelyPrecipitation
                    warnings.value = snapshot.warnings
                }

                // Remember the device's own real position so the background refresh worker can
                // reuse it at night, when the providers have no last-known fix and would otherwise
                // fall back to a coarse IP location. Only genuine device fixes qualify -- never IP,
                // a restored cache, or a saved place the user is merely browsing.
                if (type == LocationType.GPS || type == LocationType.Network ||
                    type == LocationType.Passive || type == LocationType.LastKnow
                ) {
                    weatherLocationResolver.recordDeviceFix(latitude, longitude)
                }

                // Keep the cross-process weather snapshot (lock-screen smartspace) in step with the
                // fetch we just showed, so opening the app refreshes both from one source. Only the
                // device's own location feeds it -- never a saved place the user is merely browsing.
                if (type != LocationType.Saved) {
                    runCatching {
                        val cached = WeatherSnapshotFactory.toCached(snapshot)
                        weatherProviderCache.save(cached)
                        // Wakes a live-bound WeatherProviderService to push the new Bundle to FI.
                        WeatherUpdateBus.publish(cached)
                    }.onFailure { Log.w(TAG, "updateLocation: IPC cache update failed", it) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "updateLocation: Failed to refresh weather", e)
                cityStatus.value = CityStatus.Error
                weatherStatus.value = WeatherStatus.Error
            }
        }
    }
}
