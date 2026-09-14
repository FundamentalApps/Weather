package org.fundamentalos.weather.ipc

import org.fundamentalos.weather.R

/**
 * Bridges the app's internal weather codes to the IPC contract.
 *
 * The app does not carry a raw WMO code: its snapshots come from the FundamentalOS backend, which
 * aggregates Open-Meteo / MET Norway / WAQI and normalises everything to a CMA-style numeric string
 * ("100" clear, "305" light rain, "499" snow, "501" fog, ...; see [org.fundamentalos.weather.ui.components.weatherIconFor]).
 * For the bundle wmoCode key we map that code back to the nearest WMO / Open-Meteo weather code,
 * and for the bundle conditionIcon key we pick one existing app drawable per condition (the app
 * itself draws multi-layer icons; a bound consumer only needs a single glyph).
 */
internal object WeatherCodeMapping {
    /** wmoCode value when the internal code has no sensible WMO equivalent (e.g. "999" unknown). */
    const val WMO_UNKNOWN = -1

    fun toWmoCode(iconCode: String): Int = WmoByCode[iconCode] ?: WMO_UNKNOWN

    fun toIconRes(iconCode: String): Int = IconByCode[iconCode] ?: R.drawable.weather_unknown_24dp

    // CMA-style code -> WMO weather interpretation code (WMO 4677 subset used by Open-Meteo).
    private val WmoByCode: Map<String, Int> = mapOf(
        "100" to 0, "150" to 0,          // clear
        "101" to 3, "151" to 3,          // mostly cloudy -> overcast
        "102" to 1, "152" to 1,          // mostly clear -> mainly clear
        "103" to 2, "153" to 2,          // partly cloudy
        "104" to 3,                      // overcast
        "300" to 80, "350" to 80,        // showers
        "301" to 82, "351" to 82,        // heavy showers -> violent rain showers
        "302" to 95, "303" to 95,        // thunderstorm
        "304" to 96,                     // thunderstorm with hail
        "305" to 61, "306" to 63, "307" to 65, "308" to 65, // light/moderate/heavy/extreme rain
        "309" to 51,                     // drizzle
        "310" to 65, "311" to 65, "312" to 65,              // torrential rain
        "313" to 66,                     // freezing rain
        "314" to 63, "315" to 63, "316" to 65, "317" to 65, "318" to 65, // rain ranges
        "399" to 63,                     // rain
        "400" to 71, "401" to 73, "402" to 75, "403" to 75, // light/moderate/heavy snow, blizzard
        "404" to 71, "405" to 71,        // rain & snow mix -> snow (WMO has no sleet)
        "406" to 85, "407" to 85, "456" to 85, "457" to 85, // snow showers
        "408" to 73, "409" to 75, "410" to 75,              // snow ranges
        "499" to 73,                     // snow
        "500" to 45, "501" to 45, "502" to 45, "503" to 45, "504" to 45, // fog / haze / dust
        "507" to 45, "508" to 45,        // sandstorm -> fog family (no WMO code)
        "509" to 48, "510" to 48,        // dense fog -> rime fog
        "511" to 45, "512" to 45, "513" to 45,              // haze
        "514" to 48, "515" to 48,        // heavy / extremely dense fog
        "900" to 0, "901" to 0,          // hot / cold -> clear (no WMO code)
        "999" to WMO_UNKNOWN,            // unknown
    )

    // CMA-style code -> a single representative drawable that already ships in the app.
    private val IconByCode: Map<String, Int> = mapOf(
        "100" to R.drawable.weather_clear_24dp,
        "150" to R.drawable.weather_clear_night_24dp,
        "101" to R.drawable.weather_mostly_cloudy_1_24dp,
        "151" to R.drawable.weather_mostly_cloudy_1_24dp,
        "102" to R.drawable.weather_mostly_clear_with_intermittent_clouds_1_24dp,
        "152" to R.drawable.weather_mostly_clear_with_intermittent_clouds_1_night_24dp,
        "103" to R.drawable.weather_partly_cloudy_1_24dp,
        "153" to R.drawable.weather_partly_cloudy_1_night_24dp,
        "104" to R.drawable.weather_overcast_24dp,
        "300" to R.drawable.weather_rain_2_24dp,
        "350" to R.drawable.weather_rain_2_24dp,
        "301" to R.drawable.weather_heavy_rain_2_24dp,
        "351" to R.drawable.weather_heavy_rain_2_24dp,
        "302" to R.drawable.weather_thunderstorm_1_24dp,
        "303" to R.drawable.weather_severe_thunderstorm_3_24dp,
        "304" to R.drawable.weather_thunderstorm_with_hail_3_24dp,
        "305" to R.drawable.weather_shower_3_24dp,
        "306" to R.drawable.weather_heavy_shower_3_24dp,
        "307" to R.drawable.weather_heavy_rain_2_24dp,
        "308" to R.drawable.weather_extreme_rain_2_24dp,
        "309" to R.drawable.weather_drizzle_2_24dp,
        "310" to R.drawable.weather_torrential_rain_24dp,
        "311" to R.drawable.weather_severe_torrential_rain_24dp,
        "312" to R.drawable.weather_extreme_severe_torrential_rain_24dp,
        "313" to R.drawable.weather_freezing_rain_2_24px,
        "314" to R.drawable.weather_light_to_moderate_rain_2_24dp,
        "315" to R.drawable.weather_moderate_to_heavy_rain_2_24dp,
        "316" to R.drawable.weather_heavy_to_torrential_rain_2_24dp,
        "317" to R.drawable.weather_torrential_to_severe_torrential_rain_24dp,
        "318" to R.drawable.weather_severe_torrential_to_extremely_severe_torrential_rain_24dp,
        "399" to R.drawable.weather_rain_2_24dp,
        "400" to R.drawable.weather_light_snow_2_24dp,
        "401" to R.drawable.weather_moderate_snow_2_24dp,
        "402" to R.drawable.weather_heavy_snow_24dp,
        "403" to R.drawable.weather_blizzard_24dp,
        "404" to R.drawable.weather_rain_and_snow_2_24dp,
        "405" to R.drawable.weather_rain_and_snow_2_24dp,
        "406" to R.drawable.weather_rain_and_snow_2_24dp,
        "407" to R.drawable.weather_light_snow_2_24dp,
        "456" to R.drawable.weather_rain_and_snow_2_24dp,
        "457" to R.drawable.weather_light_snow_2_24dp,
        "408" to R.drawable.weather_light_to_moderate_snow_2_24dp,
        "409" to R.drawable.weather_moderate_to_heavy_snow_2_24dp,
        "410" to R.drawable.weather_heavy_to_blizzard_snow_2_24dp,
        "499" to R.drawable.weather_snow_24dp,
        "500" to R.drawable.weather_light_fog_24dp,
        "501" to R.drawable.weather_fog_24dp,
        "502" to R.drawable.weather_haze_2_24dp,
        "503" to R.drawable.weather_dust_storm_2_24dp,
        "504" to R.drawable.weather_floating_dust_24dp,
        "507" to R.drawable.weather_sandstorm_2_24dp,
        "508" to R.drawable.weather_severe_sandstorm_2_24dp,
        "509" to R.drawable.weather_dense_fog_24dp,
        "510" to R.drawable.weather_severe_dense_fog_24dp,
        "511" to R.drawable.weather_moderate_haze_24dp,
        "512" to R.drawable.weather_heavy_haze_24dp,
        "513" to R.drawable.weather_severe_haze_24dp,
        "514" to R.drawable.weather_heavy_fog_24dp,
        "515" to R.drawable.weather_extreme_dense_fog_24dp,
        "900" to R.drawable.weather_hot_1_24dp,
        "901" to R.drawable.weather_cold_2_24dp,
        "999" to R.drawable.weather_unknown_24dp,
    )
}
