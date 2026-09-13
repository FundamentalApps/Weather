package org.fundamentalos.weather.ui.screen

import org.fundamentalos.weather.R
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.fundamentalos.weather.BuildConfig
import org.fundamentalos.weather.ui.componets.PreferenceGroup
import org.fundamentalos.weather.ui.componets.PreferenceLabels
import org.fundamentalos.weather.ui.componets.PreferenceRow
import org.fundamentalos.weather.ui.componets.PreferenceScreen
import org.fundamentalos.weather.ui.componets.PreferenceSectionHeader
import org.fundamentalos.weather.ui.theme.PreviewThemeWithBg

/** What this build is and who serves it. */
@Composable
fun AboutScreen(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    val general = listOf(
        stringResource(R.string.app_name) to stringResource(R.string.app_description),
        stringResource(R.string.version) to "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
        stringResource(R.string.package_name) to BuildConfig.APPLICATION_ID,
        stringResource(R.string.license) to "GNU GPL v3",
    )

    PreferenceScreen(title = stringResource(R.string.about), onBack = onBackClick, modifier = modifier) {
        PreferenceSectionHeader(stringResource(R.string.general))
        PreferenceGroup(general.size) { index, shape ->
            val (name, value) = general[index]
            PreferenceRow(shape) { PreferenceLabels(name, value) }
        }

        PreferenceSectionHeader(stringResource(R.string.credits))
        PreferenceGroup(1) { _, shape ->
            PreferenceRow(shape) { PreferenceLabels(stringResource(R.string.provider), "Fundamental OS") }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    PreviewThemeWithBg {
        AboutScreen(onBackClick = {})
    }
}
