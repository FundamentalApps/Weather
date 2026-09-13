package org.fundamentalos.weather.ui.componets

import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kyant.shapes.RoundedRectangle
import org.fundamentalos.weather.R
import org.fundamentalos.weather.ui.theme.PreviewTheme

val LocalUseDarkCards = compositionLocalOf { false }

private val CardCornerRadius = 24.dp

@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    title: String?=null,
    subtitle: String?=null,
    content: @Composable () -> Unit
) {
    val sticky = rememberStickyHeaderState()
    val clipTop = LocalScrollClipTop.current
    Column(
        modifier
            .stickyHeaderContainer(sticky, clipTop, cornerRadius = CardCornerRadius)
            .clip(RoundedRectangle(CardCornerRadius))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        if (!(title.isNullOrBlank() and subtitle.isNullOrBlank())) {
            Column(
                Modifier
                    .zIndex(1f)
                    .fillMaxWidth()
                    .stickyHeader(sticky, clipTop)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Spacer(Modifier.height(12.dp))
                title?.let {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .cardForegroundBlend()
                            .padding(16.dp, 0.dp)
                    )
                }
                subtitle?.let {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier
                            .cardForegroundBlend()
                            .padding(16.dp, 0.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
        }
        content()
    }
}

@Composable
fun InfoCard(
    icon: Painter,
    category: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    titleAlt: String? = null,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    val sticky = rememberStickyHeaderState()
    val clipTop = LocalScrollClipTop.current
    Column(
        modifier
            .stickyHeaderContainer(sticky, clipTop, cornerRadius = CardCornerRadius)
            .clip(RoundedRectangle(CardCornerRadius))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                // Column children draw in order, so the header has to be lifted above the body it pins over.
                .zIndex(1f)
                .fillMaxWidth()
                // Pins to the top of the clipped scroll area while the card body slides underneath.
                .stickyHeader(sticky, clipTop)
                .background(MaterialTheme.colorScheme.surface)
                .cardForegroundBlend()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                painter = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
            Text(
                text = category,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.padding(start = 6.dp)
            )
        }
        Row(
            Modifier
                .cardForegroundBlend()
                .padding(start = 16.dp)
        ) {
            title?.let {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(end = 8.dp).alignByBaseline()
                )
            }
            titleAlt?.let {
                Text(
                    text = titleAlt,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.alignByBaseline()
                )
            }
        }

        content()

        subtitle?.let {
            Text(
                modifier = Modifier
                    .cardForegroundBlend()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
fun Modifier.cardForegroundBlend(): Modifier {
    val multiplyOnSurface = !LocalUseDarkCards.current
    return if (multiplyOnSurface) {
        this.graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
            blendMode = BlendMode.Multiply
        }
    } else {
        this
    }
}

@Preview
@Composable
fun InfoCardPreview() {
    PreviewTheme {
        InfoCard(
            modifier = Modifier.fillMaxWidth(),
            icon = painterResource(R.drawable.ic_aqi_medium_20dp),
            category = stringResource(R.string.air_quality),
            title = "122",
            titleAlt = stringResource(R.string.aqi_sensitive),
            subtitle = "与昨天同时间类似。"
        ) {
            Box(Modifier.fillMaxWidth().height(128.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(0.12f)))
        }
    }
}
