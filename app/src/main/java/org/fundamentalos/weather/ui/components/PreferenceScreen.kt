package org.fundamentalos.weather.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.kyant.shapes.RoundedCornerStyle
import com.kyant.shapes.UnevenRoundedRectangle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

/** A group of settings: one rounded block of rows, big corners outside, small ones between. */
private val GroupMargin = 16.dp
private val GroupCorner = 24.dp
private val RowCorner = 4.dp
private val RowGap = 4.dp
private val RowPadding = 16.dp

/** The gap between the screen's title and its first group. */
private val TitleToContentGap = 24.dp

/**
 * A page of grouped settings: the large title the bar takes over as it scrolls under, iOS scroll
 * physics, and the same frosted bar as the rest of the app.
 */
@Composable
fun PreferenceScreen(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val hazeState = remember { HazeState() }
    val scrollState = rememberScrollState()
    val overscrollState = rememberIosOverscrollState()
    val collapsingTitle = rememberCollapsingTitle()

    // The page sits a step under the rows it carries: containers behind, bright surfaces on top.
    StatusBarAppearance(lightBackground = !isSystemInDarkTheme())
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainer) {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    // iOS-style rubber band, before verticalScroll so this sits above it in the
                    // nested scroll chain.
                    .iosOverscroll(overscrollState)
                    .verticalScroll(
                        state = scrollState,
                        flingBehavior = rememberIosFlingBehavior(scrollState),
                    ),
            ) {
                CollapsingLargeTitle(title = title, collapsingTitle = collapsingTitle)
                Spacer(Modifier.height(TitleToContentGap))
                content()
                Spacer(Modifier.height(24.dp).navigationBarsPadding())
            }

            GlassTopAppBar(
                hazeState = hazeState,
                title = { Text(title) },
                onBack = onBack,
                collapsingTitle = collapsingTitle,
            )
        }
    }
}

@Composable
fun PreferenceSectionHeader(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(
            start = GroupMargin + RowPadding,
            end = GroupMargin,
            top = 20.dp,
            bottom = 8.dp,
        ),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

/**
 * The rows of one group, as a single rounded block: the block's own corners at its two ends and
 * small ones where the rows meet, separated by a hairline gap.
 */
@Composable
fun PreferenceGroup(count: Int, row: @Composable (index: Int, shape: Shape) -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = GroupMargin),
        verticalArrangement = Arrangement.spacedBy(RowGap),
    ) {
        repeat(count) { index ->
            row(
                index,
                UnevenRoundedRectangle(
                    topStart = if (index == 0) GroupCorner else RowCorner,
                    topEnd = if (index == 0) GroupCorner else RowCorner,
                    bottomEnd = if (index == count - 1) GroupCorner else RowCorner,
                    bottomStart = if (index == count - 1) GroupCorner else RowCorner,
                    style = RoundedCornerStyle.Continuous,
                ),
            )
        }
    }
}

@Composable
fun PreferenceRow(
    shape: Shape,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceBright)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(RowPadding),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** A row's text: what it is, and underneath, what it says. */
@Composable
fun PreferenceLabels(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
