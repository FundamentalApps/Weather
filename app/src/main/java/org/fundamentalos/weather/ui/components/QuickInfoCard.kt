package org.fundamentalos.weather.ui.components

import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kyant.shapes.RoundedRectangle
import org.fundamentalos.weather.R

@Composable
fun QuickInfoCard(
    feelsLike: String,
    maxTemp: String,
    minTemp: String,
    modifier: Modifier = Modifier,
    windDirection: String = "--",
    windScale: String = "",
) {
    ChipGrid(
        modifier = modifier
            .fillMaxWidth()
            // Behaves like a pinned header: holds at the clip edge and fades under the card riding up.
            .holdWhenClipped(),
        gap = 16.dp,
    ) {
        QuickInfoCardItem(
            titleIcon = painterResource(R.drawable.ic_arrow_upward_20dp),
            titleIconDescription = stringResource(R.string.maximum_temperature),
            title = "$maxTemp°",
            contentIcon = painterResource(R.drawable.ic_arrow_downward_20dp),
            contentIconDescription = stringResource(R.string.minimum_temperature),
            content = "$minTemp°"
        )
        QuickInfoCardItem(title = stringResource(R.string.feels_like_short), content = "$feelsLike°")
        QuickInfoCardItem(title = windDirection, content = beaufortText(windScale))
    }
}

/**
 * Chips in one row at their own widths while they fit. When they have to wrap, a grid instead:
 * every chip as wide as the widest, so a chip on a lower row lines up with the one above it,
 * with its content centred. Widths are read as intrinsics first, so each chip is measured once.
 */
@Composable
private fun ChipGrid(modifier: Modifier, gap: Dp, content: @Composable () -> Unit) {
    Layout(content, modifier) { measurables, constraints ->
        val gapPx = gap.roundToPx()
        val width = constraints.maxWidth
        val natural = measurables.map { it.maxIntrinsicWidth(Constraints.Infinity) }
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        if (natural.sum() + gapPx * (natural.size - 1).coerceAtLeast(0) <= width) {
            val placeables = measurables.map { it.measure(loose) }
            val height = placeables.maxOfOrNull { it.height } ?: 0
            return@Layout layout(width, height) {
                var x = 0
                for (p in placeables) { p.placeRelative(x, (height - p.height) / 2); x += p.width + gapPx }
            }
        }
        val cell = (natural.maxOrNull() ?: 0).coerceAtMost(width)
        val columns = ((width + gapPx) / (cell + gapPx)).coerceAtLeast(1)
        val placeables = measurables.map { it.measure(loose.copy(minWidth = cell, maxWidth = cell)) }
        val rowHeight = placeables.maxOfOrNull { it.height } ?: 0
        val rows = (placeables.size + columns - 1) / columns
        val height = rows * rowHeight + (rows - 1).coerceAtLeast(0) * gapPx
        layout(width, height) {
            placeables.forEachIndexed { i, p ->
                p.placeRelative((i % columns) * (cell + gapPx), (i / columns) * (rowHeight + gapPx) + (rowHeight - p.height) / 2)
            }
        }
    }
}

/** Locale-aware Beaufort scale label. */
@Composable
private fun beaufortText(scale: String): String {
    val n = scale.trim().toIntOrNull() ?: return "--"
    return stringResource(R.string.beaufort, n)
}

@Composable
fun QuickInfoCardItem(
    modifier: Modifier = Modifier,
    titleIcon: Painter?= null,
    titleIconDescription: String?= null,
    title: String?= null,
    contentIcon: Painter?= null,
    contentIconDescription: String?= null,
    content: String?= null
) {
    Box(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedRectangle(16.dp)
            )
            .padding(16.dp, 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.cardForegroundBlend(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            titleIcon?.let {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = it,
                    contentDescription = titleIconDescription,
                    tint = MaterialTheme.colorScheme.onSurface
                )
                title?.let {
                    Spacer(Modifier.width(4.dp))
                }
            }

            title?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            contentIcon?.let {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = it,
                    contentDescription = contentIconDescription,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                content?.let {
                    Spacer(Modifier.width(4.dp))
                }
            }

            content?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Preview
@Composable
fun QuickInfoCardPreview() {
    QuickInfoCardItem(
        titleIcon = painterResource(R.drawable.ic_arrow_upward_24dp),
        titleIconDescription = stringResource(R.string.maximum_temperature),
        title = "26°",
        contentIcon = painterResource(R.drawable.ic_arrow_upward_24dp),
        contentIconDescription = stringResource(R.string.minimum_temperature),
        content = "15°"
    )
}
