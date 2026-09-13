package org.fundamentalos.weather.ui.componets

import org.fundamentalos.weather.R
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.core.EaseInOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import org.fundamentalos.weather.ui.theme.WeatherHazeStyle

/** The bar a screen's content scrolls under. */
val AppBarHeight = 64.dp

/**
 * On top of the bar's own 4dp. Lines an icon's own strokes up with the 24dp text margin rather
 * than its box: an icon carries a couple of dp of padding of its own, so matching the boxes
 * leaves it looking indented.
 */
val AppBarIconAlignmentPad = 6.dp

/** The header a screen opens with: its mark, then the large title, both on the 24dp margin. */
val HeaderStartInset = 24.dp
private val HeaderTopGap = 16.dp
private val HeaderIconSize = 48.dp
private val HeaderTitleTopGap = 24.dp

/** How far a screen's own title travels under the bar before the bar's own is fully in. */
val TitleFadeSpan = 48.dp

/**
 * Ties a screen's large title to the small one in the bar it slides under: both positions are
 * recorded during layout, so scrolling only redraws the bar's title rather than recomposing it.
 */
@Stable
class CollapsingTitle {
    internal var headerY by mutableFloatStateOf(Float.NaN)
    internal var barBottom by mutableFloatStateOf(Float.NaN)

    /** 0 while the screen's own title is clear of the bar, 1 once it is [span] px under it. */
    fun barTitleAlpha(span: Float): Float {
        val header = headerY
        val bottom = barBottom
        return if (header.isNaN() || bottom.isNaN()) 0f else ((bottom - header) / span).coerceIn(0f, 1f)
    }

    /** The other half of the handover: the screen's own title goes as the bar's comes in. */
    fun headerTitleAlpha(span: Float): Float = 1f - barTitleAlpha(span)
}

@Composable
fun rememberCollapsingTitle(): CollapsingTitle = remember { CollapsingTitle() }

/** Marks the screen's own title, the one the bar takes over from. */
fun Modifier.collapsingTitleAnchor(state: CollapsingTitle): Modifier =
    onGloballyPositioned { state.headerY = it.positionInRoot().y }

/** Blur with nothing added on top: a tint would read as a bar across the content. */
@Composable
fun BlurOnlyHazeStyle() =
    WeatherHazeStyle().copy(tints = emptyList(), backgroundColor = Color.Transparent)

/**
 * Full blur against the top edge of the screen, gone by the bottom of the bar.
 *
 * The easing matters at both ends: haze's default holds the blur and drops it at the very end,
 * which leaves a visible line where the bar meets the content, while decaying early leaves the
 * middle of the bar barely blurred. Flat at both ends and steep in between gives a bar that is
 * properly frosted and still meets the content without a step.
 */
val TopEdgeProgressive = HazeProgressive.verticalGradient(
    easing = EaseInOut,
    startY = 0f,
    startIntensity = 1f,
    endY = Float.POSITIVE_INFINITY,
    endIntensity = 0f,
    preferPerformance = false,
)

/** Blurs whatever [hazeState] has recorded, strongest against the top edge of the screen. */
fun Modifier.topEdgeBlur(hazeState: HazeState, style: dev.chrisbanes.haze.HazeStyle): Modifier =
    hazeEffect(hazeState, style) {
        inputScale = HazeInputScale.Fixed(0.5f)
        progressive = TopEdgeProgressive
    }

/** The mirror of [TopEdgeProgressive], for a strip that sits against the bottom of the screen. */
val BottomEdgeProgressive = HazeProgressive.verticalGradient(
    easing = EaseInOut,
    startY = 0f,
    startIntensity = 0f,
    endY = Float.POSITIVE_INFINITY,
    endIntensity = 1f,
    preferPerformance = false,
)

/** Blurs whatever [hazeState] has recorded, strongest against the bottom edge of the screen. */
fun Modifier.bottomEdgeBlur(hazeState: HazeState, style: dev.chrisbanes.haze.HazeStyle): Modifier =
    hazeEffect(hazeState, style) {
        inputScale = HazeInputScale.Fixed(0.5f)
        progressive = BottomEdgeProgressive
    }

/**
 * A transparent bar that frosts the content passing under it.
 *
 * Whatever should be blurred has to be a `hazeSource` for the same [hazeState] and to be drawn
 * before this, which also means anything drawn after it stays sharp.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassTopAppBar(
    hazeState: HazeState,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    collapsingTitle: CollapsingTitle? = null,
    /** Whether the content passing under the bar is frosted; a map keeps its edge sharp. */
    blur: Boolean = true,
) {
    val style = BlurOnlyHazeStyle()
    val height = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + AppBarHeight
    Box(modifier.fillMaxWidth()) {
        // The blur is its own box: hung on the bar itself it takes the bar's measured bounds,
        // which are not the bounds it draws within.
        if (blur) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(height)
                    .topEdgeBlur(hazeState, style),
            )
        }
        TopAppBar(
            // The bar carries no title of its own until the screen's has slid under it.
            title = {
                if (collapsingTitle == null) {
                    title()
                } else {
                    Box(
                        Modifier.graphicsLayer {
                            alpha = collapsingTitle.barTitleAlpha(TitleFadeSpan.toPx())
                        }
                    ) { title() }
                }
            },
            navigationIcon = {
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.padding(start = AppBarIconAlignmentPad),
                    ) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            },
            actions = actions,
            expandedHeight = AppBarHeight,
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            modifier = Modifier.onGloballyPositioned {
                collapsingTitle?.barBottom = it.positionInRoot().y + it.size.height
            },
        )
    }
}

/**
 * The large title a screen's content starts with, under its mark if it has one. A screen without
 * a mark is shorter by exactly the mark's height: the gaps around it stay.
 *
 * Emits into a [Column]; the caller adds whatever gap its content wants below.
 */
@Composable
fun ColumnScope.CollapsingLargeTitle(
    title: String,
    collapsingTitle: CollapsingTitle,
    icon: ImageVector? = null,
) {
    Spacer(
        Modifier.height(
            WindowInsets.statusBars.asPaddingValues().calculateTopPadding() +
                AppBarHeight + HeaderTopGap
        )
    )
    if (icon != null) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .padding(start = HeaderStartInset)
                .size(HeaderIconSize),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
    Text(
        text = title,
        modifier = Modifier
            .padding(start = HeaderStartInset, top = HeaderTitleTopGap)
            .collapsingTitleAnchor(collapsingTitle)
            .graphicsLayer {
                alpha = collapsingTitle.headerTitleAlpha(TitleFadeSpan.toPx())
            },
        style = MaterialTheme.typography.displayMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}
