package org.fundamentalos.weather.ui.componets

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp as lerpFloat
import org.fundamentalos.weather.ui.text.NoFallbackText
import org.fundamentalos.weather.ui.theme.PreviewTheme
import kotlin.math.exp

/** Height of the temperature block when the page is scrolled to the top. */
val BannerExpandedHeight: Dp = 300.dp

/** Height it shrinks to once pinned under the status bar. */
val BannerCollapsedHeight: Dp = 40.dp

private val BannerExpandedTopPadding: Dp = 16.dp
private val BannerCollapsedTopPadding: Dp = 4.dp

/** Gap between the pinned headline and the top edge of the clipped content below it. */
val BannerContentGap: Dp = 24.dp

/**
 * How sharply the shrink front-loads; larger finishes the size change earlier in the scroll. Kept
 * gentle: at a flick's speed a steeper curve resolves within a frame or two and reads as a jump.
 */
private const val ShrinkDecay = 1.8f

private val ShrinkNormaliser = 1f - exp(-ShrinkDecay)

/** Distance from the status bar to the clipped content once the headline is pinned. */
val BannerPinnedContentOffset: Dp =
    BannerCollapsedTopPadding + BannerCollapsedHeight + BannerContentGap

/** Space the scroll column has to reserve above its first card to sit below the expanded headline. */
val BannerScrollReserve: Dp =
    BannerExpandedTopPadding + BannerExpandedHeight - BannerPinnedContentOffset

private val TemperatureSize = 57.sp
private val ConditionSize = 50.sp
private val HeadlineHeight = 81.dp

/**
 * Type is laid out once at its expanded size and scaled down from there. Interpolating the font size
 * instead would re-measure and re-lay-out the text on every scrolled frame.
 */
private const val CollapsedScale = 24f / 57f

/**
 * Space the separator sits in. The layout is sized for the collapsed form, where everything is
 * scaled down; while expanded the gap is pulled back to [SeparatorExpandedGap] by translating,
 * which keeps both ends right without re-laying-out anything mid-scroll.
 */
private val SeparatorLayoutGap = 42.dp
private val SeparatorExpandedGap = 14.dp

/**
 * Temperature and condition headline that collapses as the page scrolls, like Apple Weather:
 * `23° 少云` shrinks in place and pins under the status bar as `23° | 少云`.
 *
 * It is pinned outside the scrolling content, which is clipped to a rounded rect below it, so the
 * headline never needs a scrim. The caller must reserve [BannerScrollReserve] at the top of the
 * scroll column. [overscroll], when given, lets it follow the page's rubber band at the top.
 *
 * Every scroll-driven value is read inside a draw-phase lambda, so scrolling neither recomposes nor
 * re-lays-out anything here.
 */
@Composable
fun Banner(
    temperature: String,
    text: String,
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    overscroll: IosOverscrollState? = null,
    /** Whether the headline is arriving — rolling in — rather than already there. */
    enter: Boolean = false,
) {
    val density = LocalDensity.current
    // The headline travels 1:1 with the content: it is fully collapsed exactly when the space the
    // scroll column reserved for it has been scrolled away.
    val riseDistance = with(density) {
        ((BannerExpandedTopPadding + BannerExpandedHeight) -
            (BannerCollapsedTopPadding + BannerCollapsedHeight)).toPx()
    }
    // Centring is measured against the window, not this Box: callers may pad it asymmetrically, and
    // centring inside a lopsided box puts the headline off centre by half that padding.
    val windowWidth = LocalWindowInfo.current.containerSize.width.toFloat()
    var rowLeftInWindow by remember { mutableFloatStateOf(0f) }
    val gapPullback = with(density) { (SeparatorLayoutGap - SeparatorExpandedGap).toPx() }
    val onSurface = MaterialTheme.colorScheme.onSurface
    // The material replaces the fill, so the glyphs only act as a mask and must be drawn opaque.
    val glass = LocalGlassBackdrop.current != null

    fun fraction(): Float =
        if (riseDistance <= 0f) 0f else (scrollState.value / riseDistance).coerceIn(0f, 1f)

    fun shrink(fraction: Float): Float =
        ((1f - exp(-ShrinkDecay * fraction)) / ShrinkNormaliser).coerceIn(0f, 1f)

    Box(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = BannerExpandedTopPadding, start = 16.dp, end = 16.dp)
            .height(BannerExpandedHeight),
        contentAlignment = Alignment.BottomStart,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(HeadlineHeight)
                // Before the layer, so this is the layout position and the transform cannot feed back.
                .onGloballyPositioned { rowLeftInWindow = it.positionInWindow().x }
                .graphicsLayer {
                    val f = fraction()
                    val scale = lerpFloat(1f, CollapsedScale, shrink(f))
                    scaleX = scale
                    scaleY = scale
                    // Bottom left, so the baseline stays put and the block grows toward the centre.
                    transformOrigin = TransformOrigin(0f, 1f)
                    translationY = -riseDistance * f +
                        (overscroll?.offset?.coerceAtLeast(0f) ?: 0f)
                    // Slides from left-aligned to centred in the window as it pins.
                    val centred = (windowWidth - size.width * scale) / 2f - rowLeftInWindow
                    translationX = centred * f
                },
        ) {
            BlurRollText(
                text = "$temperature°",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = TemperatureSize),
                color = onSurface,
                enter = enter,
            )
            // The separator belongs to the collapsed form only.
            Box(
                Modifier
                    .width(SeparatorLayoutGap)
                    .graphicsLayer {
                        val s = shrink(fraction())
                        // squared, so it stays out of the way until the collapse is nearly done
                        alpha = s * s
                        // Sits centred in whatever the gap currently looks like.
                        translationX = -gapPullback / 2f * (1f - s)
                    },
                contentAlignment = Alignment.Center,
            ) {
                NoFallbackText(
                    text = "|",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = TemperatureSize,
                        fontWeight = FontWeight.Light,
                    ),
                    color = onSurface.copy(alpha = 0.4f),
                )
            }
            BlurRollText(
                text = text,
                modifier = Modifier
                    .graphicsLayer {
                        val s = shrink(fraction())
                        // Base colour carries the collapsed opacity; the layer dims it while expanded.
                        alpha = lerpFloat(0.4f / 0.75f, 1f, s)
                        // Closes the separator's gap back down while there is no separator to show.
                        translationX = -gapPullback * (1f - s)
                    }
                    // Inside the layer above: the mask is recorded at this node's size, so glyphs
                    // moved by an outer translation would be clipped at its edge.
                    .conditionTextGlassMaterial(),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = ConditionSize,
                    fontWeight = FontWeight.Medium,
                ),
                color = if (glass) Color.White else onSurface.copy(alpha = 0.75f),
                enter = enter,
            )
        }
    }
}

@Preview
@Composable
private fun ExpandedPreview() {
    PreviewTheme {
        Banner("24", text = "小雨", scrollState = rememberScrollState())
    }
}
