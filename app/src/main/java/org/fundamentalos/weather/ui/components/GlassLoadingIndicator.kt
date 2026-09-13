package org.fundamentalos.weather.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material's expressive circular wavy progress indicator while the weather is on its way, set in
 * the sky's own glass rather than a colour: the ring and its wave are a mask over the
 * condition-text material, so they read as the same substance as the headline they make way
 * for, the track a fainter cut of it.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GlassLoadingIndicator(modifier: Modifier = Modifier, size: Dp = 56.dp) {
    // The material replaces the fill, so the indicator only acts as a mask and is drawn opaque;
    // the track's alpha becomes how much of the material shows through it.
    val glass = LocalGlassBackdrop.current != null
    val ink = if (glass) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
    CircularWavyProgressIndicator(
        modifier = modifier.size(size).conditionTextGlassMaterial(),
        color = ink,
        trackColor = ink.copy(alpha = ink.alpha * TrackAlpha),
    )
}

/** How much of the ring the track shows against the wave. */
private const val TrackAlpha = 0.3f
