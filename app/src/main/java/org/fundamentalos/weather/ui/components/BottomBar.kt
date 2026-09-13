package org.fundamentalos.weather.ui.components

import org.fundamentalos.weather.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.hazeEffect
import com.kyant.shapes.RoundedRectangle
import com.kyant.shapes.Capsule
import org.fundamentalos.weather.ui.LocalHazeState
import org.fundamentalos.weather.ui.theme.weatherHazeStyle
import org.fundamentalos.weather.ui.theme.PreviewThemeWithBg
import org.fundamentalos.weather.viewmodel.MainViewModel

data class LocationItem(
    val name: String,
    val id: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeApi::class)
@Composable
fun BottomBar(
    gpsStatus: MainViewModel.GpsStatus,
    locations: List<LocationItem>,
    selected: Int,
    onSelectionChange: (Int) -> Unit,
    onMapClick: () -> Unit,
    onLocationListClick: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Whether the place sits beside the map button rather than between the buttons: sideways,
     * where the middle of the bar is under the cards.
     */
    placeBesideMap: Boolean = false,
) {
    val hazeStyle = weatherHazeStyle()
    val overlayColor = hazeStyle.tints.firstOrNull()?.color ?: Color.Transparent
    val overlayBrush = Brush.verticalGradient(
        0f to overlayColor.copy(alpha = 0f),
        0.55f to overlayColor.copy(alpha = overlayColor.alpha * 0.35f),
        1f to overlayColor
    )

    Surface(modifier, color = Color.Transparent) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .hazeEffect(LocalHazeState.current, hazeStyle) {
                    inputScale = HazeInputScale.Fixed(0.5f)
                    progressive = HazeProgressive.verticalGradient(
                        startY = 0f,
                        startIntensity = 0f,
                        endY = Float.POSITIVE_INFINITY,
                        endIntensity = 1f,
                        preferPerformance = false
                    )
                }
                .drawWithContent {
                    drawRect(brush = overlayBrush)
                    drawContent()
                }
                .navigationBarsPadding()
                // The blur runs the full width, under a notch too; only the buttons keep clear of it.
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
                .height(80.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassButton(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .size(48.dp),
                shape = Capsule(),
                onClick = onMapClick
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Outlined.Map, contentDescription = stringResource(R.string.map))
                }
            }

            val place: @Composable () -> Unit = {
                GlassButton(
                    modifier = Modifier.height(48.dp),
                    shape = RoundedRectangle(16.dp),
                    onClick = { onSelectionChange(0) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(start = 18.dp, end = 22.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.NearMe,
                            stringResource(R.string.current_location),
                            Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            locations[0].name,
                            maxLines = 1,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
            if (placeBesideMap) {
                place()
                Spacer(Modifier.weight(1f))
            } else {
                Box(Modifier.weight(1f).fillMaxHeight(), Alignment.Center) { place() }
            }

            GlassButton(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .size(48.dp),
                shape = Capsule(),
                onClick = onLocationListClick
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.AutoMirrored.Default.List, contentDescription = stringResource(R.string.locations))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeApi::class)
@Composable
private fun GlassButton(
    modifier: Modifier = Modifier,
    shape: Shape,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val hazeStyle = weatherHazeStyle()
    val outlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)

    Surface(
        modifier = modifier
            .dropShadow(
                shape = shape,
                shadow = Shadow(
                    radius = 16.dp,
                    spread = 0.dp,
                    offset = DpOffset(x = 0.dp, y = 6.dp),
                    color = Color.Black,
                    alpha = 0.12f
                )
            )
            .clip(shape)
            .hazeEffect(LocalHazeState.current, hazeStyle) {
                inputScale = HazeInputScale.Fixed(0.5f)
                blurredEdgeTreatment = BlurredEdgeTreatment(shape)
            },
        shape = shape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.18f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, outlineColor),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        onClick = onClick,
        content = content
    )
}

@Preview
@Composable
private fun Preview() {
    PreviewThemeWithBg {
        val locations = remember { (1..10).map { LocationItem("地区$it", it.toString()) } }
        BottomBar(
            gpsStatus = MainViewModel.GpsStatus.Ok,
            locations = locations,
            selected = 0,
            onSelectionChange = { },
            onMapClick = { },
            onLocationListClick = { },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
