package org.fundamentalos.weather.ui.screen

import org.fundamentalos.weather.R
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.fundamentalos.weather.ui.components.PreferenceGroup
import org.fundamentalos.weather.ui.components.PreferenceLabels
import org.fundamentalos.weather.ui.components.PreferenceRow
import org.fundamentalos.weather.ui.components.PreferenceScreen
import org.fundamentalos.weather.ui.theme.PreviewThemeWithBg

/** Who the numbers, the names and the pictures come from. */
@Composable
fun DataSourcesScreen(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    val sources = listOf(
        stringResource(R.string.forecast) to "Open-Meteo",
        stringResource(R.string.air_quality) to "Open-Meteo",
        stringResource(R.string.place_names) to "GeoNames",
        stringResource(R.string.ip_location) to "DB-IP Lite",
        stringResource(R.string.moon_texture) to stringResource(R.string.moon_texture_credit),
        stringResource(R.string.icons) to "Material Symbols",
    )

    PreferenceScreen(title = stringResource(R.string.data_sources), onBack = onBackClick, modifier = modifier) {
        PreferenceGroup(sources.size) { index, shape ->
            val (name, credit) = sources[index]
            PreferenceRow(shape) { PreferenceLabels(name, subtitle = credit) }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    PreviewThemeWithBg {
        DataSourcesScreen(onBackClick = {})
    }
}
