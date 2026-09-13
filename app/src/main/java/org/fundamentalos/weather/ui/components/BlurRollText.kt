package org.fundamentalos.weather.ui.components

import android.os.Build
import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import org.fundamentalos.weather.ui.text.NoFallbackText

/**
 * Text whose changes roll like a wheel, the way Apple's headlines change: the old text blurs as it
 * rolls off the top, and the new one rolls in from below and comes into focus. With [enter], the
 * first text rolls in the same way instead of simply being there.
 *
 * The text takes the width of what it shows, and moves to it rather than jumping, so whatever
 * sits beside it slides along; the text on its way out has no width of its own. The roll is
 * clipped above and below, like a wheel in a slot, and not at the sides, where a wider text is
 * still growing into its width. Every animated value is read in a draw-phase block, so a roll
 * redraws and never recomposes.
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
    val blurPx = with(LocalDensity.current) { MaxBlur.toPx() }

    Box(
        modifier
            .animateContentSize(tween(ResizeMillis, easing = FastOutSlowInEasing))
            .drawWithContent {
                clipRect(left = -size.width * 8f, top = 0f, right = size.width * 9f, bottom = size.height) {
                    this@drawWithContent.drawContent()
                }
            },
    ) {
        leaving?.let { old ->
            NoFallbackText(
                text = old,
                style = style,
                color = color,
                modifier = Modifier
                    // Drawn at its own size, but takes up no room: the slot is the new text's.
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        layout(0, 0) { placeable.place(0, 0) }
                    }
                    .graphicsLayer {
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

/** The width settles ahead of the roll, while the text arriving is still faint and soft. */
private const val ResizeMillis = 320

/** How far a line travels while it rolls, as a share of its height. */
private const val Travel = 0.7f

private val MaxBlur = 10.dp
