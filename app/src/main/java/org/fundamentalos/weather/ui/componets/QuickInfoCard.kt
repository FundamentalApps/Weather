package org.fundamentalos.weather.ui.componets

import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kyant.shapes.RoundedRectangle
import org.fundamentalos.weather.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickInfoCard(
    feelsLike: String,
    maxTemp: String,
    minTemp: String,
    modifier: Modifier = Modifier,
    windDirection: String = "--",
    windScale: String = "",
) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            // Behaves like a pinned header: holds at the clip edge and fades under the card riding up.
            .holdWhenClipped(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
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
