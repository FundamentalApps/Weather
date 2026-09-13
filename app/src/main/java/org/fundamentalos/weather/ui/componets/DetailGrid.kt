package org.fundamentalos.weather.ui.componets

import org.fundamentalos.weather.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.kyant.shapes.RoundedRectangle
import org.fundamentalos.weather.ui.theme.PreviewThemeWithBg

private val TileCornerRadius = 20.dp

/**
 * Line height leaves slack above and below the glyphs, so a padding of N would read as noticeably
 * more than N. Trimming it makes the paddings below mean what they say.
 */
private val Trimmed = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both,
)

/** One detail readout: a dim label with the value under it, and optionally a graphic between. */
data class DetailInfo(
    val label: String,
    val value: String,
    val caption: String? = null,
    @DrawableRes val icon: Int? = null,
    /** Sits above the value; with a blank [value] it gets the whole tile and draws its own readout. */
    val graphic: (@Composable () -> Unit)? = null,
)

/**
 * Two-column grid of detail readouts, as many rows as it takes. Built from plain rows rather than a
 * lazy grid because it lives inside the page's own vertical scroll.
 */
@Composable
fun DetailGrid(items: List<DetailInfo>, modifier: Modifier = Modifier) {
    if (items.isEmpty()) return
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items.chunked(2).fastForEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                row.fastForEach { DetailTile(it, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DetailTile(info: DetailInfo, modifier: Modifier = Modifier) {
    val sticky = rememberStickyHeaderState()
    val clipTop = LocalScrollClipTop.current
    Box(
        modifier
            // Square, so the grid reads as a set of equal tiles rather than rows of varying height.
            .aspectRatio(1f)
            .stickyHeaderContainer(sticky, clipTop, cornerRadius = TileCornerRadius)
            .clip(RoundedRectangle(TileCornerRadius))
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(
                modifier = Modifier
                    // Same treatment as a card header: pins to the clip edge over its own body.
                    .zIndex(1f)
                    .fillMaxWidth()
                    .stickyHeader(sticky, clipTop)
                    .background(MaterialTheme.colorScheme.surface)
                    .cardForegroundBlend()
                    // 16dp between the tile's title and the value under it.
                    .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                info.icon?.let {
                    Icon(
                        painter = painterResource(it),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = info.label,
                    style = MaterialTheme.typography.titleSmall.copy(lineHeightStyle = Trimmed),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (info.value.isBlank()) {
                // A graphic that carries its own readout wants the whole tile, without the value row.
                info.graphic?.let {
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                    ) { it.invoke() }
                }
                return@Column
            }
            Column(
                Modifier
                    .cardForegroundBlend()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                info.graphic?.let { Box(Modifier.weight(1f).fillMaxWidth()) { it.invoke() } }
                Text(
                    text = info.value,
                    style = MaterialTheme.typography.headlineMedium.copy(lineHeightStyle = Trimmed),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                info.caption?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    PreviewThemeWithBg {
        DetailGrid(
            listOf(
                DetailInfo(stringResource(R.string.sunset), "18:32", "日出 05:50"),
                DetailInfo(stringResource(R.string.uv_index), "6", "强"),
                DetailInfo(stringResource(R.string.feels_like), "26°"),
                DetailInfo(stringResource(R.string.average_temperature), "22°"),
                DetailInfo(stringResource(R.string.precipitation), "0 毫米", stringResource(R.string.past_hour)),
                DetailInfo(stringResource(R.string.visibility), "15 公里"),
                DetailInfo(stringResource(R.string.humidity), "18%"),
                DetailInfo(stringResource(R.string.pressure), "1015 hPa"),
            ),
            Modifier.padding(16.dp),
        )
    }
}
