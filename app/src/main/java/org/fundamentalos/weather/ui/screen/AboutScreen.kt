package org.fundamentalos.weather.ui.screen

import org.fundamentalos.weather.R
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.fundamentalos.weather.BuildConfig
import org.fundamentalos.weather.ui.components.PreferenceGroup
import org.fundamentalos.weather.ui.components.PreferenceLabels
import org.fundamentalos.weather.ui.components.PreferenceRow
import org.fundamentalos.weather.ui.components.PreferenceScreen
import org.fundamentalos.weather.ui.components.PreferenceSectionHeader
import org.fundamentalos.weather.ui.theme.PreviewThemeWithBg

/**
 * What this build is and who serves it. Built into FundamentalOS, the package name and the
 * provider go without saying and are left out.
 */
@Composable
fun AboutScreen(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    val general = buildList {
        add(stringResource(R.string.app_name) to stringResource(R.string.app_description))
        add(stringResource(R.string.version) to "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        if (!BuildConfig.INLINE) add(stringResource(R.string.package_name) to BuildConfig.APPLICATION_ID)
        add(stringResource(R.string.license) to "GNU GPL v3")
    }

    PreferenceScreen(title = stringResource(R.string.about), onBack = onBackClick, modifier = modifier) {
        PreferenceSectionHeader(stringResource(R.string.general))
        PreferenceGroup(general.size) { index, shape ->
            val (name, value) = general[index]
            PreferenceRow(shape) { PreferenceLabels(name, subtitle = value) }
        }

        if (!BuildConfig.INLINE) {
            PreferenceSectionHeader(stringResource(R.string.credits))
            PreferenceGroup(1) { _, shape ->
                PreferenceRow(shape) { PreferenceLabels(stringResource(R.string.provider), subtitle = "Fundamental OS") }
            }
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
