package org.fundamentalos.weather.ui.screen

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.fundamentalos.weather.ui.components.AppleWeatherCardTitleMaterial
import org.fundamentalos.weather.ui.components.AppleWeatherCardTitleMaterialLight
import org.fundamentalos.weather.ui.components.AppleWeatherConditionTextMaterial
import org.fundamentalos.weather.ui.components.AppleWeatherConditionTextMaterialLight
import org.fundamentalos.weather.ui.components.GlassBackdropState
import org.fundamentalos.weather.ui.components.rememberGlassBackdropState
import org.fundamentalos.weather.ui.theme.WeatherVisualScheme

/**
 * The Material colours the page's content takes from the sky: the cards' surfaces and the text
 * on them from the scheme's card colours, the accent from what harmonises with the sky.
 */
internal fun WeatherVisualScheme.contentColorScheme(base: ColorScheme): ColorScheme {
    val isLightCard = !useDarkCards
    val subtleContainer = harmonizeTarget.copy(alpha = if (isLightCard) 0.040f else 0.060f).compositeOver(card)
    val strongContainer = harmonizeTarget.copy(alpha = if (isLightCard) 0.065f else 0.090f).compositeOver(card)
    return base.copy(
        primary = harmonizeTarget,
        onPrimary = if (harmonizeTarget.luminance() < 0.5f) Color.White else Color(0xFF111414),
        surface = card,
        surfaceVariant = card,
        surfaceContainer = subtleContainer,
        surfaceContainerLow = subtleContainer,
        surfaceContainerHigh = strongContainer,
        surfaceContainerHighest = strongContainer,
        onSurface = onCard,
        onSurfaceVariant = onCardVariant,
        outlineVariant = onCardVariant.copy(alpha = 0.26f),
    )
}

/**
 * The glass backdrop the page's text is cut from, or null where there is none: the runtime
 * shaders behind it need API 33, and the setting can turn it off, and then every glass modifier
 * falls back to plain text. A bright sky needs the text cut out of the backdrop rather than
 * lifted off it, or every channel clamps to white.
 */
@Composable
internal fun rememberHomeGlassBackdrop(enabled: Boolean, darkCards: Boolean): GlassBackdropState? {
    if (!enabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
    return rememberGlassBackdropState(
        conditionTextMaterial = if (darkCards) AppleWeatherConditionTextMaterial else AppleWeatherConditionTextMaterialLight,
        cardTitleMaterial = if (darkCards) AppleWeatherCardTitleMaterial else AppleWeatherCardTitleMaterialLight,
    )
}
