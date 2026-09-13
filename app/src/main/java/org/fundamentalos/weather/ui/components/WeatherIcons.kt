package org.fundamentalos.weather.ui.components

import androidx.compose.material3.MaterialTheme
import org.fundamentalos.weather.R
import org.fundamentalos.weather.ui.components.MultilayerIcon.ColorTag

object WeatherIcons {
    val Clear = multilayerIcon {
        layerTag(R.drawable.weather_clear_24dp, ColorTag.SUN)
    }
    val PartlyCloudy = multilayerIcon {
        layerTag(R.drawable.weather_partly_cloudy_1_24dp, ColorTag.SUN)
        layerTag(R.drawable.weather_partly_cloudy_2_24dp, ColorTag.CLOUD)
    }
    val MostlyCloudy = multilayerIcon {
        layerTag(R.drawable.weather_mostly_cloudy_1_24dp, ColorTag.CLOUD)
        layerTag(R.drawable.weather_mostly_cloudy_2_24dp, ColorTag.SUN)
    }
    val MostlyClearWithIntermittentClouds = multilayerIcon {
        layerTag(R.drawable.weather_mostly_clear_with_intermittent_clouds_1_24dp, ColorTag.SUN)
        layerTag(R.drawable.weather_mostly_clear_with_intermittent_clouds_2_24dp, ColorTag.CLOUD)
    }
    val Overcast = multilayerIcon {
        layerTag(R.drawable.weather_overcast_24dp, ColorTag.CLOUD)
    }
    val ClearNight = multilayerIcon {
        layerTag(R.drawable.weather_clear_night_24dp, ColorTag.MOON)
    }
    val PartlyCloudyNight = multilayerIcon {
        layerTag(R.drawable.weather_partly_cloudy_1_night_24dp, ColorTag.MOON)
        layerTag(R.drawable.weather_partly_cloudy_2_24dp, ColorTag.CLOUD)
    }
    val MostlyCloudyNight = multilayerIcon {
        layerTag(R.drawable.weather_mostly_cloudy_1_24dp, ColorTag.CLOUD)
        layerTag(R.drawable.weather_mostly_cloudy_2_night_24dp, ColorTag.MOON)
    }
    val MostlyClearWithIntermittentCloudsNight = multilayerIcon {
        layerTag(R.drawable.weather_mostly_clear_with_intermittent_clouds_1_night_24dp, ColorTag.MOON)
        layerTag(R.drawable.weather_mostly_clear_with_intermittent_clouds_2_24dp, ColorTag.CLOUD)
    }
    val Shower = multilayerIcon {
        layerTag(R.drawable.weather_shower_1_24dp, ColorTag.CLOUD)
        layerTag(R.drawable.weather_shower_2_24dp, ColorTag.SUN)
        layerTag(R.drawable.weather_shower_3_24dp, ColorTag.RAIN)
    }
    val HeavyShower = multilayerIcon {
        layerTag(R.drawable.weather_shower_1_24dp, ColorTag.CLOUD)
        layerTag(R.drawable.weather_shower_2_24dp, ColorTag.SUN)
        layerTag(R.drawable.weather_heavy_shower_3_24dp, ColorTag.RAIN)
    }
    val Thunderstorm = multilayerIcon {
        layerTag(R.drawable.weather_thunderstorm_1_24dp, ColorTag.CLOUD)
        layerTag(R.drawable.weather_thunderstorm_2_24dp, ColorTag.SUN)
    }
    val SevereThunderstorm = multilayerIcon {
        layerTag(R.drawable.weather_thunderstorm_1_24dp, ColorTag.CLOUD)
        layerTag(R.drawable.weather_thunderstorm_2_24dp, ColorTag.SUN)
        layerTag(R.drawable.weather_severe_thunderstorm_3_24dp, ColorTag.RAIN)
    }
    val ThunderstormWithHail = multilayerIcon {
        layerTag(R.drawable.weather_thunderstorm_1_24dp, ColorTag.CLOUD)
        layerTag(R.drawable.weather_thunderstorm_2_24dp, ColorTag.SUN)
        layerTag(R.drawable.weather_thunderstorm_with_hail_3_24dp, ColorTag.CLOUD)
    }
    val LightRain = multilayerIcon {
        layerTag(R.drawable.weather_thunderstorm_1_24dp, ColorTag.CLOUD)
        layerTag(R.drawable.weather_shower_3_24dp, ColorTag.RAIN)
    }
    val ModerateRain = multilayerIcon {
        layerTag(R.drawable.weather_thunderstorm_1_24dp, ColorTag.CLOUD)
        layerTag(R.drawable.weather_heavy_shower_3_24dp, ColorTag.RAIN)
    }
    val HeavyRain = multilayerIcon {
        layerTag(R.drawable.weather_thunderstorm_1_24dp, ColorTag.CLOUD)
        layerTag(R.drawable.weather_heavy_rain_2_24dp, ColorTag.RAIN)
    }
    val ExtremeRain = multilayerIcon {
        layerTag(R.drawable.weather_thunderstorm_1_24dp, ColorTag.CLOUD)
        layerTag(R.drawable.weather_extreme_rain_2_24dp, ColorTag.RAIN)
    }
    val Drizzle = multilayerIcon {
        layerTag(R.drawable.weather_thunderstorm_1_24dp, ColorTag.CLOUD)
        layerTag(R.drawable.weather_drizzle_2_24dp, ColorTag.RAIN)
    }
    val TorrentialRain = multilayerIcon {
        layerTag(R.drawable.weather_torrential_rain_24dp, ColorTag.RAIN)
    }
    val SevereTorrentialRain = multilayerIcon {
        layerTag(R.drawable.weather_severe_torrential_rain_24dp, ColorTag.RAIN)
    }
    val ExtremelySevereTorrentialRain = multilayerIcon {
        layerTag(R.drawable.weather_extreme_severe_torrential_rain_24dp, ColorTag.RAIN)
    }
    val FreezingRain = multilayerIcon {
        layerTag(R.drawable.weather_thunderstorm_1_24dp, ColorTag.CLOUD)
        layerTag(R.drawable.weather_freezing_rain_2_24px, ColorTag.RAIN)
        layerTag(R.drawable.weather_freezing_rain_3_24px, ColorTag.CLOUD)
    }
    val LightToModerateRain = multilayerIcon {
        layerTag(R.drawable.weather_thunderstorm_1_24dp, ColorTag.CLOUD)
        layerTag(R.drawable.weather_light_to_moderate_rain_2_24dp, ColorTag.RAIN)
    }
    val ModerateToHeavyRain = multilayerIcon {
        layerTag(R.drawable.weather_thunderstorm_1_24dp, ColorTag.CLOUD)
        layerTag(R.drawable.weather_moderate_to_heavy_rain_2_24dp, ColorTag.RAIN)
    }
    val HeavyToTorrentialRain = multilayerIcon {
        layerTag(R.drawable.weather_thunderstorm_1_24dp, ColorTag.CLOUD)
        layerTag(R.drawable.weather_heavy_to_torrential_rain_2_24dp, ColorTag.RAIN)
    }
    val TorrentialToSevereTorrentialRain = multilayerIcon {
        layerTag(R.drawable.weather_torrential_to_severe_torrential_rain_24dp, ColorTag.RAIN)
    }
    val SevereTorrentialToExtremelySevereTorrentialRain = multilayerIcon {
        layerTag(R.drawable.weather_severe_torrential_to_extremely_severe_torrential_rain_24dp, ColorTag.RAIN)
    }
    val ShowerNight = multilayerIcon {
        layerTag(R.drawable.weather_shower_1_24dp, ColorTag.CLOUD)
        layerTag(R.drawable.weather_shower_2_night_24dp, ColorTag.MOON)
        layerTag(R.drawable.weather_shower_3_24dp, ColorTag.RAIN)
    }
    val HeavyShowerNight = multilayerIcon {
        layerTag(R.drawable.weather_shower_1_24dp, ColorTag.CLOUD)
        layerTag(R.drawable.weather_shower_2_night_24dp, ColorTag.MOON)
        layerTag(R.drawable.weather_heavy_shower_3_24dp, ColorTag.RAIN)
    }
    val Rain = multilayerIcon {
        layerTag(R.drawable.weather_thunderstorm_1_24dp, ColorTag.CLOUD)
        layerTag(R.drawable.weather_rain_2_24dp, ColorTag.RAIN)
    }
    val LightSnow = multilayerIcon {
        layerTag(R.drawable.weather_thunderstorm_1_24dp, ColorTag.CLOUD)
        layerTag(R.drawable.weather_light_snow_2_24dp, ColorTag.CLOUD)
    }
    val ModerateSnow = multilayerIcon {
        layerTag(R.drawable.weather_thunderstorm_1_24dp, ColorTag.CLOUD)
        layerTag(R.drawable.weather_moderate_snow_2_24dp, ColorTag.CLOUD)
    }
    val HeavySnow = multilayerIcon {
        layerTag(R.drawable.weather_heavy_snow_24dp, ColorTag.CLOUD)
    }
    val Blizzard = multilayerIcon {
        layerTag(R.drawable.weather_blizzard_24dp, ColorTag.CLOUD)
    }
    val RainAndSnowMix = multilayerIcon {
        layerTag(R.drawable.weather_thunderstorm_1_24dp, ColorTag.CLOUD)
        layerTag(R.drawable.weather_light_snow_2_24dp, ColorTag.CLOUD)
        layerTag(R.drawable.weather_severe_thunderstorm_3_24dp, ColorTag.RAIN)
    }
    val RainAndSnow = multilayerIcon {
        layerTag(R.drawable.weather_thunderstorm_1_24dp, ColorTag.CLOUD)
        layerTag(R.drawable.weather_rain_and_snow_2_24dp, ColorTag.RAIN)
        layerTag(R.drawable.weather_rain_and_snow_3_24dp, ColorTag.CLOUD)
    }
    val ShowerWithSnow = multilayerIcon {
        layerTag(R.drawable.weather_shower_1_24dp, ColorTag.CLOUD)
        layerTag(R.drawable.weather_shower_2_24dp, ColorTag.SUN)
        layerTag(R.drawable.weather_rain_and_snow_2_24dp, ColorTag.RAIN)
        layerTag(R.drawable.weather_rain_and_snow_3_24dp, ColorTag.CLOUD)
    }
    val SnowShower  = multilayerIcon {
        layerTag(R.drawable.weather_shower_1_24dp, ColorTag.CLOUD)
        layerTag(R.drawable.weather_shower_2_24dp, ColorTag.SUN)
        layerTag(R.drawable.weather_light_snow_2_24dp, ColorTag.CLOUD)
    }
    val LightToModerateSnow = multilayerIcon {
        layerTag(R.drawable.weather_thunderstorm_1_24dp, ColorTag.CLOUD)
        layerTag(R.drawable.weather_light_to_moderate_snow_2_24dp, ColorTag.CLOUD)
    }
    val ModerateToHeavySnow = multilayerIcon {
        layerTag(R.drawable.weather_thunderstorm_1_24dp, ColorTag.CLOUD)
        layerTag(R.drawable.weather_moderate_to_heavy_snow_2_24dp, ColorTag.CLOUD)
    }
    val HeavyToBlizzardSnow = multilayerIcon {
        layerTag(R.drawable.weather_heavy_to_blizzard_snow_2_24dp, ColorTag.CLOUD)
    }
    val ShowerWithSnowNight = multilayerIcon {
        layerTag(R.drawable.weather_shower_1_24dp, ColorTag.CLOUD)
        layerTag(R.drawable.weather_shower_2_night_24dp, ColorTag.MOON)
        layerTag(R.drawable.weather_rain_and_snow_2_24dp, ColorTag.RAIN)
        layerTag(R.drawable.weather_rain_and_snow_3_24dp, ColorTag.CLOUD)
    }
    val SnowShowerNight  = multilayerIcon {
        layerTag(R.drawable.weather_shower_1_24dp, ColorTag.CLOUD)
        layerTag(R.drawable.weather_shower_2_night_24dp, ColorTag.MOON)
        layerTag(R.drawable.weather_light_snow_2_24dp, ColorTag.CLOUD)
    }
    val Snow = multilayerIcon {
        layerTag(R.drawable.weather_snow_24dp, ColorTag.CLOUD)
    }
    val LightFog = multilayerIcon {
        layerTag(R.drawable.weather_light_fog_24dp, ColorTag.CLOUD)
    }
    val Fog = multilayerIcon {
        layerTag(R.drawable.weather_fog_24dp, ColorTag.CLOUD)
    }
    val Haze = multilayerIcon {
        layerTag(R.drawable.weather_thunderstorm_1_24dp, ColorTag.CLOUD)
        layerTag(R.drawable.weather_haze_2_24dp, ColorTag.CLOUD)
    }
    val DustStorm = multilayerIcon {
        layerTag(R.drawable.weather_dust_storm_1_24dp, ColorTag.CLOUD)
        layerTag(R.drawable.weather_dust_storm_2_24dp, ColorTag.DUST)
    }
    val FloatingDust = multilayerIcon {
        layerTag(R.drawable.weather_floating_dust_24dp, ColorTag.DUST)
    }
    val Sandstorm = multilayerIcon {
        layerTag(R.drawable.weather_sandstorm_1_24dp, ColorTag.CLOUD)
        layerTag(R.drawable.weather_sandstorm_2_24dp, ColorTag.DUST)
    }
    val	SevereSandstorm = multilayerIcon {
        layerTag(R.drawable.weather_sandstorm_1_24dp, ColorTag.CLOUD)
        layerTag(R.drawable.weather_severe_sandstorm_2_24dp, ColorTag.DUST)
    }
    val DenseFog = multilayerIcon {
        layerTag(R.drawable.weather_dense_fog_24dp, ColorTag.CLOUD)
    }
    val SevereDenseFog = multilayerIcon {
        layerTag(R.drawable.weather_severe_dense_fog_24dp, ColorTag.CLOUD)
    }
    val ModerateHaze = multilayerIcon {
        layerTag(R.drawable.weather_moderate_haze_24dp, ColorTag.CLOUD)
    }
    val HeavyHaze = multilayerIcon {
        layerTag(R.drawable.weather_heavy_haze_24dp, ColorTag.CLOUD)
    }
    val SevereHaze = multilayerIcon {
        layerTag(R.drawable.weather_severe_haze_24dp, ColorTag.CLOUD)
    }
    val HeavyFog = multilayerIcon {
        layerTag(R.drawable.weather_heavy_fog_24dp, ColorTag.CLOUD)
    }
    val ExtremelyDenseFog = multilayerIcon {
        layerTag(R.drawable.weather_extreme_dense_fog_24dp, ColorTag.CLOUD)
    }
    val Hot = multilayerIcon {
        layerTag(R.drawable.weather_hot_3_24dp, ColorTag.SUN)
        layerTag(R.drawable.weather_hot_2_24dp, ColorTag.HOT)
        layerTag(R.drawable.weather_hot_1_24dp, ColorTag.CLOUD)
    } // Bottom draws front
    val Cold = multilayerIcon {
        layerTag(R.drawable.weather_cold_3_24dp, ColorTag.CLOUD)
        layerTag(R.drawable.weather_cold_2_24dp, ColorTag.RAIN)
        layerTag(R.drawable.weather_hot_1_24dp, ColorTag.CLOUD)
    }
    val Unknown = multilayerIcon {
        layer(R.drawable.weather_unknown_24dp){ MaterialTheme.colorScheme.onSurface }
    } // Kinda scary
}
