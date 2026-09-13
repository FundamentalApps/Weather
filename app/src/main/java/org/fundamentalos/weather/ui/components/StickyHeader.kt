package org.fundamentalos.weather.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import com.kyant.shapes.RoundedRectangle
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Top edge of the scrolling clip window, in window coordinates (px). Cards read it to know when they
 * have been scrolled past it. Defaults to the top of the window so a card outside any scroll area
 * (a preview, for instance) simply never pins.
 */
val LocalScrollClipTop = staticCompositionLocalOf<FloatState> { mutableFloatStateOf(0f) }

/** Fade-out threshold for a card that has no pinned header of its own. */
private val DefaultMinVisible: Dp = 44.dp

/** How long a held card keeps fading, in header heights, before it is fully covered anyway. */
private const val HeldFadeSpan = 1.5f


/** Geometry shared between a card and the header it pins. Written during layout, read while drawing. */
@Stable
class StickyHeaderState {
    var containerTop by mutableFloatStateOf(0f)
    var containerHeight by mutableFloatStateOf(0f)
    var headerHeight by mutableFloatStateOf(0f)
}

@Composable
fun rememberStickyHeaderState(): StickyHeaderState = remember { StickyHeaderState() }

/**
 * Apply to a card root, outside its background, so it holds at the clip edge instead of being sliced.
 *
 * While the card has more than its header height left it scrolls normally. Below that the clip edge
 * would start cutting through the pinned header, so the card stops moving and holds there, showing
 * exactly its header, and fades out as the next card rides up and covers it.
 *
 * A card with no pinned header has nothing to protect, so it just fades over its last [minVisible].
 */
fun Modifier.stickyHeaderContainer(
    state: StickyHeaderState,
    clipTop: FloatState,
    minVisible: Dp = DefaultMinVisible,
    cornerRadius: Dp? = null,
): Modifier = this
    // Before the layer below, so the reported position is the layout one and the hold cannot feed back.
    .onGloballyPositioned {
        state.containerTop = it.positionInWindow().y
        state.containerHeight = it.size.height.toFloat()
    }
    .graphicsLayer {
        // Offscreen, so the card composites internally at full opacity and only the result is faded;
        // otherwise the body would show through its own pinned header.
        compositingStrategy = CompositingStrategy.Offscreen
        val visible = state.containerTop - clipTop.floatValue + state.containerHeight
        val header = state.headerHeight
        val held = if (header > 0f) (header - visible).coerceAtLeast(0f) else 0f

        translationY = held
        alpha = if (header > 0f) {
            (1f - held / (header * HeldFadeSpan)).coerceIn(0f, 1f)
        } else {
            (visible / minVisible.toPx()).coerceIn(0f, 1f)
        }

        if (cornerRadius != null) {
            // The card rounds its own cut rather than letting the scroll window's corners bite into it:
            // the edge that meets the clip always carries the card's own radius, so a card sliding under
            // it stays rounded and the held state is a clean pill.
            val shown = (visible + held).coerceIn(0f, state.containerHeight)
            clip = true
            shape = CutCardShape(
                topInset = state.containerHeight - shown,
                cornerRadius = cornerRadius,
            )
        }
    }

/**
 * The part of a card still below the clip edge, with the card's own continuous-curvature corners on
 * every side, so a card being cut keeps the same corner shape as one sitting untouched.
 */
private class CutCardShape(
    private val topInset: Float,
    private val cornerRadius: Dp,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val top = topInset.coerceIn(0f, size.height)
        val visible = Size(size.width, size.height - top)
        val outline = RoundedRectangle(cornerRadius).createOutline(visible, layoutDirection, density)
        if (top == 0f) return outline
        return Outline.Generic(
            Path().apply {
                addOutline(outline)
                translate(Offset(0f, top))
            }
        )
    }

    // So a card that is not moving keeps the same outline instead of rebuilding a pathevery frame.
    override fun equals(other: Any?): Boolean =
        other is CutCardShape && other.topInset == topInset && other.cornerRadius == cornerRadius

    override fun hashCode(): Int = topInset.hashCode() * 31 + cornerRadius.hashCode()
}

/**
 * Pins a card's header to [clipTop] while the body scrolls underneath, never letting it leave its own
 * card. Goes before the header's background so the background travels with it and hides the body.
 */
fun Modifier.stickyHeader(state: StickyHeaderState, clipTop: FloatState): Modifier = this
    .onSizeChanged { state.headerHeight = it.height.toFloat() }
    .graphicsLayer {
        val top = state.containerTop - clipTop.floatValue
        val maxPush = (state.containerHeight - state.headerHeight).coerceAtLeast(0f)
        translationY = (-top).coerceIn(0f, maxPush)
    }

/**
 * For a short row with no header of its own, such as the chips under the headline: the whole row acts
 * as its own header, so it holds at the clip edge fully visible and fades there while the next card
 * rides up over it, exactly like a pinned card header.
 *
 * Not for tall content, which would stick to the edge instead of scrolling away; that keeps
 * [fadeWhenClipped] instead.
 */
@Composable
fun Modifier.holdWhenClipped(): Modifier {
    val state = rememberStickyHeaderState()
    return this
        .onSizeChanged { state.headerHeight = it.height.toFloat() }
        .stickyHeaderContainer(state, LocalScrollClipTop.current)
}

/** Convenience for cards that have no header: fades them out once the clip edge starts eating them. */
@Composable
fun Modifier.fadeWhenClipped(minVisible: Dp = DefaultMinVisible): Modifier =
    stickyHeaderContainer(rememberStickyHeaderState(), LocalScrollClipTop.current, minVisible)
