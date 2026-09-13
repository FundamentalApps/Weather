package org.fundamentalos.weather.ui.componets

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import org.fundamentalos.weather.ui.text.NoFallbackText

/**
 * Text whose changes roll like a wheel, the way Apple's headlines change: the old text blurs as it
 * rolls off the top, and the new one rolls in from below and comes into focus. With [enter], the
 * first text rolls in the same way instead of simply being there.
 *
 * Every animated value is read in a draw-phase block, so a roll redraws and never recomposes.
 */
@Composable
fun BlurRollText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    enter: Boolean = false,
) {
    var shown by remember { mutableStateOf(text) }
    var leaving by remember { mutableStateOf<String?>(null) }
    val roll = remember { Animatable(if (enter) 0f else 1f) }
    LaunchedEffect(text) {
        if (shown != text) {
            leaving = shown
            shown = text
            roll.snapTo(0f)
        }
        if (roll.value < 1f) roll.animateTo(1f, tween(RollMillis, easing = FastOutSlowInEasing))
        leaving = null
    }
    val blurPx = with(androidx.compose.ui.platform.LocalDensity.current) { MaxBlur.toPx() }

    Box(modifier.clipToBounds()) {
        leaving?.let { old ->
            NoFallbackText(
                text = old,
                style = style,
                color = color,
                modifier = Modifier.graphicsLayer {
                    val p = roll.value
                    alpha = (1f - p) * (1f - p)
                    translationY = -p * size.height * Travel
                    renderEffect = blur(p * blurPx)
                },
            )
        }
        NoFallbackText(
            text = shown,
            style = style,
            color = color,
            modifier = Modifier.graphicsLayer {
                val p = roll.value
                alpha = p * p
                translationY = (1f - p) * size.height * Travel
                renderEffect = blur((1f - p) * blurPx)
            },
        )
    }
}

private fun blur(radiusPx: Float) =
    if (radiusPx > 0.5f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) BlurEffect(radiusPx, radiusPx, TileMode.Decal) else null

private const val RollMillis = 520

/** How far a line travels while it rolls, as a share of its height. */
private const val Travel = 0.7f

private val MaxBlur = 10.dp
