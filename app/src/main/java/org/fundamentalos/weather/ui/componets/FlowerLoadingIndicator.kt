package org.fundamentalos.weather.ui.componets

import android.graphics.Path
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath

/**
 * A flower that turns and breathes from one bloom to the next while the weather is on its way:
 * Material's expressive loading indicator, set in the sky's own glass — the shape is a mask over
 * the condition-text material, so it reads as the same substance as the headline it makes way for.
 */
@Composable
fun FlowerLoadingIndicator(modifier: Modifier = Modifier, size: Dp = 56.dp) {
    // The material replaces the fill, so the shape only acts as a mask and must be drawn opaque.
    val glass = LocalGlassBackdrop.current != null
    val color = if (glass) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
    val transition = rememberInfiniteTransition(label = "flower")
    val cycle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(CycleMillis, easing = LinearEasing)),
        label = "cycle",
    )
    val morphs = remember { Blooms.indices.map { Morph(Blooms[it], Blooms[(it + 1) % Blooms.size]) } }
    val path = remember { Path() }
    val composePath = remember(path) { path.asComposePath() }

    Canvas(modifier.size(size).conditionTextGlassMaterial()) {
        val t = cycle
        val place = t * morphs.size
        val index = place.toInt().coerceIn(0, morphs.size - 1)
        val progress = FastOutSlowInEasing.transform(place - index)
        morphs[index].toPath(progress, path)
        // The shapes are normalised into the unit square; one turn per cycle, so the wrap is seamless.
        withTransform({
            rotate(t * 360f)
            scale(this@Canvas.size.minDimension, this@Canvas.size.minDimension, pivot = Offset.Zero)
        }) {
            drawPath(composePath, color)
        }
    }
}

/** One turn of the flower, through every bloom and back. */
private const val CycleMillis = 3200

/**
 * The blooms the indicator breathes through, each fitted to the unit square: always a flower,
 * with the petals opening and closing and their count changing from one to the next.
 */
private val Blooms: List<RoundedPolygon> = listOf(
    flower(petals = 8, depth = 0.58f),
    flower(petals = 6, depth = 0.52f),
    flower(petals = 8, depth = 0.7f),
    flower(petals = 5, depth = 0.55f),
)

/** A flower of round [petals], cut to [depth] of the radius between them. */
private fun flower(petals: Int, depth: Float): RoundedPolygon = RoundedPolygon.star(
    numVerticesPerRadius = petals,
    innerRadius = depth,
    rounding = CornerRounding(radius = 0.42f, smoothing = 0.8f),
    innerRounding = CornerRounding(radius = 0.16f, smoothing = 0.5f),
).normalized()
