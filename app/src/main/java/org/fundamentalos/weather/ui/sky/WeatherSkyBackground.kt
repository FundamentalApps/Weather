package org.fundamentalos.weather.ui.sky

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.fundamentalos.weather.R
import org.fundamentalos.weather.ui.LocalScreenCovered
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.OffsetDateTime
import kotlin.math.*

/** Automatic quality is bounded, including on pre-AGSL devices. No per-frame bitmap allocation. */
enum class SkyQuality(val downscale: Float, val cloudLayers: Int, val particles: Int) {
    Standard(3f, 3, 64), Economy(4f, 2, 24)
}

@Composable
private fun rememberPowerSave(): Boolean {
    val context = LocalContext.current
    val power = remember(context) { context.getSystemService(PowerManager::class.java) }
    var saving by remember { mutableStateOf(power.isPowerSaveMode) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) { saving = power.isPowerSaveMode }
        }
        context.registerReceiver(receiver, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED))
        onDispose { context.unregisterReceiver(receiver) }
    }
    return saving
}

@Composable
private fun rememberVisible(): Boolean {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var active by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ -> active = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val covered = LocalScreenCovered.current
    return active && !covered()
}

/** Solar illumination uses wall time and is independent of animation enablement and forecast age. */
@Composable
fun rememberSkyWallTime(): State<OffsetDateTime> {
    val visible = rememberVisible()
    return produceState(OffsetDateTime.now(), visible) {
        if (visible) while (isActive) {
            value = OffsetDateTime.now()
            delay(60_000)
        }
    }
}

@Composable
private fun rememberSkyTime(running: Boolean): State<Float> {
    val clock = remember { SkyAnimationClock() }
    val time = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(running) {
        clock.pause()
        if (!running) return@LaunchedEffect
        var emitted = clock.elapsedNanos
        while (isActive) withFrameNanos { frame ->
            val elapsed = clock.advance(frame)
            if (elapsed - emitted >= 33_000_000L) {
                emitted = elapsed
                time.floatValue = elapsed / 1_000_000_000f
            }
        }
    }
    return time
}

/** How long one sky takes to become another. */
private const val SkyCrossfadeMillis = 3500

@Composable
private fun smoothSky(target: SkyState, animate: Boolean): State<SkyState> = produceState(target, target, animate) {
    if (!animate) { value = target; return@produceState }
    val start = value
    // Eased at both ends, and seen to be: SkyState.interpolate mixes the sun's height in the
    // sky's own terms, so a night-to-day change is not a moment's flip in the middle of this.
    if (start != target) animate(0f, 1f, animationSpec = tween(SkyCrossfadeMillis, easing = FastOutSlowInEasing)) { fraction, _ ->
        value = start.interpolate(target, fraction)
    }
}

@Composable
fun WeatherSkyBackground(
    sky: SkyState,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
    qualityOverride: SkyQuality? = null,
    forceCompat: Boolean = false,
    /** Test-only capture can hold a deterministic phase without changing production timing. */
    timeOverride: Float? = null,
) {
    val context = LocalContext.current
    val lowRam = remember(context) { context.getSystemService(ActivityManager::class.java).isLowRamDevice }
    val powerSave = rememberPowerSave()
    val visible = rememberVisible()
    // Read on resume/recomposition. System animator scale is also respected when opening the app.
    val motionEnabled = remember(visible, animated) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
    val quality = qualityOverride ?: if (lowRam || powerSave) SkyQuality.Economy else SkyQuality.Standard
    val running = animated && visible && !powerSave && motionEnabled && timeOverride == null
    // Held as State and never read in composition: the smoothSky cross-fade rewrites the scene on
    // every frame for 3.5s, so reading it here would recompose this whole subtree per frame (and,
    // stacked on a screen push, blow the frame budget). Each read below is deferred into a draw
    // block instead, turning the cross-fade into a redraw rather than a recompose.
    val sceneState = smoothSky(sky, running)
    val time = rememberSkyTime(running)
    // Decode to <= 627px on this asset: modest texture memory, detail remains below cloud scale.
    val bitmap = remember(context.resources) {
        BitmapFactory.decodeResource(context.resources, R.drawable.sky_cloud_density,
            BitmapFactory.Options().apply { inScaled = false; inSampleSize = 2 })
    }
    Box(modifier.fillMaxSize().clipToBounds()) {
        if (Build.VERSION.SDK_INT >= 33 && !forceCompat) {
            ShaderSky({ sceneState.value }, bitmap, quality, { timeOverride ?: time.value })
        } else {
            CompatSky({ sceneState.value }, bitmap.asImageBitmap(), quality, { timeOverride ?: time.value })
        }
        // Rain needs fine lines: keep particles full-resolution over the low-resolution sky. The
        // layer is always present; whether it paints is decided in the draw phase, so precipitation
        // rising or falling during the cross-fade never restructures the composition.
        Canvas(Modifier.fillMaxSize()) {
            val scene = sceneState.value
            if (scene.precipitation > 0.01f) {
                val t = timeOverride ?: time.value
                drawPrecipitation(scene, t, quality.particles)
                if (running || timeOverride != null) drawLightning(scene, t)
            }
        }
    }
}

@RequiresApi(33)
@Composable
private fun ShaderSky(sky: () -> SkyState, bitmap: android.graphics.Bitmap, quality: SkyQuality, time: () -> Float) {
    val shader = remember(bitmap) {
        RuntimeShader(SKY_SHADER).apply {
            // Mirroring prevents any generated edge mismatch from producing a discontinuity.
            setInputShader("density", BitmapShader(bitmap, Shader.TileMode.MIRROR, Shader.TileMode.MIRROR).apply {
                setFilterMode(BitmapShader.FILTER_MODE_LINEAR)
            })
            setFloatUniform("textureSize", bitmap.width.toFloat(), bitmap.height.toFloat())
        }
    }
    val brush = remember(shader) { ShaderBrush(shader) }
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(maxWidth / quality.downscale, maxHeight / quality.downscale).graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
            scaleX = quality.downscale * 1.02f
            scaleY = quality.downscale * 1.02f
        }) {
            // Read the scene here, in the draw phase: the cross-fade advances it every frame, and
            // the light/palette it feeds are cheap enough to recompute per draw.
            val s = sky()
            val light = skyLight(s)
            val palette = skyPalette(s, light.day, light.dusk)
            shader.setFloatUniform("resolution", size.width, size.height)
            shader.setFloatUniform("time", time())
            shader.setFloatUniform("daylight", light.day)
            shader.setFloatUniform("dusk", light.dusk)
            shader.setFloatUniform("sun", light.sun.x, light.sun.y)
            shader.setColorUniform("topColor", palette[0].toArgb())
            shader.setColorUniform("middleColor", palette[1].toArgb())
            shader.setColorUniform("bottomColor", palette[2].toArgb())
            shader.setFloatUniform("cumulus", s.cumulus)
            shader.setFloatUniform("storm", s.storm)
            shader.setFloatUniform("cover", s.cloudCover)
            shader.setFloatUniform("precip", s.precipitation)
            shader.setFloatUniform("haze", s.haze)
            shader.setFloatUniform("dust", s.dust)
            shader.setFloatUniform("wind", s.windX, s.windY)
            shader.setFloatUniform("moon", s.moonIllumination)
            shader.setFloatUniform("layers", quality.cloudLayers.toFloat())
            drawRect(brush)
        }
    }
}

private data class SkyLight(val day: Float, val dusk: Float, val sun: Offset)
private fun skyLight(sky: SkyState): SkyLight {
    val day = ((sky.sunAltitude + 8f) / 22f).coerceIn(0f, 1f).let { it * it * (3 - 2 * it) }
    val dusk = exp(-((sky.sunAltitude - 2f) / 10f).pow(2)) * day
    val sun = Offset(0.24f + sky.sunProgress * 0.15f,
        if (sky.sunAltitude < -8) 0.20f else (0.70f - sky.sunAltitude / 70f).coerceIn(0.085f, 0.88f))
    return SkyLight(day, dusk, sun)
}

@Composable
private fun CompatSky(sky: () -> SkyState, bitmap: ImageBitmap, quality: SkyQuality, time: () -> Float) {
    // Bake a density contour once; the bank ends in cloud-shaped lobes rather
    // than being hidden behind a vertical sky-colored gradient.
    val cloudBank = remember(bitmap) {
        val source = bitmap.asAndroidBitmap()
        val pixels = IntArray(source.width * source.height)
        source.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
        for (y in 0 until source.height) for (x in 0 until source.width) {
            val index = y * source.width + x
            val density = ((pixels[index] ushr 16) and 255) / 255f
            val bankDensity = density - max(y.toFloat() / source.height - .55f, 0f) * 3.5f
            val edge = ((bankDensity - .25f) / .18f).coerceIn(0f, 1f)
            val alpha = (edge * edge * (3f - 2f * edge) * 255f).roundToInt()
            pixels[index] = (alpha shl 24) or (pixels[index] and 0x00ffffff)
        }
        android.graphics.Bitmap.createBitmap(pixels, source.width, source.height,
            android.graphics.Bitmap.Config.ARGB_8888).asImageBitmap()
    }
    Canvas(Modifier.fillMaxSize()) {
        // Read the scene in the draw phase so the cross-fade redraws without recomposing.
        val s = sky()
        val light = skyLight(s)
        val palette = skyPalette(s, light.day, light.dusk)
        drawRect(Brush.verticalGradient(palette))
        val sun = Offset(light.sun.x * size.width, light.sun.y * size.height)
        val cloudOcclusion = ((s.cloudCover - .28f) / .34f).coerceIn(0f, 1f)
        val solarVisibility = light.day * (1f - cloudOcclusion * cloudOcclusion * (3f - 2f * cloudOcclusion))
        val haloRadius = size.height * .30f
        drawCircle(Brush.radialGradient(listOf(Color(0xFFA8B2BD).copy(alpha = solarVisibility * .58f),
            Color.Transparent), sun, haloRadius), radius = haloRadius, center = sun)
        val bloomRadius = size.height * .10f
        drawCircle(Brush.radialGradient(
            0f to Color(0xFFF2F9FF).copy(alpha = solarVisibility),
            .22f to Color(0xFFF2F9FF).copy(alpha = solarVisibility),
            .45f to Color(0xFFE8E3D6).copy(alpha = solarVisibility * .7f),
            1f to Color.Transparent, center = sun, radius = bloomRadius), radius = bloomRadius, center = sun)
        if (light.day < 0.1f) repeat(45) { index ->
            val x = fraction(sin(index * 127.1f) * 43758.54f)
            val y = fraction(sin(index * 311.7f) * 15321.12f) * 0.78f
            drawCircle(Color.White.copy(alpha = (1 - s.cloudCover) * 0.38f), radius = 1f, center = Offset(x * size.width, y * size.height))
        }
        val t = time()
        val windSpeed = hypot(s.windX, s.windY)
        val flowX = if (windSpeed > .001f) s.windX * (max(windSpeed, .18f) / windSpeed) else .18f
        val flowY = if (windSpeed > .001f) s.windY * (max(windSpeed, .18f) / windSpeed) else .025f
        repeat(quality.cloudLayers) { layer ->
            val extent = (size.width * (1.5f + layer * 0.45f)).roundToInt()
            val x = ((t * flowX * (3 + layer * 2) * 4f) % extent).roundToInt()
            // Bound vertical advection without wrapping the bank through a hard seam.
            val y = -extent / 3 - layer * (size.height / 12).roundToInt() +
                (sin(t * .008f) * flowY * size.height * .08f).roundToInt()
            // Preserve the precomputed cloud contour while tinting the volume.
            val shade = (0.55f + light.day * 0.4f - s.precipitation * 0.24f).coerceIn(0f, 1f)
            val alpha = s.cloudCover * (0.4f + layer * 0.08f)
            val filter = ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                0f, 0f, 0f, 0f, shade * 255, 0f, 0f, 0f, 0f, shade * 255,
                0f, 0f, 0f, 0f, (shade + 0.03f) * 255, 0f, 0f, 0f, alpha, 0f)))
            for (tile in -1..1) drawImage(cloudBank, dstOffset = IntOffset(x + tile * extent, y), dstSize = IntSize(extent, extent), colorFilter = filter)
        }
        drawRect(Color(0xFFB4BDC4).copy(alpha = s.haze * 0.08f))
        if (solarVisibility > .001f) {
            // Camera halo and defocused reflections stay in front of the cloud layers.
            val thin = ((s.cloudCover - .08f) / .16f).coerceIn(0f, 1f).let { it * it * (3f - 2f * it) }
            val radius = size.height * (.340f - .0675f * thin)
            drawCircle(Brush.radialGradient(
                0f to Color.Transparent, .65f to Color.Transparent,
                .74f to Color(0xFFFF9166).copy(alpha = .065f * solarVisibility),
                .80f to Color(0xFF91DF8A).copy(alpha = .055f * solarVisibility),
                .87f to Color(0xFF839BFF).copy(alpha = .060f * solarVisibility),
                1f to Color.Transparent, center = sun, radius = radius), radius = radius, center = sun)
            val axis = Offset(size.width * .58f, size.height * .53f) - sun
            val positions = floatArrayOf(.17f, .30f, .47f, .71f, .94f)
            val radii = floatArrayOf(.010f, .023f, .017f, .026f, .015f)
            repeat(5) { index ->
                val at = sun + axis * positions[index]
                val r = size.height * radii[index]
                val tint = if (index == 0) Color(0xFFC777FF) else Color(0xFFB1DDFF)
                drawCircle(Brush.radialGradient(
                    0f to tint.copy(alpha = solarVisibility * .08f),
                    .75f to tint.copy(alpha = solarVisibility * .06f),
                    1f to Color.Transparent, center = at, radius = r), radius = r, center = at)
            }
        }
    }
}

private fun fraction(value: Float): Float = value - floor(value)

private fun DrawScope.drawPrecipitation(sky: SkyState, time: Float, budget: Int) {
    val count = (budget * sky.precipitation).roundToInt().coerceAtLeast(1)
    val snow = sky.frozen > 0.5f
    repeat(count) { index ->
        val depth = 0.35f + fraction(index * 0.618034f) * 0.65f
        val speed = if (snow) 0.025f + depth * 0.04f else 0.42f + depth * 0.50f
        val y = fraction(index * 0.754877f + time * speed)
        val drift = sky.windX * time * (if (snow) 0.02f else 0.04f)
        val x = fraction(index * 0.569841f + drift + if (snow) sin(time * 0.7f + index) * 0.025f else y * sky.windX * 0.10f)
        val at = Offset(x * size.width, y * size.height)
        if (snow) drawCircle(Color.White.copy(alpha = 0.25f + depth * 0.45f), radius = (0.8f + depth * 2.0f) * density, center = at)
        else drawLine(Color(0xFFD5E7F4).copy(alpha = 0.10f + depth * 0.22f),
            start = at, end = at + Offset(sky.windX * 5f * density, (8 + depth * 16) * density), strokeWidth = (0.4f + depth * 0.4f) * density)
    }
}

private fun skyPalette(sky: SkyState, day: Float, dusk: Float): List<Color> {
    val night = listOf(Color(0xFF050319), Color(0xFF22243F), Color(0xFF304660))
    val clear = listOf(Color(0xFF396A96), Color(0xFF4B83B6), Color(0xFF70A5D6))
    val cloudy = listOf(Color(0xFFB5C7DA), Color(0xFFA3B4C5), Color(0xFF90A2B4))
    val rainy = listOf(Color(0xFF586E80), Color(0xFF354C5D), Color(0xFF2D4151))
    val overcast = ((sky.cloudCover - 0.42f) / 0.20f).coerceIn(0f, 1f) * (1f - sky.cumulus) * (1f - sky.precipitation)
    val wet = (sky.precipitation * 1.4f).coerceIn(0f, 1f)
    return List(3) { index ->
        val base = lerp(lerp(clear[index], cloudy[index], overcast), rainy[index], wet)
        val solar = lerp(night[index], base, day)
        lerp(solar, listOf(Color(0xFF444057), Color(0xFF805A69), Color(0xFFB17C74))[index], dusk * 0.50f)
    }
}

/** One short pulse per 19 active seconds; disabled with reduced motion or a frozen background. */
private fun DrawScope.drawLightning(sky: SkyState, time: Float) {
    if (sky.storm < 0.5f) return
    val phase = time % 19f
    if (phase < 14f || phase > 14.18f) return
    val alpha = sin((phase - 14f) / 0.18f * PI).toFloat().coerceIn(0f, 1f) * sky.storm
    val points = listOf(Offset(.77f, -.02f), Offset(.74f, .04f), Offset(.78f, .085f),
        Offset(.76f, .12f), Offset(.79f, .15f), Offset(.75f, .205f), Offset(.76f, .25f))
    val path = Path().apply {
        moveTo(points.first().x * size.width, points.first().y * size.height)
        points.drop(1).forEach { lineTo(it.x * size.width, it.y * size.height) }
    }
    drawPath(path, Color(0xFFDDE6F3).copy(alpha = alpha * .12f), style = androidx.compose.ui.graphics.drawscope.Stroke(8f * density))
    drawPath(path, Color.White.copy(alpha = alpha * .75f), style = androidx.compose.ui.graphics.drawscope.Stroke(1.4f * density))
}
