package org.fundamentalos.weather.ui.screen

import org.fundamentalos.weather.ui.text.mapLayerName
import org.fundamentalos.weather.ui.text.localizedTime
import org.fundamentalos.weather.R
import androidx.compose.ui.res.stringResource
import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.kyant.shapes.RoundedRectangle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import org.fundamentalos.weather.BuildConfig
import org.fundamentalos.weather.settings.AppSettings
import org.fundamentalos.weather.ui.components.AppleWeatherConditionTextMaterial
import org.fundamentalos.weather.ui.components.AppleWeatherConditionTextMaterialLight
import org.fundamentalos.weather.ui.components.blurOnlyHazeStyle
import org.fundamentalos.weather.ui.components.GlassTopAppBar
import org.fundamentalos.weather.ui.components.LocalGlassBackdrop
import org.fundamentalos.weather.ui.components.StatusBarAppearance
import org.fundamentalos.weather.ui.components.TemperatureField
import org.fundamentalos.weather.ui.components.ContinuityTileProvider
import org.fundamentalos.weather.ui.components.conditionTextGlassMaterial
import org.fundamentalos.weather.ui.components.glassBackdropSource
import org.fundamentalos.weather.ui.components.rememberGlassBackdropState
import org.fundamentalos.weather.ui.map.FieldLayerSource
import org.fundamentalos.weather.ui.map.InkTileStore
import org.fundamentalos.weather.ui.map.LocationBubbleOverlay
import org.fundamentalos.weather.ui.map.MapInkOverlay
import org.fundamentalos.weather.ui.map.WeatherFieldOverlay
import org.fundamentalos.weather.ui.map.WeatherFieldStore
import org.fundamentalos.weather.ui.theme.PreviewThemeWithBg
import org.fundamentalos.weather.ui.theme.temperature0
import org.fundamentalos.weather.ui.theme.temperature10
import org.fundamentalos.weather.ui.theme.temperature20
import org.fundamentalos.weather.ui.theme.temperature30
import org.fundamentalos.weather.ui.theme.temperature40
import org.fundamentalos.weather.ui.theme.temperature50
import org.fundamentalos.weather.ui.theme.temperatureMinor10
import org.fundamentalos.weather.ui.theme.temperatureMinor20
import org.fundamentalos.weather.ui.theme.temperatureMinor40
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
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.util.TileSystem
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.TilesOverlay
import kotlin.math.log2
import kotlin.math.max

/**
 * Open on the city and its neighbours, the way Apple Weather does: about 40 km across a phone.
 * Zoom levels here count density-scaled tiles, so this means the same extent on every screen.
 */
private const val DefaultZoom = 10.0

/** Close enough to place a district; the field has nothing finer to show past this. */
private const val MaxZoom = 13.0

/** Every xyz layer here covers the world, so its own tiles start at the top. */
private const val MinTileZoom = 1

/** The map paper: what shows where the field has nothing, and behind its translucency. */
private val LightPaper = Color(0xFFF3F0EA)
private val DarkPaper = Color(0xFF2B2E36)

/** The marker's ring and range colours if no temperature legend has arrived yet. */
private const val FallbackMarkerColor = 0xFFF28C38.toInt()

/**
 * The forecast's temperature colours, by degree: the same stops the ten-day card's range bar is
 * painted with, so the map and the card agree on what warm looks like.
 */
private val TemperaturePalette = listOf(
    -40f to temperatureMinor40,
    -20f to temperatureMinor20,
    -10f to temperatureMinor10,
    0f to temperature0,
    10f to temperature10,
    20f to temperature20,
    30f to temperature30,
    40f to temperature40,
    50f to temperature50,
)

/**
 * The weather map: the field first, then the map's own ink — water, roads, borders and place
 * names — drawn over it from OpenStreetMap's vector tiles, centred on whichever place the home
 * screen is showing and marked there with the temperature now.
 *
 * The tiles are fetched from their sources directly — OSM's policy forbids re-serving them and a
 * proxy would put every pan through our box — but which layers exist, where their tiles are and
 * what their colours mean all come from the server, so a source can change without a release.
 *
 * A field layer such as temperature is not tiled at all: it is fetched in whole square chunks of
 * the world, a few screens wide at any zoom, that are kept across visits, so a pan or a zoom
 * redraws the same bitmap instead of loading more.
 */
@Composable
fun MapScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    vm: MainViewModel = koinViewModel(),
    api: FosApiClient = koinInject(),
    fieldStore: WeatherFieldStore = koinInject(),
    inkStore: InkTileStore = koinInject(),
    settings: AppSettings = koinInject(),
) {
    val context = LocalContext.current
    val darkMap = isSystemInDarkTheme()
    val hazeState = remember { HazeState() }
    // Start from the list the last visit ended with, so the overlays are there before the
    // server answers; a changed answer swaps them in place.
    var layers by remember { mutableStateOf(fieldStore.layers ?: emptyList()) }

    LaunchedEffect(Unit) {
        val fresh = runCatching { api.mapLayers().layers }.getOrNull() ?: return@LaunchedEffect
        fieldStore.layers = fresh
        layers = fresh
    }

    // Temperature is always on. Radar and user layer switches are no longer part of this map.
    val visible = layers.filter { it.id == "temperature" && it.legend.isNotEmpty() }

    val center = vm.currentLocation.value?.let { GeoPoint(it.latitude, it.longitude) }

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
                // No raster tiles: the map is drawn from vector tiles by an overlay, so its
                // names can sit above the field. Off, the base overlay asks for nothing.
                overlayManager.tilesOverlay.isEnabled = false
                // Tiles are drawn at density scale so labels are legible and zoom levels mean
                // the same extent on every screen.
                setTilesScaledToDpi(true)
                // One world only: no second copy past the date line or beyond the poles, and no
                // scrolling into the void around it.
                isHorizontalMapRepetitionEnabled = false
                isVerticalMapRepetitionEnabled = false
                val tiles = MapView.getTileSystem()
                setScrollableAreaLimitDouble(
                    BoundingBox(tiles.maxLatitude, tiles.maxLongitude, tiles.minLatitude, tiles.minLongitude)
                )
                setMaxZoomLevel(MaxZoom)
                // The furthest out is where the world just fills the longer side of the view;
                // that needs the view's size, which arrives with the first layout.
                addOnFirstLayoutListener { view, _, _, _, _ ->
                    val fill = max(view.width, view.height).toDouble() / TileSystem.getTileSize()
                    val floor = log2(fill).coerceAtLeast(0.0)
                    setMinZoomLevel(floor)
                    if (zoomLevelDouble < floor) controller.setZoom(floor)
                }
                controller.setZoom(DefaultZoom)
                // Centre before the first draw: an overlay that draws at (0°, 0°) first would
                // ask for the Gulf of Guinea before the place the user came here to see.
                vm.currentLocation.value?.let { controller.setCenter(GeoPoint(it.latitude, it.longitude)) }
                onResume()
            }
            mapView = ownedMap
            awaitCancellation()
        } finally {
            ownedMap?.onPause()
            if (ownedMap != null) ownedMap.onDetach() else provider?.detach()
            mapView = null
            // The stores outlive the screen so the next visit is instant, but not at full size.
            fieldStore.trim()
            inkStore.trim()
        }
    }

    LaunchedEffect(mapView, center) {
        val map = mapView ?: return@LaunchedEffect
        if (center != null) map.controller.setCenter(center)
    }

    // The overlays wait for a centre: a field overlay asks for the chunks under the view the
    // first time it is drawn, and until the location is known that view is (0°, 0°).
    var marker by remember { mutableStateOf<LocationBubbleOverlay?>(null) }
    val locale = LocalConfiguration.current.locales[0]
    // The field is coloured with the forecast's own temperature palette, unharmonised: a field
    // is read against the legend, so its colours must be the same on every device.
    val palette = TemperaturePalette
    val paletteArgb = palette.map { (degrees, color) -> degrees to color.toArgb() }
    DisposableEffect(mapView, visible, center == null, darkMap, paletteArgb) {
        val map = mapView ?: return@DisposableEffect onDispose { }
        if (center == null) return@DisposableEffect onDispose { }
        map.controller.setCenter(center)
        val paletteKey = "-p" + paletteArgb.hashCode().toUInt().toString(16)
        var legend: TemperatureField? = null
        val overlays = visible.map { layer ->
            if (layer.scheme == "wms3857" && layer.legend.isNotEmpty()) {
                val field = TemperatureField(layer.legend, paletteArgb)
                if (legend == null) legend = field
                WeatherFieldOverlay(map, FieldLayerSource(layer, field, paletteKey), fieldStore)
            } else {
                layer.toOverlay(context, map)
            }
        }.toMutableList()
        // The map's own ink goes above the field, and the place marker above everything; the
        // ink keeps its names out from under the marker.
        val colorFor: (Float) -> Int = legend?.let { field -> { value -> field.colorFor(value) } }
            ?: { FallbackMarkerColor }
        val bubble = LocationBubbleOverlay(map, colorFor)
        overlays += MapInkOverlay(map, inkStore, darkMap, locale).apply { reserved = listOf(bubble.footprint) }
        overlays += bubble
        overlays.forEachIndexed { index, overlay -> map.overlays.add(index, overlay) }
        marker = bubble
        map.invalidate()
        onDispose {
            marker = null
            overlays.forEach { overlay ->
                map.overlays.remove(overlay)
                overlay.onDetach(map)
            }
        }
    }

    val weather = vm.weather.value
    val today = vm.dailyForecast.value.firstOrNull()
    val placeName = vm.currentLocation.value?.name ?: ""
    val caption = stringResource(R.string.current_location)
    LaunchedEffect(marker, center, weather, today, placeName, caption) {
        val bubble = marker ?: return@LaunchedEffect
        bubble.point = center
        bubble.tempCelsius = weather?.tempCelsius
        bubble.minCelsius = today?.tempMinCelsius
        bubble.maxCelsius = today?.tempMaxCelsius
        bubble.caption = caption
        bubble.placeName = placeName
        mapView?.invalidate()
    }

    StatusBarAppearance(lightBackground = !darkMap)

    // The bar title and the credits are glass text over the map, as the home screen's text is
    // over the sky: the glyphs take the blurred map behind them, lifted in the dark theme and
    // cut down into the bright day map. The shaders need API 33; below that they are plain text.
    val glassBackdrop = if (settings.glassText && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberGlassBackdropState(
            conditionTextMaterial = if (darkMap) AppleWeatherConditionTextMaterial else AppleWeatherConditionTextMaterialLight,
        )
    } else {
        null
    }
    val glassText = glassBackdrop != null

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        CompositionLocalProvider(LocalGlassBackdrop provides glassBackdrop) {
        Box(Modifier.fillMaxSize()) {
            // The blurs and the glass text sample this box, paper included: the map view alone is
            // translucent where the field is, and a blur of translucent content over the paper
            // reads as a lighter band.
            Box(
                Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState)
                    .then(if (glassBackdrop != null) Modifier.glassBackdropSource(glassBackdrop) else Modifier)
                    .background(if (darkMap) DarkPaper else LightPaper)
            ) {
                mapView?.let { map ->
                    AndroidView(factory = { map }, modifier = Modifier.fillMaxSize())
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
                    LegendCard(layer = layer, palette = palette, hazeState = hazeState)
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp),
            ) {
                // Every layer that knows when it was made says so, oldest first.
                val stamps = visible.mapNotNull { layer ->
                    val at = layer.observedAt ?: return@mapNotNull null
                    "${mapLayerName(layer.id, layer.name)} ${localizedTime(Instant.fromEpochSeconds(at))}"
                }
                // With the material the glyphs are a mask, so they are drawn opaque.
                Text(
                    text = when {
                        stamps.isNotEmpty() -> stamps.joinToString(" · ")
                        layers.isEmpty() -> stringResource(R.string.layers_loading)
                        visible.isEmpty() -> stringResource(R.string.layers_disabled)
                        else -> visible.map { mapLayerName(it.id, it.name) }.joinToString(" · ")
                    },
                    modifier = Modifier.conditionTextGlassMaterial(),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (glassText) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = (listOf(stringResource(R.string.map_attribution)) + visible.map { it.attribution })
                        .joinToString(" · "),
                    modifier = Modifier.conditionTextGlassMaterial(),
                    style = MaterialTheme.typography.labelSmall,
                    color = (if (glassText) Color.White else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.7f),
                )
            }

            GlassTopAppBar(
                hazeState = hazeState,
                title = {
                    Text(
                        text = stringResource(R.string.weather_map),
                        modifier = Modifier.conditionTextGlassMaterial(),
                        color = if (glassText) Color.White else Color.Unspecified,
                    )
                },
                onBack = onBackClick,
                blur = false,
            )
        }
        }
    }
}

/** What the field's colours mean: the forecast palette, cold at the bottom. */
@Composable
private fun LegendCard(
    layer: FosMapLayer,
    palette: List<Pair<Float, Color>>,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    val low = palette.first().first
    val high = palette.last().first
    // Fraction of the way from the bottom (cold) to the top (hot) a temperature sits.
    fun fraction(degrees: Float) = (degrees - low) / (high - low)
    Column(
        modifier = modifier
            // Sized here rather than by its content: the divider inside would otherwise stretch
            // the card across the screen.
            .width(112.dp)
            .clip(RoundedRectangle(20.dp))
            .hazeEffect(hazeState, blurOnlyHazeStyle())
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
                    // The gradient's stops sit where their degrees do, top being hot; a gradient
                    // wants its stops from the top down, so the palette is read hot to cold.
                    .background(
                        Brush.verticalGradient(
                            *palette.asReversed().map { (degrees, color) -> (1f - fraction(degrees)) to color }.toTypedArray()
                        )
                    ),
            )
            Box(modifier = Modifier.padding(start = 10.dp).fillMaxHeight()) {
                // Each mark beside the degree it names; the bottom one sits on the bar's end.
                listOf(40f, 20f, 0f, -20f, -40f).forEach { mark ->
                    Text(
                        text = mark.toInt().toString(),
                        modifier = Modifier.align(BiasAlignment(-1f, 1f - 2f * fraction(mark))),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}

/** An xyz layer as osmdroid sees it: a tile source, its opacity, and nothing to draw while it loads. */
private fun FosMapLayer.toOverlay(context: Context, map: MapView): TilesOverlay {
    val layer = this
    val source = object : OnlineTileSourceBase(
        layer.id, MinTileZoom, layer.maxZoom, layer.tileSize, ".png", arrayOf(layer.urlTemplate)
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String = layer.urlTemplate
            .replace("{z}", MapTileIndex.getZoom(pMapTileIndex).toString())
            .replace("{x}", MapTileIndex.getX(pMapTileIndex).toString())
            .replace("{y}", MapTileIndex.getY(pMapTileIndex).toString())
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

@Preview
@Composable
private fun Preview() {
    PreviewThemeWithBg {
        MapScreen(onBackClick = {})
    }
}
