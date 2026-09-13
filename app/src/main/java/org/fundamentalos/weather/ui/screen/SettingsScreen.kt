package org.fundamentalos.weather.ui.screen

import org.fundamentalos.weather.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fundamentalos.weather.settings.AppSettings
import org.fundamentalos.weather.ui.components.PreferenceGroup
import org.fundamentalos.weather.ui.components.PreferenceLabels
import org.fundamentalos.weather.ui.components.PreferenceRow
import org.fundamentalos.weather.ui.components.PreferenceScreen
import org.fundamentalos.weather.ui.components.PreferenceSectionHeader
import org.fundamentalos.weather.ui.theme.PreviewThemeWithBg
import org.koin.compose.koinInject

/**
 * App settings: the switches that change how the app looks and locates you. Who the data comes
 * from and what the app is are pages of their own, under 更多.
 */
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onDataSourcesClick: () -> Unit,
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier,
    settings: AppSettings = koinInject(),
) {
    val appearance = listOf(
        Toggle(
            title = stringResource(R.string.background_animation),
            subtitle = stringResource(R.string.background_animation_description),
            checked = settings.animatedBackground,
            onCheckedChange = { settings.animatedBackground = it },
        ),
        Toggle(
            title = stringResource(R.string.glass_text),
            subtitle = stringResource(R.string.glass_text_description),
            checked = settings.glassText,
            onCheckedChange = { settings.glassText = it },
        ),
    )
    val location = listOf(
        Toggle(
            title = stringResource(R.string.ip_fallback),
            subtitle = stringResource(R.string.ip_fallback_description),
            checked = settings.ipLocationFallback,
            onCheckedChange = { settings.ipLocationFallback = it },
        ),
    )
    val more = listOf(
        stringResource(R.string.data_sources) to onDataSourcesClick,
        stringResource(R.string.about) to onAboutClick,
    )

    PreferenceScreen(title = stringResource(R.string.settings), onBack = onBackClick, modifier = modifier) {
        PreferenceSectionHeader(stringResource(R.string.appearance))
        PreferenceGroup(appearance.size) { index, shape -> ToggleRow(appearance[index], shape) }

        PreferenceSectionHeader(stringResource(R.string.location))
        PreferenceGroup(location.size) { index, shape -> ToggleRow(location[index], shape) }

        PreferenceSectionHeader(stringResource(R.string.more))
        PreferenceGroup(more.size) { index, shape ->
            val (title, onClick) = more[index]
            PreferenceRow(shape, onClick = onClick) {
                PreferenceLabels(title)
            }
        }
    }
}

/** One switch and what it does. */
private data class Toggle(
    val title: String,
    val subtitle: String,
    val checked: Boolean,
    val onCheckedChange: (Boolean) -> Unit,
)

@Composable
private fun ToggleRow(toggle: Toggle, shape: Shape) {
    PreferenceRow(shape, onClick = { toggle.onCheckedChange(!toggle.checked) }) {
        PreferenceLabels(toggle.title, Modifier.weight(1f), toggle.subtitle)
        Spacer(Modifier.width(16.dp))
        Switch(checked = toggle.checked, onCheckedChange = toggle.onCheckedChange)
    }
}

@Preview
@Composable
private fun Preview() {
    PreviewThemeWithBg {
        SettingsScreen(onBackClick = {}, onDataSourcesClick = {}, onAboutClick = {})
    }
}
