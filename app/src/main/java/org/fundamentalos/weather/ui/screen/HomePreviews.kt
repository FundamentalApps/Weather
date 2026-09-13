package org.fundamentalos.weather.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fundamentalos.weather.R
import org.fundamentalos.weather.ui.components.InfoCard
import org.fundamentalos.weather.ui.components.MultilayerIcon
import org.fundamentalos.weather.ui.components.WeatherIcons
import org.fundamentalos.weather.ui.theme.PreviewTheme
import org.fundamentalos.weather.ui.theme.PreviewThemeWithBg

@Preview
@Composable
private fun HomePreview() {
    PreviewThemeWithBg {
        HomeScreen(onMapClick = {}, onLocationsClick = {})
    }
}

/** Every weather icon and every warning icon, to eye them side by side. */
@OptIn(ExperimentalLayoutApi::class)
@Preview
@Composable
private fun IconsPreview() {
    PreviewTheme {
        Column {
            InfoCard {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    for (icon in WeatherIconSamples) MultilayerIcon(icon, "")
                }
            }

            Spacer(Modifier.height(12.dp))

            InfoCard {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    for (icon in WarningIconSamples) {
                        Icon(painterResource(icon), "", Modifier, MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

private val WeatherIconSamples = listOf(
    WeatherIcons.Clear, WeatherIcons.PartlyCloudy, WeatherIcons.MostlyClearWithIntermittentClouds,
    WeatherIcons.MostlyCloudy, WeatherIcons.Overcast, WeatherIcons.ClearNight, WeatherIcons.PartlyCloudyNight,
    WeatherIcons.MostlyClearWithIntermittentCloudsNight, WeatherIcons.MostlyCloudyNight, WeatherIcons.Shower,
    WeatherIcons.HeavyShower, WeatherIcons.ShowerNight, WeatherIcons.HeavyShowerNight, WeatherIcons.Thunderstorm,
    WeatherIcons.SevereThunderstorm, WeatherIcons.ThunderstormWithHail, WeatherIcons.LightRain,
    WeatherIcons.LightToModerateRain, WeatherIcons.ModerateRain, WeatherIcons.ModerateToHeavyRain,
    WeatherIcons.HeavyRain, WeatherIcons.HeavyToTorrentialRain, WeatherIcons.TorrentialRain,
    WeatherIcons.TorrentialToSevereTorrentialRain, WeatherIcons.SevereTorrentialRain,
    WeatherIcons.SevereTorrentialToExtremelySevereTorrentialRain, WeatherIcons.ExtremelySevereTorrentialRain,
    WeatherIcons.ExtremeRain, WeatherIcons.Drizzle, WeatherIcons.FreezingRain, WeatherIcons.Rain,
    WeatherIcons.LightSnow, WeatherIcons.LightToModerateSnow, WeatherIcons.ModerateSnow,
    WeatherIcons.ModerateToHeavySnow, WeatherIcons.HeavySnow, WeatherIcons.HeavyToBlizzardSnow, WeatherIcons.Blizzard,
    WeatherIcons.RainAndSnowMix, WeatherIcons.RainAndSnow, WeatherIcons.ShowerWithSnow, WeatherIcons.SnowShower,
    WeatherIcons.ShowerWithSnowNight, WeatherIcons.SnowShowerNight, WeatherIcons.Snow, WeatherIcons.LightFog,
    WeatherIcons.Fog, WeatherIcons.HeavyFog, WeatherIcons.DenseFog, WeatherIcons.SevereDenseFog,
    WeatherIcons.ExtremelyDenseFog, WeatherIcons.Haze, WeatherIcons.ModerateHaze, WeatherIcons.HeavyHaze,
    WeatherIcons.SevereHaze, WeatherIcons.FloatingDust, WeatherIcons.DustStorm, WeatherIcons.Sandstorm,
    WeatherIcons.SevereSandstorm, WeatherIcons.Hot, WeatherIcons.Cold, WeatherIcons.Unknown,
)

private val WarningIconSamples = listOf(
    R.drawable.warning_typhoon_24dp, R.drawable.warning_tornado_24dp, R.drawable.warning_rainstorm_24dp,
    R.drawable.warning_snow_storm_24dp, R.drawable.warning_cold_wave_24dp, R.drawable.warning_gale_24dp,
    R.drawable.warning_heat_wave_24dp, R.drawable.warning_downburst_24dp, R.drawable.warning_avalanche_24dp,
    R.drawable.warning_lightning_24dp, R.drawable.warning_hail_24dp, R.drawable.warning_frost_24dp,
    R.drawable.warning_heavy_fog_24dp,
)
