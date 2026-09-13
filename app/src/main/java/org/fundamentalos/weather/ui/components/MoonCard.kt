package org.fundamentalos.weather.ui.components

import org.fundamentalos.weather.ui.text.localizedDays
import org.fundamentalos.weather.ui.text.localizedFractionalDays
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import org.fundamentalos.weather.R
import org.fundamentalos.weather.ui.theme.PreviewThemeWithBg
import org.fundamentalos.weather.ui.theme.moon
import kotlinx.datetime.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt

/** How much of the photo the shadowed side keeps on a light card. */
private const val ShadowAlpha = 0.25f

private val DiscSize = 116.dp

/**
 * Moon readouts beside a disc drawn at the current phase.
 *
 * Moonrise is not shown: none of the free sources carry it and it is not worth an ephemeris here.
 */
@Composable
fun MoonCard(date: LocalDate, modifier: Modifier = Modifier) {
    val illumination = moonIlluminationFraction(date)
    val waxing = moonIsWaxing(date)

    InfoCard(
        modifier = modifier,
        icon = painterResource(R.drawable.weather_clear_night_24dp),
        category = stringResource(moonPhaseName(date)),
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                DetailRow(stringResource(R.string.illumination), "${(illumination * 100).roundToInt()}%")
                DetailRowDivider()
                DetailRow(stringResource(R.string.next_full_moon), localizedDays(daysToNextFullMoon(date).roundToInt()))
                DetailRowDivider()
                DetailRow(stringResource(R.string.moon_age), localizedFractionalDays(moonAgeDays(date)))
            }
            Spacer(Modifier.width(16.dp))
            MoonDisc(illumination.toFloat(), waxing, Modifier.size(DiscSize))
        }
    }
}

@Composable
private fun MoonDisc(illumination: Float, waxing: Boolean, modifier: Modifier = Modifier) {
    val texture = ImageBitmap.imageResource(R.drawable.moon_texture)
    val darkCards = LocalUseDarkCards.current
    // The unlit side is not black: earthshine and the surrounding card keep it faintly readable.
    // On a dark card that is a wash towards the card colour. On a light one any colour laid over
    // the photo fights it, so the shadow is the photo itself at low alpha instead — same picture,
    // faded into the card, with the lit part painted back at full strength.
    val shadowTint = if (darkCards) {
        ColorFilter.tint(
            MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
            BlendMode.SrcAtop,
        )
    } else {
        null
    }
    val litPath = remember { Path() }

    Canvas(modifier.fillMaxSize().cardForegroundBlend()) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val diameter = (radius * 2f).toInt()
        val topLeft = IntOffset((center.x - radius).roundToInt(), (center.y - radius).roundToInt())
        val target = IntSize(diameter, diameter)

        // The whole disc in shadow, then the lit crescent or gibbous painted back over it.
        drawImage(
            image = texture,
            dstOffset = topLeft,
            dstSize = target,
            alpha = if (darkCards) 1f else ShadowAlpha,
            colorFilter = shadowTint,
        )

        buildLitPath(litPath, center, radius, illumination.coerceIn(0f, 1f), waxing)
        clipPath(litPath) {
            drawImage(texture, dstOffset = topLeft, dstSize = target)
        }
    }
}

/**
 * Region of the disc that is lit: the limb on one side, and on the other the terminator, which is a
 * half ellipse whose width goes to zero at the quarters and which bulges toward the lit side while
 * the moon is a crescent and away from it once it is gibbous.
 */
private fun buildLitPath(
    path: Path,
    center: Offset,
    radius: Float,
    illumination: Float,
    waxing: Boolean,
) {
    val disc = Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius)
    val halfWidth = abs(radius * (1f - 2f * illumination))
    val terminator = Rect(center.x - halfWidth, center.y - radius, center.x + halfWidth, center.y + radius)
    val crescent = illumination < 0.5f

    path.reset()
    if (waxing) {
        path.moveTo(center.x, center.y - radius)
        path.arcTo(disc, -90f, 180f, false)
        if (halfWidth < 0.5f) {
            path.lineTo(center.x, center.y - radius)
        } else {
            path.arcTo(terminator, 90f, if (crescent) -180f else 180f, false)
        }
    } else {
        path.moveTo(center.x, center.y + radius)
        path.arcTo(disc, 90f, 180f, false)
        if (halfWidth < 0.5f) {
            path.lineTo(center.x, center.y + radius)
        } else {
            path.arcTo(terminator, 270f, if (crescent) -180f else 180f, false)
        }
    }
    path.close()
}

@Preview
@Composable
private fun Preview() {
    PreviewThemeWithBg {
        MoonCard(LocalDate(2026, 9, 10), Modifier.padding(16.dp))
    }
}
