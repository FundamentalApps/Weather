package org.fundamentalos.weather.ui.screen

import org.fundamentalos.weather.ui.text.mapLayerName
import org.fundamentalos.weather.ui.text.localizedTime
import org.fundamentalos.weather.R
import androidx.compose.ui.res.stringResource
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import java.io.FileInputStream
import java.io.InputStream
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.kyant.shapes.RoundedRectangle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import org.fundamentalos.weather.BuildConfig
import org.fundamentalos.weather.ui.componets.BlurOnlyHazeStyle
import org.fundamentalos.weather.ui.componets.GlassTopAppBar
import org.fundamentalos.weather.ui.componets.StatusBarAppearance
import org.fundamentalos.weather.ui.componets.TemperatureField
import org.fundamentalos.weather.ui.componets.ContinuityTileProvider
import org.fundamentalos.weather.ui.componets.bottomEdgeBlur
import org.fundamentalos.weather.ui.theme.PreviewThemeWithBg
import org.fundamentalos.weather.viewmodel.MainViewModel
import org.fundamentalos.weather.weather.provider.fos.FosApiClient
import org.fundamentalos.weather.weather.provider.fos.FosMapLayer
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.awaitCancellation
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.TilesOverlay

/** Start with a regional view of the temperature field. */
private const val DefaultZoom = 6.0

/** Half the width of the web mercator world, in metres; a WMS layer asks for tiles in these. */
private const val WorldEdge = 20037508.342789244

/** Every layer here covers the world, so its own tiles start at the top. */
private const val MinZoom = 1

private val GoogleBlue = Color(0xFF4285F4)

/** Desaturated and dimmed, with a slight lift so the darkest ink does not go pure black. */
private val DarkMapFilter = ColorMatrixColorFilter(
    ColorMatrix().apply {
        setSaturation(0.35f)
        postConcat(
            ColorMatrix(
                floatArrayOf(
                    0.50f, 0f, 0f, 0f, 12f,
                    0f, 0.50f, 0f, 0f, 12f,
                    0f, 0f, 0.55f, 0f, 16f,
                    0f, 0f, 0f, 1f, 0f,
                )
            )
        )
    }
)

/**
 * The weather map: OpenStreetMap underneath, the server's overlay layers on top, centred on
 * whichever place the home screen is showing.
 *
 * The tiles are fetched from their sources directly — OSM's policy forbids re-serving them and a
 * proxy would put every pan through our box — but which layers exist, where their tiles are and
 * what their colours mean all come from the server, so a source can change without a release.
 */
@Composable
fun MapScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    vm: MainViewModel = koinViewModel(),
    api: FosApiClient = koinInject(),
) {
    val context = LocalContext.current
    val darkMap = isSystemInDarkTheme()
    val hazeState = remember { HazeState() }
    var layers by remember { mutableStateOf<List<FosMapLayer>>(emptyList()) }

    LaunchedEffect(Unit) {
        layers = runCatching { api.mapLayers().layers }.getOrDefault(emptyList())
    }

    // Temperature is always on. Radar and user layer switches are no longer part of this map.
    val visible = layers.filter { it.id == "temperature" && it.legend.isNotEmpty() }

    val center = vm.currentLocation.value?.let { GeoPoint(it.latitude, it.longitude) }
    val dotColor = GoogleBlue.toArgb()
    val dotRadiusPx = with(LocalDensity.current) { 6.dp.toPx() }

    var mapView by remember { mutableStateOf<MapView?>(null) }
    LaunchedEffect(context) {
        var provider: ContinuityTileProvider? = null
        var ownedMap: MapView? = null
        try {
            // Opening preferences, SQLite and tile archives must not block the push frame.
            withContext(Dispatchers.IO) {
                Configuration.getInstance().apply {
                    load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
                    userAgentValue = BuildConfig.APPLICATION_ID
                    osmdroidBasePath = context.cacheDir
                    osmdroidTileCache = context.cacheDir.resolve("osmdroid")
                }
                provider = ContinuityTileProvider(context.applicationContext, TileSourceFactory.MAPNIK)
            }
            ownedMap = MapView(context, provider, null, null).apply {
                setMultiTouchControls(true)
                zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                setUseDataConnection(true)
                controller.setZoom(DefaultZoom)
                onResume()
            }
            mapView = ownedMap
            awaitCancellation()
        } finally {
            ownedMap?.onPause()
            if (ownedMap != null) ownedMap.onDetach() else provider?.detach()
            mapView = null
        }
    }

    // The raster tiles are drawn for a light UI. Inverting them is the usual trick but it turns
    // water brown and woodland magenta; draining the colour and dimming it keeps the map readable
    // and lets the overlays be the only saturated thing on screen.
    LaunchedEffect(mapView, darkMap) {
        val map = mapView ?: return@LaunchedEffect
        map.overlayManager.tilesOverlay.apply {
            setColorFilter(if (darkMap) DarkMapFilter else null)
            // A missing base tile must never expose the almost-black Compose surface.
            // This neutral map paper also passes through the same day/night filter.
            loadingBackgroundColor = AndroidColor.rgb(224, 229, 232)
            loadingLineColor = AndroidColor.TRANSPARENT
        }
    }

    LaunchedEffect(mapView, center) {
        val map = mapView ?: return@LaunchedEffect
        if (center != null) {
            map.controller.setCenter(center)
            map.overlays.removeAll { it is HerePin }
            map.overlays.add(HerePin(center, dotColor, AndroidColor.WHITE, dotRadiusPx))
            map.invalidate()
        }
    }

    DisposableEffect(mapView, visible) {
        val map = mapView ?: return@DisposableEffect onDispose { }
        val overlays = visible.map { layer ->
            layer.toOverlay(context, map, TemperatureField(layer.legend,
                if (layer.scheme == "wms3857") TemperatureField.Radius else 0))
        }
        overlays.forEachIndexed { index, overlay -> map.overlays.add(index, overlay) }
        map.invalidate()
        onDispose {
            overlays.forEach { overlay ->
                map.overlays.remove(overlay)
                overlay.onDetach(map)
            }
        }
    }

    StatusBarAppearance(lightBackground = !darkMap)

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(if (darkMap) Color(0xFF7C7F8C) else Color(0xFFE0E5E8))) {
                mapView?.let { map ->
                    AndroidView(factory = { map }, modifier = Modifier.fillMaxSize().hazeSource(hazeState))
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 72.dp, end = 16.dp),
                horizontalAlignment = Alignment.End,
            ) {
                visible.firstOrNull { it.legend.isNotEmpty() }?.let { layer ->
                    Spacer(Modifier.height(12.dp))
                    LegendCard(layer = layer, hazeState = hazeState)
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .bottomEdgeBlur(hazeState, BlurOnlyHazeStyle())
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp),
            ) {
                // Every layer that knows when it was made says so, oldest first.
                val stamps = visible.mapNotNull { layer ->
                    val at = layer.observedAt ?: return@mapNotNull null
                    "${mapLayerName(layer.id, layer.name)} ${localizedTime(Instant.fromEpochSeconds(at))}"
                }
                Text(
                    text = when {
                        stamps.isNotEmpty() -> stamps.joinToString(" · ")
                        layers.isEmpty() -> stringResource(R.string.layers_loading)
                        visible.isEmpty() -> stringResource(R.string.layers_disabled)
                        else -> visible.map { mapLayerName(it.id, it.name) }.joinToString(" · ")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = (listOf(stringResource(R.string.map_attribution)) + visible.map { it.attribution })
                        .joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }

            GlassTopAppBar(
                hazeState = hazeState,
                title = { Text(stringResource(R.string.weather_map)) },
                onBack = onBackClick,
            )
        }
    }
}

/** What the layer's colours mean, as the scale the server sent with it. */
@Composable
private fun LegendCard(layer: FosMapLayer, hazeState: HazeState, modifier: Modifier = Modifier) {
    val stops = layer.legend
    val colors = stops.map { Color(it.color.removePrefix("#").toLong(16) or 0xFF000000L) }
    Column(
        modifier = modifier
            // Sized here rather than by its content: the divider inside would otherwise stretch
            // the card across the screen.
            .width(112.dp)
            .clip(RoundedRectangle(20.dp))
            .hazeEffect(hazeState, BlurOnlyHazeStyle())
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = listOfNotNull(mapLayerName(layer.id, layer.name), layer.unit?.let { "($it)" }).joinToString(" "),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 10.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
        )
        Row(modifier = Modifier.height(148.dp)) {
            Box(
                Modifier
                    .width(8.dp)
                    .fillMaxHeight()
                    .clip(RoundedRectangle(4.dp))
                    // The scale runs cold to hot; a legend reads the other way round.
                    .background(Brush.verticalGradient(colors.reversed())),
            )
            Column(
                modifier = Modifier.padding(start = 10.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // The palette steps every five degrees and its two ends are open-ended, so the
                // labels are the round numbers nearest the bands rather than the band values.
                listOf(40, 20, 0, -20, -40).forEach { mark ->
                    Text(
                        text = mark.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}

/** A layer as osmdroid sees it: a tile source, its opacity, and nothing to draw while it loads. */
private fun FosMapLayer.toOverlay(context: Context, map: MapView, field: TemperatureField?): TilesOverlay {
    val layer = this
    val source = object : OnlineTileSourceBase(
        // The name is the cache key: processed tiles must not land in the same drawer as the raw
        // ones they were made from.
        if (field == null) layer.id else "${layer.id}-temperature-continuous-v3-${layer.observedAt ?: 0}",
        MinZoom, layer.maxZoom, layer.tileSize, ".png", arrayOf(layer.urlTemplate)
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val zoom = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return if (layer.scheme == "wms3857") {
                layer.urlTemplate.replace("{bbox}", mercatorBounds(zoom, x, y, TemperatureField.Radius, layer.tileSize))
                    .replace(Regex("(?i)([?&](?:width|height)=)\\d+")) {
                        it.groupValues[1] + (layer.tileSize + TemperatureField.Radius * 2)
                    }
            } else {
                layer.urlTemplate
                    .replace("{z}", zoom.toString())
                    .replace("{x}", x.toString())
                    .replace("{y}", y.toString())
            }
        }

        override fun getDrawable(aTileInputStream: InputStream): Drawable? {
            if (field == null) return super.getDrawable(aTileInputStream)
            val bitmap = field.decode(aTileInputStream) ?: return null
            return BitmapDrawable(context.resources, bitmap)
        }

        /** The cache hands back the raw file it saved, so that path needs the same treatment. */
        override fun getDrawable(aFilePath: String): Drawable? {
            if (field == null) return super.getDrawable(aFilePath)
            val bitmap = runCatching { FileInputStream(aFilePath).use(field::decode) }.getOrNull()
            return bitmap?.let { BitmapDrawable(context.resources, it) }
        }
    }
    // Keep cached parent tiles available during zoom changes while detailed tiles load.
    val provider = ContinuityTileProvider(context, source).apply {
        tileRequestCompleteHandlers.add(map.tileRequestCompleteHandler)
    }
    return TilesOverlay(provider, context).apply {
        loadingBackgroundColor = AndroidColor.TRANSPARENT
        loadingLineColor = AndroidColor.TRANSPARENT
        // TilesOverlay has no alpha of its own; scaling the matrix's alpha row is how a layer is
        // kept translucent enough to read the map through it.
        setColorFilter(
            ColorMatrixColorFilter(
                ColorMatrix(
                    floatArrayOf(
                        1f, 0f, 0f, 0f, 0f,
                        0f, 1f, 0f, 0f, 0f,
                        0f, 0f, 1f, 0f, 0f,
                        0f, 0f, 0f, layer.opacity, 0f,
                    )
                )
            )
        )
    }
}

/** The tile's own square in web mercator metres, which is what a WMS asks for. */
private fun mercatorBounds(zoom: Int, x: Int, y: Int, gutter: Int = 0, tileSize: Int = 256): String {
    val span = 2 * WorldEdge / (1 shl zoom)
    val minX = -WorldEdge + x * span
    val maxY = WorldEdge - y * span
    val pad = span * gutter / tileSize
    return "${minX - pad},${maxY - span - pad},${minX + span + pad},${maxY + pad}"
}

/** Where the home screen's location is, drawn as a dot rather than a pin. */
private class HerePin(
    private val point: GeoPoint,
    dotColor: Int,
    haloColor: Int,
    private val radius: Float,
) : Overlay() {
    private val fill = Paint().apply {
        isAntiAlias = true
        color = dotColor
    }
    private val halo = Paint().apply {
        isAntiAlias = true
        color = haloColor
    }

    override fun draw(canvas: Canvas, projection: Projection) {
        val screen = projection.toPixels(point, null)
        canvas.drawCircle(screen.x.toFloat(), screen.y.toFloat(), radius * 1.5f, halo)
        canvas.drawCircle(screen.x.toFloat(), screen.y.toFloat(), radius, fill)
    }
}

@Preview
@Composable
private fun Preview() {
    PreviewThemeWithBg {
        MapScreen(onBackClick = {})
    }
}
