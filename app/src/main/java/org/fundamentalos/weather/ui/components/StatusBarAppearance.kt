package org.fundamentalos.weather.ui.components

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import org.fundamentalos.weather.ui.LocalScreenDepth
import java.util.TreeMap

/**
 * Which way round the system bar icons are drawn, decided by what is behind them rather than by
 * the system theme: the home screen's sky is bright in the afternoon even while the phone is in
 * dark mode.
 *
 * Screens stack, and the one on top decides. Each screen says what it wants at its own depth,
 * and the deepest — the one on top — is what the bars get; a screen underneath changing its
 * mind (the home page's sky turning to day under the map) changes nothing until it is on top
 * again, and a screen leaving hands the bars back to whatever is left. Setting the bars on the
 * way in and restoring "what was there" on the way out could not do that: a change underneath
 * would set the bars over the top screen, and the top screen's leave would then put back a
 * value the page underneath had already moved on from.
 */
@Composable
fun StatusBarAppearance(lightBackground: Boolean) {
    val view = LocalView.current
    val depth = LocalScreenDepth.current
    DisposableEffect(view, depth, lightBackground) {
        val window = (view.context as? Activity)?.window
        if (window == null || view.isInEditMode) return@DisposableEffect onDispose {}
        StatusBarWants.set(depth, lightBackground, window, view)
        onDispose { StatusBarWants.clear(depth, window, view) }
    }
}

/** What each screen on the stack wants of the bars, by depth; the deepest applies. */
private object StatusBarWants {
    private val wants = TreeMap<Int, Boolean>()

    fun set(depth: Int, lightBackground: Boolean, window: android.view.Window, view: android.view.View) {
        wants[depth] = lightBackground
        apply(window, view)
    }

    fun clear(depth: Int, window: android.view.Window, view: android.view.View) {
        wants.remove(depth)
        apply(window, view)
    }

    private fun apply(window: android.view.Window, view: android.view.View) {
        val light = wants.lastEntry()?.value ?: return
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = light
        controller.isAppearanceLightNavigationBars = light
    }
}
