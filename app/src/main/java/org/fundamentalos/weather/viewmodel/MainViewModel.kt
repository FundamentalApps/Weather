package org.fundamentalos.weather.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.fundamentalos.weather.ui.componets.DailyWeatherInfo
import org.fundamentalos.weather.ui.componets.HourlyWeatherInfo
import org.fundamentalos.weather.ui.componets.MultilayerIcon
import org.fundamentalos.weather.ui.componets.WeatherIcons
import org.fundamentalos.weather.weather.domain.AirQuality
import org.fundamentalos.weather.weather.domain.CurrentWeather
import org.fundamentalos.weather.weather.domain.DailyForecast
import org.fundamentalos.weather.weather.domain.MinutelyPrecipitation
import org.fundamentalos.weather.weather.domain.WeatherWarning
import org.fundamentalos.weather.settings.AppSettings
import org.fundamentalos.weather.weather.provider.WeatherService
import org.fundamentalos.weather.weather.provider.fos.FosApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlin.time.Clock

private const val TAG = "MainViewModel"

private const val HourlyHours = 24

class MainViewModel(
    private val weatherService: WeatherService,
    private val fosApiClient: FosApiClient,
    private val settings: AppSettings,
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
    val dailyWeatherStatus = mutableStateOf(DailyWeatherStatus.Idle)
    val dailyWeather = mutableStateOf<List<DailyWeatherInfo>>(emptyList())

    val aqi = mutableStateOf<AirQuality?>(null)
    val hourlyWeather = mutableStateOf<List<HourlyWeatherInfo>>(emptyList())

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

    enum class DailyWeatherStatus {
        Idle, Pending, Ok, Error
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
            dailyWeatherStatus.value = DailyWeatherStatus.Pending
            try {
                val snapshot = weatherService.getWeather(latitude, longitude)
                if (!force) currentLocation.value?.let {
                    if (!type.betterThan(it.type)) {
                        Log.d(TAG, "updateLocation: dropping stale $type result, already have ${it.type}")
                        return@launch
                    }
                }
                // Map first, then publish every field in one snapshot: written one by one, Compose can
                // render a frame where, say, the daily forecast exists but the cards built from it do not.
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
                val dailyInfo = snapshot.daily.map { it.toDailyWeatherInfo() }
                // The card is a one-day strip; the source sends two days of hours.
                val hourlyInfo = snapshot.hourly.take(HourlyHours).map {
                    HourlyWeatherInfo(
                        time = it.time.toString(),
                        icon = iconCodeToWeatherIcon(it.condition.iconCode, it.condition.isDay ?: true),
                        description = it.condition.text,
                        conditionCode = it.condition.iconCode,
                        temp = it.tempCelsius
                    )
                }

                Snapshot.withMutableSnapshot {
                    currentLocation.value = location
                    // Kept apart from what is on screen: the locations page offers this one as
                    // "your location" even while a saved place is being shown.
                    if (type != LocationType.Saved) deviceLocation.value = location
                    cityStatus.value = CityStatus.Ok

                    weather.value = snapshot.current
                    weatherStatus.value = WeatherStatus.Ok

                    dailyForecast.value = snapshot.daily
                    dailyWeather.value = dailyInfo
                    dailyWeatherStatus.value = DailyWeatherStatus.Ok

                    aqi.value = snapshot.airQuality
                    minutelyPrecipitation.value = snapshot.minutelyPrecipitation
                    warnings.value = snapshot.warnings
                    hourlyWeather.value = hourlyInfo
                }
            } catch (e: Exception) {
                Log.e(TAG, "updateLocation: Failed to refresh weather", e)
                cityStatus.value = CityStatus.Error
                weatherStatus.value = WeatherStatus.Error
                dailyWeatherStatus.value = DailyWeatherStatus.Error
            }
        }
    }
}

private fun DailyForecast.toDailyWeatherInfo(): DailyWeatherInfo {
    return DailyWeatherInfo(
        date = date.toString(),
        icon = iconCodeToWeatherIcon(dayCondition.iconCode, dayCondition.isDay ?: true),
        description = dayCondition.text,
        conditionCode = dayCondition.iconCode,
        probability = precipitationProbabilityPercent?.let { "$it%" },
        tempMin = tempMinCelsius,
        tempMax = tempMaxCelsius
    )
}

/** Icon-code map: the shared numeric weather codes each map to a layered icon. */
private val dayMaps = mapOf<String, () -> MultilayerIcon>(
    "100" to { WeatherIcons.Clear },
    "101" to { WeatherIcons.MostlyCloudy },
    "102" to { WeatherIcons.MostlyClearWithIntermittentClouds },
    "103" to { WeatherIcons.PartlyCloudy },
    "104" to { WeatherIcons.Overcast },
    "300" to { WeatherIcons.Shower },
    "301" to { WeatherIcons.HeavyShower },
    "302" to { WeatherIcons.Thunderstorm },
    "303" to { WeatherIcons.SevereThunderstorm },
    "304" to { WeatherIcons.ThunderstormWithHail },
    "305" to { WeatherIcons.LightRain },
    "306" to { WeatherIcons.ModerateRain },
    "307" to { WeatherIcons.HeavyRain },
    "308" to { WeatherIcons.ExtremeRain },
    "309" to { WeatherIcons.Drizzle },
    "310" to { WeatherIcons.TorrentialRain },
    "311" to { WeatherIcons.SevereTorrentialRain },
    "312" to { WeatherIcons.ExtremelySevereTorrentialRain },
    "313" to { WeatherIcons.FreezingRain },
    "314" to { WeatherIcons.LightToModerateRain },
    "315" to { WeatherIcons.ModerateToHeavyRain },
    "316" to { WeatherIcons.HeavyToTorrentialRain },
    "317" to { WeatherIcons.TorrentialToSevereTorrentialRain },
    "318" to { WeatherIcons.SevereTorrentialToExtremelySevereTorrentialRain },
    "399" to { WeatherIcons.Rain },
    "400" to { WeatherIcons.LightSnow },
    "401" to { WeatherIcons.ModerateSnow },
    "402" to { WeatherIcons.HeavySnow },
    "403" to { WeatherIcons.Blizzard },
    "404" to { WeatherIcons.RainAndSnowMix },
    "405" to { WeatherIcons.RainAndSnow },
    "406" to { WeatherIcons.ShowerWithSnow },
    "407" to { WeatherIcons.SnowShower },
    "408" to { WeatherIcons.LightToModerateSnow },
    "409" to { WeatherIcons.ModerateToHeavySnow },
    "410" to { WeatherIcons.HeavyToBlizzardSnow },
    "499" to { WeatherIcons.Snow },
    "500" to { WeatherIcons.LightFog },
    "501" to { WeatherIcons.Fog },
    "502" to { WeatherIcons.Haze },
    "503" to { WeatherIcons.DustStorm },
    "504" to { WeatherIcons.FloatingDust },
    "507" to { WeatherIcons.Sandstorm },
    "508" to { WeatherIcons.SevereSandstorm },
    "509" to { WeatherIcons.DenseFog },
    "510" to { WeatherIcons.SevereDenseFog },
    "511" to { WeatherIcons.ModerateHaze },
    "512" to { WeatherIcons.HeavyHaze },
    "513" to { WeatherIcons.SevereHaze },
    "514" to { WeatherIcons.HeavyFog },
    "515" to { WeatherIcons.ExtremelyDenseFog },
    "900" to { WeatherIcons.Hot },
    "901" to { WeatherIcons.Cold },
    "999" to { WeatherIcons.Unknown },
)

private val nightMap = mapOf<String, () -> MultilayerIcon>(
    "150" to { WeatherIcons.ClearNight },
    "151" to { WeatherIcons.MostlyCloudyNight },
    "152" to { WeatherIcons.MostlyClearWithIntermittentCloudsNight },
    "153" to { WeatherIcons.PartlyCloudyNight },
    "104" to { WeatherIcons.Overcast },
    "350" to { WeatherIcons.ShowerNight },
    "351" to { WeatherIcons.HeavyShowerNight },
    "302" to { WeatherIcons.Thunderstorm },
    "303" to { WeatherIcons.SevereThunderstorm },
    "304" to { WeatherIcons.ThunderstormWithHail },
    "305" to { WeatherIcons.LightRain },
    "306" to { WeatherIcons.ModerateRain },
    "307" to { WeatherIcons.HeavyRain },
    "308" to { WeatherIcons.ExtremeRain },
    "309" to { WeatherIcons.Drizzle },
    "310" to { WeatherIcons.TorrentialRain },
    "311" to { WeatherIcons.SevereTorrentialRain },
    "312" to { WeatherIcons.ExtremelySevereTorrentialRain },
    "313" to { WeatherIcons.FreezingRain },
    "314" to { WeatherIcons.LightToModerateRain },
    "315" to { WeatherIcons.ModerateToHeavyRain },
    "316" to { WeatherIcons.HeavyToTorrentialRain },
    "317" to { WeatherIcons.TorrentialToSevereTorrentialRain },
    "318" to { WeatherIcons.SevereTorrentialToExtremelySevereTorrentialRain },
    "400" to { WeatherIcons.LightSnow },
    "401" to { WeatherIcons.ModerateSnow },
    "402" to { WeatherIcons.HeavySnow },
    "403" to { WeatherIcons.Blizzard },
    "404" to { WeatherIcons.RainAndSnowMix },
    "405" to { WeatherIcons.RainAndSnow },
    "456" to { WeatherIcons.ShowerWithSnowNight },
    "457" to { WeatherIcons.SnowShowerNight },
    "408" to { WeatherIcons.LightToModerateSnow },
    "409" to { WeatherIcons.ModerateToHeavySnow },
    "410" to { WeatherIcons.HeavyToBlizzardSnow },
    "499" to { WeatherIcons.Snow },
    "500" to { WeatherIcons.LightFog },
    "501" to { WeatherIcons.Fog },
    "502" to { WeatherIcons.Haze },
    "503" to { WeatherIcons.DustStorm },
    "504" to { WeatherIcons.FloatingDust },
    "507" to { WeatherIcons.Sandstorm },
    "508" to { WeatherIcons.SevereSandstorm },
    "509" to { WeatherIcons.DenseFog },
    "510" to { WeatherIcons.SevereDenseFog },
    "511" to { WeatherIcons.ModerateHaze },
    "512" to { WeatherIcons.HeavyHaze },
    "513" to { WeatherIcons.SevereHaze },
    "514" to { WeatherIcons.HeavyFog },
    "515" to { WeatherIcons.ExtremelyDenseFog },
    "900" to { WeatherIcons.Hot },
    "901" to { WeatherIcons.Cold },
    "999" to { WeatherIcons.Unknown }
)

private fun iconCodeToWeatherIcon(code: String): MultilayerIcon {
    return dayMaps[code]?.invoke() ?: nightMap[code]?.invoke() ?: WeatherIcons.Unknown
}

private fun iconCodeToWeatherIcon(code: String, isDay: Boolean): MultilayerIcon {
    return (if (isDay) dayMaps else nightMap)[code]?.invoke() ?: iconCodeToWeatherIcon(code)
}
