package org.fundamentalos.weather.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.google.android.material.color.MaterialColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.toArgb
import org.fundamentalos.weather.weather.domain.WeatherWarning

@Composable
fun warningColorScheme(): WarningColorScheme =
    if (isSystemInDarkTheme()) warningDark else warningLight

/**
 * Resolve a QWeather `severityColor` value (White/Blue/Green/Yellow/Orange/Red/Black) to a
 * [ColorFamily]. Unknown / missing values fall back to Blue.
 */
@Composable
fun severityColorFamily(severityColor: String?): ColorFamily {
    val scheme = warningColorScheme()
    return when (severityColor?.trim()?.lowercase()) {
        "white" -> scheme.warningWhite
        "blue" -> scheme.warningBlue
        "green" -> scheme.warningGreen
        "yellow" -> scheme.warningYellow
        "orange" -> scheme.warningOrange
        "red" -> scheme.warningRed
        "black" -> scheme.warningBlack
        else -> scheme.warningBlue
    }
}

@Composable
fun WeatherWarning.colorFamily(): ColorFamily = severityColorFamily(severityColor)

/**
 * Harmonize every color in a [ColorFamily] against the current primary. White and Black preserve
 * their foreground/background contrast (harmonizing pure white/black would shift them noticeably).
 */
@Composable
fun ColorFamily.harmonized(preserveExtremes: Boolean = true): ColorFamily {
    val primary = MaterialTheme.colorScheme.primary.toArgb()
    fun Color.shift(): Color {
        if (preserveExtremes && (isNearWhite() || isNearBlack())) return this
        return Color(MaterialColors.harmonize(this.toArgb(), primary))
    }
    return ColorFamily(
        color = color.shift(),
        onColor = onColor.shift(),
        colorContainer = colorContainer.shift(),
        onColorContainer = onColorContainer.shift(),
    )
}

private fun Color.isNearWhite(): Boolean = red > 0.95f && green > 0.95f && blue > 0.95f

private fun Color.isNearBlack(): Boolean = red < 0.12f && green < 0.12f && blue < 0.12f
