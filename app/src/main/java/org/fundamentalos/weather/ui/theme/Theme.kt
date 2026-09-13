package org.fundamentalos.weather.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.google.android.material.color.MaterialColors
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint

private val lightScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
)

private val darkScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
)

private val mediumContrastLightColorScheme = lightColorScheme(
    primary = primaryLightMediumContrast,
    onPrimary = onPrimaryLightMediumContrast,
    primaryContainer = primaryContainerLightMediumContrast,
    onPrimaryContainer = onPrimaryContainerLightMediumContrast,
    secondary = secondaryLightMediumContrast,
    onSecondary = onSecondaryLightMediumContrast,
    secondaryContainer = secondaryContainerLightMediumContrast,
    onSecondaryContainer = onSecondaryContainerLightMediumContrast,
    tertiary = tertiaryLightMediumContrast,
    onTertiary = onTertiaryLightMediumContrast,
    tertiaryContainer = tertiaryContainerLightMediumContrast,
    onTertiaryContainer = onTertiaryContainerLightMediumContrast,
    error = errorLightMediumContrast,
    onError = onErrorLightMediumContrast,
    errorContainer = errorContainerLightMediumContrast,
    onErrorContainer = onErrorContainerLightMediumContrast,
    background = backgroundLightMediumContrast,
    onBackground = onBackgroundLightMediumContrast,
    surface = surfaceLightMediumContrast,
    onSurface = onSurfaceLightMediumContrast,
    surfaceVariant = surfaceVariantLightMediumContrast,
    onSurfaceVariant = onSurfaceVariantLightMediumContrast,
    outline = outlineLightMediumContrast,
    outlineVariant = outlineVariantLightMediumContrast,
    scrim = scrimLightMediumContrast,
    inverseSurface = inverseSurfaceLightMediumContrast,
    inverseOnSurface = inverseOnSurfaceLightMediumContrast,
    inversePrimary = inversePrimaryLightMediumContrast,
    surfaceDim = surfaceDimLightMediumContrast,
    surfaceBright = surfaceBrightLightMediumContrast,
    surfaceContainerLowest = surfaceContainerLowestLightMediumContrast,
    surfaceContainerLow = surfaceContainerLowLightMediumContrast,
    surfaceContainer = surfaceContainerLightMediumContrast,
    surfaceContainerHigh = surfaceContainerHighLightMediumContrast,
    surfaceContainerHighest = surfaceContainerHighestLightMediumContrast,
)

private val highContrastLightColorScheme = lightColorScheme(
    primary = primaryLightHighContrast,
    onPrimary = onPrimaryLightHighContrast,
    primaryContainer = primaryContainerLightHighContrast,
    onPrimaryContainer = onPrimaryContainerLightHighContrast,
    secondary = secondaryLightHighContrast,
    onSecondary = onSecondaryLightHighContrast,
    secondaryContainer = secondaryContainerLightHighContrast,
    onSecondaryContainer = onSecondaryContainerLightHighContrast,
    tertiary = tertiaryLightHighContrast,
    onTertiary = onTertiaryLightHighContrast,
    tertiaryContainer = tertiaryContainerLightHighContrast,
    onTertiaryContainer = onTertiaryContainerLightHighContrast,
    error = errorLightHighContrast,
    onError = onErrorLightHighContrast,
    errorContainer = errorContainerLightHighContrast,
    onErrorContainer = onErrorContainerLightHighContrast,
    background = backgroundLightHighContrast,
    onBackground = onBackgroundLightHighContrast,
    surface = surfaceLightHighContrast,
    onSurface = onSurfaceLightHighContrast,
    surfaceVariant = surfaceVariantLightHighContrast,
    onSurfaceVariant = onSurfaceVariantLightHighContrast,
    outline = outlineLightHighContrast,
    outlineVariant = outlineVariantLightHighContrast,
    scrim = scrimLightHighContrast,
    inverseSurface = inverseSurfaceLightHighContrast,
    inverseOnSurface = inverseOnSurfaceLightHighContrast,
    inversePrimary = inversePrimaryLightHighContrast,
    surfaceDim = surfaceDimLightHighContrast,
    surfaceBright = surfaceBrightLightHighContrast,
    surfaceContainerLowest = surfaceContainerLowestLightHighContrast,
    surfaceContainerLow = surfaceContainerLowLightHighContrast,
    surfaceContainer = surfaceContainerLightHighContrast,
    surfaceContainerHigh = surfaceContainerHighLightHighContrast,
    surfaceContainerHighest = surfaceContainerHighestLightHighContrast,
)

private val mediumContrastDarkColorScheme = darkColorScheme(
    primary = primaryDarkMediumContrast,
    onPrimary = onPrimaryDarkMediumContrast,
    primaryContainer = primaryContainerDarkMediumContrast,
    onPrimaryContainer = onPrimaryContainerDarkMediumContrast,
    secondary = secondaryDarkMediumContrast,
    onSecondary = onSecondaryDarkMediumContrast,
    secondaryContainer = secondaryContainerDarkMediumContrast,
    onSecondaryContainer = onSecondaryContainerDarkMediumContrast,
    tertiary = tertiaryDarkMediumContrast,
    onTertiary = onTertiaryDarkMediumContrast,
    tertiaryContainer = tertiaryContainerDarkMediumContrast,
    onTertiaryContainer = onTertiaryContainerDarkMediumContrast,
    error = errorDarkMediumContrast,
    onError = onErrorDarkMediumContrast,
    errorContainer = errorContainerDarkMediumContrast,
    onErrorContainer = onErrorContainerDarkMediumContrast,
    background = backgroundDarkMediumContrast,
    onBackground = onBackgroundDarkMediumContrast,
    surface = surfaceDarkMediumContrast,
    onSurface = onSurfaceDarkMediumContrast,
    surfaceVariant = surfaceVariantDarkMediumContrast,
    onSurfaceVariant = onSurfaceVariantDarkMediumContrast,
    outline = outlineDarkMediumContrast,
    outlineVariant = outlineVariantDarkMediumContrast,
    scrim = scrimDarkMediumContrast,
    inverseSurface = inverseSurfaceDarkMediumContrast,
    inverseOnSurface = inverseOnSurfaceDarkMediumContrast,
    inversePrimary = inversePrimaryDarkMediumContrast,
    surfaceDim = surfaceDimDarkMediumContrast,
    surfaceBright = surfaceBrightDarkMediumContrast,
    surfaceContainerLowest = surfaceContainerLowestDarkMediumContrast,
    surfaceContainerLow = surfaceContainerLowDarkMediumContrast,
    surfaceContainer = surfaceContainerDarkMediumContrast,
    surfaceContainerHigh = surfaceContainerHighDarkMediumContrast,
    surfaceContainerHighest = surfaceContainerHighestDarkMediumContrast,
)

private val highContrastDarkColorScheme = darkColorScheme(
    primary = primaryDarkHighContrast,
    onPrimary = onPrimaryDarkHighContrast,
    primaryContainer = primaryContainerDarkHighContrast,
    onPrimaryContainer = onPrimaryContainerDarkHighContrast,
    secondary = secondaryDarkHighContrast,
    onSecondary = onSecondaryDarkHighContrast,
    secondaryContainer = secondaryContainerDarkHighContrast,
    onSecondaryContainer = onSecondaryContainerDarkHighContrast,
    tertiary = tertiaryDarkHighContrast,
    onTertiary = onTertiaryDarkHighContrast,
    tertiaryContainer = tertiaryContainerDarkHighContrast,
    onTertiaryContainer = onTertiaryContainerDarkHighContrast,
    error = errorDarkHighContrast,
    onError = onErrorDarkHighContrast,
    errorContainer = errorContainerDarkHighContrast,
    onErrorContainer = onErrorContainerDarkHighContrast,
    background = backgroundDarkHighContrast,
    onBackground = onBackgroundDarkHighContrast,
    surface = surfaceDarkHighContrast,
    onSurface = onSurfaceDarkHighContrast,
    surfaceVariant = surfaceVariantDarkHighContrast,
    onSurfaceVariant = onSurfaceVariantDarkHighContrast,
    outline = outlineDarkHighContrast,
    outlineVariant = outlineVariantDarkHighContrast,
    scrim = scrimDarkHighContrast,
    inverseSurface = inverseSurfaceDarkHighContrast,
    inverseOnSurface = inverseOnSurfaceDarkHighContrast,
    inversePrimary = inversePrimaryDarkHighContrast,
    surfaceDim = surfaceDimDarkHighContrast,
    surfaceBright = surfaceBrightDarkHighContrast,
    surfaceContainerLowest = surfaceContainerLowestDarkHighContrast,
    surfaceContainerLow = surfaceContainerLowDarkHighContrast,
    surfaceContainer = surfaceContainerDarkHighContrast,
    surfaceContainerHigh = surfaceContainerHighDarkHighContrast,
    surfaceContainerHighest = surfaceContainerHighestDarkHighContrast,
)

@Immutable
data class WarningColorScheme(
    val warningWhite: ColorFamily,
    val warningBlue: ColorFamily,
    val warningGreen: ColorFamily,
    val warningYellow: ColorFamily,
    val warningOrange: ColorFamily,
    val warningRed: ColorFamily,
    val warningBlack: ColorFamily,
)

val warningLight = WarningColorScheme(
    warningBlue = ColorFamily(
        warningBlueLight,
        onWarningBlueLight,
        warningBlueContainerLight,
        onWarningBlueContainerLight,
    ),
    warningYellow = ColorFamily(
        warningYellowLight,
        onWarningYellowLight,
        warningYellowContainerLight,
        onWarningYellowContainerLight,
    ),
    warningOrange = ColorFamily(
        warningOrangeLight,
        onWarningOrangeLight,
        warningOrangeContainerLight,
        onWarningOrangeContainerLight,
    ),
    warningRed = ColorFamily(
        warningRedLight,
        onWarningRedLight,
        warningRedContainerLight,
        onWarningRedContainerLight,
    ),
    warningGreen = ColorFamily(
        warningGreenLight,
        onWarningGreenLight,
        warningGreenContainerLight,
        onWarningGreenContainerLight,
    ),
    warningWhite = ColorFamily(
        warningWhiteLight,
        onWarningWhiteLight,
        warningWhiteContainerLight,
        onWarningWhiteContainerLight,
    ),
    warningBlack = ColorFamily(
        warningBlackLight,
        onWarningBlackLight,
        warningBlackContainerLight,
        onWarningBlackContainerLight,
    ),
)

val warningDark = WarningColorScheme(
    warningBlue = ColorFamily(
        warningBlueDark,
        onWarningBlueDark,
        warningBlueContainerDark,
        onWarningBlueContainerDark,
    ),
    warningYellow = ColorFamily(
        warningYellowDark,
        onWarningYellowDark,
        warningYellowContainerDark,
        onWarningYellowContainerDark,
    ),
    warningOrange = ColorFamily(
        warningOrangeDark,
        onWarningOrangeDark,
        warningOrangeContainerDark,
        onWarningOrangeContainerDark,
    ),
    warningRed = ColorFamily(
        warningRedDark,
        onWarningRedDark,
        warningRedContainerDark,
        onWarningRedContainerDark,
    ),
    warningGreen = ColorFamily(
        warningGreenDark,
        onWarningGreenDark,
        warningGreenContainerDark,
        onWarningGreenContainerDark,
    ),
    warningWhite = ColorFamily(
        warningWhiteDark,
        onWarningWhiteDark,
        warningWhiteContainerDark,
        onWarningWhiteContainerDark,
    ),
    warningBlack = ColorFamily(
        warningBlackDark,
        onWarningBlackDark,
        warningBlackContainerDark,
        onWarningBlackContainerDark,
    ),
)

val warningLightMediumContrast = WarningColorScheme(
    warningBlue = ColorFamily(
        warningBlueLightMediumContrast,
        onWarningBlueLightMediumContrast,
        warningBlueContainerLightMediumContrast,
        onWarningBlueContainerLightMediumContrast,
    ),
    warningYellow = ColorFamily(
        warningYellowLightMediumContrast,
        onWarningYellowLightMediumContrast,
        warningYellowContainerLightMediumContrast,
        onWarningYellowContainerLightMediumContrast,
    ),
    warningOrange = ColorFamily(
        warningOrangeLightMediumContrast,
        onWarningOrangeLightMediumContrast,
        warningOrangeContainerLightMediumContrast,
        onWarningOrangeContainerLightMediumContrast,
    ),
    warningRed = ColorFamily(
        warningRedLightMediumContrast,
        onWarningRedLightMediumContrast,
        warningRedContainerLightMediumContrast,
        onWarningRedContainerLightMediumContrast,
    ),
    warningGreen = ColorFamily(
        warningGreenLightMediumContrast,
        onWarningGreenLightMediumContrast,
        warningGreenContainerLightMediumContrast,
        onWarningGreenContainerLightMediumContrast,
    ),
    warningWhite = ColorFamily(
        warningWhiteLightMediumContrast,
        onWarningWhiteLightMediumContrast,
        warningWhiteContainerLightMediumContrast,
        onWarningWhiteContainerLightMediumContrast,
    ),
    warningBlack = ColorFamily(
        warningBlackLightMediumContrast,
        onWarningBlackLightMediumContrast,
        warningBlackContainerLightMediumContrast,
        onWarningBlackContainerLightMediumContrast,
    ),
)

val warningLightHighContrast = WarningColorScheme(
    warningBlue = ColorFamily(
        warningBlueLightHighContrast,
        onWarningBlueLightHighContrast,
        warningBlueContainerLightHighContrast,
        onWarningBlueContainerLightHighContrast,
    ),
    warningYellow = ColorFamily(
        warningYellowLightHighContrast,
        onWarningYellowLightHighContrast,
        warningYellowContainerLightHighContrast,
        onWarningYellowContainerLightHighContrast,
    ),
    warningOrange = ColorFamily(
        warningOrangeLightHighContrast,
        onWarningOrangeLightHighContrast,
        warningOrangeContainerLightHighContrast,
        onWarningOrangeContainerLightHighContrast,
    ),
    warningRed = ColorFamily(
        warningRedLightHighContrast,
        onWarningRedLightHighContrast,
        warningRedContainerLightHighContrast,
        onWarningRedContainerLightHighContrast,
    ),
    warningGreen = ColorFamily(
        warningGreenLightHighContrast,
        onWarningGreenLightHighContrast,
        warningGreenContainerLightHighContrast,
        onWarningGreenContainerLightHighContrast,
    ),
    warningWhite = ColorFamily(
        warningWhiteLightHighContrast,
        onWarningWhiteLightHighContrast,
        warningWhiteContainerLightHighContrast,
        onWarningWhiteContainerLightHighContrast,
    ),
    warningBlack = ColorFamily(
        warningBlackLightHighContrast,
        onWarningBlackLightHighContrast,
        warningBlackContainerLightHighContrast,
        onWarningBlackContainerLightHighContrast,
    ),
)

val warningDarkMediumContrast = WarningColorScheme(
    warningBlue = ColorFamily(
        warningBlueDarkMediumContrast,
        onWarningBlueDarkMediumContrast,
        warningBlueContainerDarkMediumContrast,
        onWarningBlueContainerDarkMediumContrast,
    ),
    warningYellow = ColorFamily(
        warningYellowDarkMediumContrast,
        onWarningYellowDarkMediumContrast,
        warningYellowContainerDarkMediumContrast,
        onWarningYellowContainerDarkMediumContrast,
    ),
    warningOrange = ColorFamily(
        warningOrangeDarkMediumContrast,
        onWarningOrangeDarkMediumContrast,
        warningOrangeContainerDarkMediumContrast,
        onWarningOrangeContainerDarkMediumContrast,
    ),
    warningRed = ColorFamily(
        warningRedDarkMediumContrast,
        onWarningRedDarkMediumContrast,
        warningRedContainerDarkMediumContrast,
        onWarningRedContainerDarkMediumContrast,
    ),
    warningGreen = ColorFamily(
        warningGreenDarkMediumContrast,
        onWarningGreenDarkMediumContrast,
        warningGreenContainerDarkMediumContrast,
        onWarningGreenContainerDarkMediumContrast,
    ),
    warningWhite = ColorFamily(
        warningWhiteDarkMediumContrast,
        onWarningWhiteDarkMediumContrast,
        warningWhiteContainerDarkMediumContrast,
        onWarningWhiteContainerDarkMediumContrast,
    ),
    warningBlack = ColorFamily(
        warningBlackDarkMediumContrast,
        onWarningBlackDarkMediumContrast,
        warningBlackContainerDarkMediumContrast,
        onWarningBlackContainerDarkMediumContrast,
    ),
)

val warningDarkHighContrast = WarningColorScheme(
    warningBlue = ColorFamily(
        warningBlueDarkHighContrast,
        onWarningBlueDarkHighContrast,
        warningBlueContainerDarkHighContrast,
        onWarningBlueContainerDarkHighContrast,
    ),
    warningYellow = ColorFamily(
        warningYellowDarkHighContrast,
        onWarningYellowDarkHighContrast,
        warningYellowContainerDarkHighContrast,
        onWarningYellowContainerDarkHighContrast,
    ),
    warningOrange = ColorFamily(
        warningOrangeDarkHighContrast,
        onWarningOrangeDarkHighContrast,
        warningOrangeContainerDarkHighContrast,
        onWarningOrangeContainerDarkHighContrast,
    ),
    warningRed = ColorFamily(
        warningRedDarkHighContrast,
        onWarningRedDarkHighContrast,
        warningRedContainerDarkHighContrast,
        onWarningRedContainerDarkHighContrast,
    ),
    warningGreen = ColorFamily(
        warningGreenDarkHighContrast,
        onWarningGreenDarkHighContrast,
        warningGreenContainerDarkHighContrast,
        onWarningGreenContainerDarkHighContrast,
    ),
    warningWhite = ColorFamily(
        warningWhiteDarkHighContrast,
        onWarningWhiteDarkHighContrast,
        warningWhiteContainerDarkHighContrast,
        onWarningWhiteContainerDarkHighContrast,
    ),
    warningBlack = ColorFamily(
        warningBlackDarkHighContrast,
        onWarningBlackDarkHighContrast,
        warningBlackContainerDarkHighContrast,
        onWarningBlackContainerDarkHighContrast,
    ),
)

@Immutable
data class ColorFamily(
    val color: Color,
    val onColor: Color,
    val colorContainer: Color,
    val onColorContainer: Color
)

val unspecified_scheme = ColorFamily(
    Color.Unspecified, Color.Unspecified, Color.Unspecified, Color.Unspecified
)

@Composable
fun WeatherHazeStyle(
    containerColor: Color = if (isSystemInDarkTheme()) {
        MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
    } else {
        MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp)
    },
    // No grain: it reads as banding under the progressive blur and as speckle on the glass buttons.
    noiseFactor: Float = 0f,
) = HazeStyle (
    blurRadius = 32.dp,
    backgroundColor = containerColor,
    tint = HazeTint(
        containerColor.copy(alpha = if (containerColor.luminance() >= 0.5) 0.35f else 0.55f),
    ),
    noiseFactor = noiseFactor,
)


@Composable
fun WeatherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable() () -> Unit
) {
    var colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> darkScheme
        else -> lightScheme
    }

    if (darkTheme) {
        colorScheme = colorScheme
            .copy(
                surface = colorScheme.surfaceContainer,
                surfaceContainer = colorScheme.surface
            )
    }

    val view = LocalView.current
    SideEffect {
        val window = (view.context as Activity).window
        window.navigationBarColor = Color.Transparent.toArgb() // Make MIUI happy ig
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun Color.harmonized(): Color {
    return Color(
        MaterialColors.harmonize(
            this.toArgb(), MaterialTheme.colorScheme.primary.toArgb()
        ).toInt()
    )
}

@Composable
fun PreviewTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) darkScheme else lightScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun PreviewThemeWithBg(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) darkScheme else lightScheme,
        typography = Typography,
        content = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                content()
            }
        }
    )
}