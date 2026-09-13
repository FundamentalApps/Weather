package org.fundamentalos.weather.ui.components

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Which way round the system bar icons are drawn, decided by what is behind them rather than by
 * the system theme: the home screen's sky is bright in the afternoon even while the phone is in
 * dark mode.
 *
 * Screens stack, so each one sets this on the way in and puts back what it found on the way out.
 */
@Composable
fun StatusBarAppearance(lightBackground: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, lightBackground) {
        val window = (view.context as? Activity)?.window
        if (window == null || view.isInEditMode) return@DisposableEffect onDispose {}
        val controller = WindowCompat.getInsetsController(window, view)
        val previousStatus = controller.isAppearanceLightStatusBars
        val previousNavigation = controller.isAppearanceLightNavigationBars
        controller.isAppearanceLightStatusBars = lightBackground
        controller.isAppearanceLightNavigationBars = lightBackground
        onDispose {
            controller.isAppearanceLightStatusBars = previousStatus
            controller.isAppearanceLightNavigationBars = previousNavigation
        }
    }
}
