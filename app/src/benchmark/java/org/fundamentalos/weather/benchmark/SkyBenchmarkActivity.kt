package org.fundamentalos.weather.benchmark

import android.graphics.Bitmap
import android.os.*
import android.view.FrameMetrics
import android.view.PixelCopy
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import org.fundamentalos.weather.ui.LocalScreenCovered
import org.fundamentalos.weather.ui.componets.*
import org.fundamentalos.weather.ui.sky.SkyState
import org.fundamentalos.weather.ui.sky.WeatherSkyBackground
import org.fundamentalos.weather.ui.sky.SkyQuality
import org.fundamentalos.weather.ui.theme.WeatherTheme
import org.fundamentalos.weather.ui.theme.WeatherVisualScheme
import org.fundamentalos.weather.ui.theme.weatherVisualScheme
import org.fundamentalos.weather.weather.domain.CurrentWeather
import org.fundamentalos.weather.weather.domain.WeatherCondition
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.OffsetDateTime
import java.util.concurrent.atomic.AtomicBoolean

/** Non-debuggable, offline, release-like harness; never merged into the shipping application. */
class SkyBenchmarkActivity : ComponentActivity() {
    private val collecting = AtomicBoolean(false)
    private val firstFrame = AtomicBoolean(false)
    private val metrics = mutableListOf<DoubleArray>()
    private val worker = HandlerThread("SkyFrameMetrics")
    private var listener: Window.OnFrameMetricsAvailableListener? = null
    private var dropped = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true) }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val renderer = intent.getStringExtra("renderer") ?: "sky"
        val scene = intent.getStringExtra("scene") ?: "clear"
        val workload = intent.getStringExtra("workload") ?: "bare"
        val duration = intent.getIntExtra("duration", 8000).coerceIn(1000, 60000)
        val warmup = intent.getIntExtra("warmup", 3000).coerceIn(500, 15000)
        val id = (intent.getStringExtra("run_id") ?: "$renderer-$scene-$workload").replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val captureTime = if (intent.hasExtra("capture_time")) intent.getFloatExtra("capture_time", 0f) else null
        val fixture = fixture(scene)
        val scheme = weatherVisualScheme(fixture, emptyList(), 37.33, -122.03,
            OffsetDateTime.parse(if (scene.startsWith("night")) "2026-06-21T23:00:00-07:00" else "2026-06-21T14:00:00-07:00"))
        var phase by mutableStateOf("warmup")
        setContent {
            WeatherTheme {
                val covered = phase == "measure" && workload == "covered"
                val coverGetter = remember(covered) { { covered } }
                CompositionLocalProvider(LocalScreenCovered provides coverGetter) {
                    BenchmarkContent(renderer, fixture.condition.text, scheme, workload,
                        animated = workload != "frozen" && phase != "done", covered = covered, captureTime = captureTime)
                }
            }
        }
        worker.start()
        listener = Window.OnFrameMetricsAvailableListener { _, frame, drops ->
            firstFrame.set(true)
            if (collecting.get() && frame.getMetric(FrameMetrics.FIRST_DRAW_FRAME) == 0L) {
                val total = frame.getMetric(FrameMetrics.TOTAL_DURATION) / 1e6
                val deadline = if (Build.VERSION.SDK_INT >= 31) frame.getMetric(FrameMetrics.DEADLINE) / 1e6 else -1.0
                val gpu = if (Build.VERSION.SDK_INT >= 31) frame.getMetric(FrameMetrics.GPU_DURATION).let { if (it >= 0) it / 1e6 else -1.0 } else -1.0
                synchronized(metrics) {
                    metrics.add(doubleArrayOf(total, gpu, deadline, frame.getMetric(FrameMetrics.DRAW_DURATION) / 1e6,
                        if (Build.VERSION.SDK_INT >= 26) frame.getMetric(FrameMetrics.INTENDED_VSYNC_TIMESTAMP).toDouble() else -1.0))
                    dropped += drops
                }
            }
        }
        window.addOnFrameMetricsAvailableListener(listener!!, Handler(worker.looper))
        lifecycleScope.launch {
            withTimeout(15_000) { while (!firstFrame.get()) delay(50) }
            delay(warmup.toLong())
            if (intent.getBooleanExtra("capture", false)) capture(File(getExternalFilesDir(null), "$id.png"))
            if (intent.getBooleanExtra("capture_only", false)) {
                // Visual review explicitly does not collect any performance samples.
                phase = "done"
                File(getExternalFilesDir(null), "$id.json").writeText("{\"captureOnly\":true}")
                return@launch
            }
            phase = "measure"
            // Allow coverage transition/layout to settle before measuring quiescence.
            delay(500)
            val power = getSystemService(PowerManager::class.java)
            val thermalStart = if (Build.VERSION.SDK_INT >= 29) power.currentThermalStatus else -1
            val cpuStart = Process.getElapsedCpuTime()
            val wallStart = SystemClock.elapsedRealtime()
            collecting.set(true)
            delay(duration.toLong())
            collecting.set(false)
            val wall = SystemClock.elapsedRealtime() - wallStart
            val cpu = Process.getElapsedCpuTime() - cpuStart
            // Drain already posted frame notifications before taking the result snapshot.
            delay(150)
            val rows = synchronized(metrics) { metrics.map { it.copyOf() } }
            val memory = Debug.MemoryInfo().also { Debug.getMemoryInfo(it) }
            fun percentile(column: Int, p: Double): Double? {
                val values = rows.map { it[column] }.filter { it >= 0 }.sorted()
                return if (values.isEmpty()) null else values[(kotlin.math.ceil(values.size * p).toInt() - 1).coerceIn(values.indices)]
            }
            val root = JSONObject().apply {
                put("runId", id); put("renderer", renderer); put("scene", scene); put("workload", workload)
                put("device", Build.MODEL); put("sdk", Build.VERSION.SDK_INT)
                put("debuggable", applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0)
                put("width", window.decorView.width); put("height", window.decorView.height)
                put("warmupMs", warmup); put("measurementMs", wall); put("frames", rows.size)
                put("renderedFps", rows.size * 1000.0 / wall); put("callbackDrops", dropped)
                put("totalP50Ms", percentile(0, .50)); put("totalP95Ms", percentile(0, .95)); put("totalP99Ms", percentile(0, .99))
                put("gpuP95Ms", percentile(1, .95)); put("drawP95Ms", percentile(3, .95))
                val eligible = rows.filter { it[2] > 0 }
                put("missedDeadlines", eligible.count { it[0] >= it[2] })
                put("deadlineFrames", eligible.size)
                put("processCpuMs", cpu); put("pssKb", memory.totalPss)
                put("thermalStart", thermalStart); put("thermalEnd", if (Build.VERSION.SDK_INT >= 29) power.currentThermalStatus else -1)
                put("powerSave", power.isPowerSaveMode)
                put("rawColumns", JSONArray(listOf("totalMs", "gpuMs", "deadlineMs", "drawMs", "intendedVsyncNs")))
                put("rawFrames", JSONArray(rows.map { JSONArray(it.toList()) }))
            }
            withContext(Dispatchers.IO) { File(getExternalFilesDir(null), "$id.json").writeText(root.toString(2)) }
            // Leave the scene visible for inspection, but stop its clock after the measurement.
            phase = "done"
        }
    }

    private suspend fun capture(file: File) = suspendCancellableCoroutine<Unit> { continuation ->
        val bitmap = Bitmap.createBitmap(window.decorView.width, window.decorView.height, Bitmap.Config.ARGB_8888)
        PixelCopy.request(window, bitmap, { result ->
            if (result == PixelCopy.SUCCESS) {
                file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                bitmap.recycle()
                continuation.resume(Unit) { _, _, _ -> }
            } else {
                bitmap.recycle()
                continuation.resumeWith(Result.failure(IllegalStateException("PixelCopy failed: $result")))
            }
        }, Handler(mainLooper))
    }

    override fun onDestroy() {
        collecting.set(false)
        listener?.let { window.removeOnFrameMetricsAvailableListener(it) }
        worker.quitSafely()
        super.onDestroy()
    }
}

@Composable
private fun BenchmarkContent(renderer: String, label: String, scheme: WeatherVisualScheme, workload: String, animated: Boolean, covered: Boolean, captureTime: Float?) {
    val glass = if (workload == "scroll" || workload == "glass") rememberGlassBackdropState(
        conditionTextMaterial = if (scheme.useDarkCards) AppleWeatherConditionTextMaterial else AppleWeatherConditionTextMaterialLight,
        cardTitleMaterial = if (scheme.useDarkCards) AppleWeatherCardTitleMaterial else AppleWeatherCardTitleMaterialLight,
    ) else null
    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(
        surface = scheme.card, onSurface = scheme.onCard, onSurfaceVariant = scheme.onCardVariant)) {
    CompositionLocalProvider(LocalGlassBackdrop provides glass,
        LocalUseDarkCards provides scheme.useDarkCards, LocalContentColor provides scheme.onCard) {
        Box(Modifier.fillMaxSize()) {
            val background = if (glass != null) Modifier.glassBackdropSource(glass) else Modifier
            if (renderer == "legacy") {
                val colors = scheme.backgroundColors
                LegacyFluidGradientBackground(colors[0], colors[1], colors[2], colors[3], background, animated)
            } else {
                WeatherSkyBackground(scheme.sky, background, animated,
                    qualityOverride = if (renderer == "economy") SkyQuality.Economy else null,
                    forceCompat = renderer == "compat", timeOverride = captureTime)
            }
            if (workload == "scroll" || workload == "glass") {
                val scroll = rememberScrollState()
                LaunchedEffect(workload) {
                    if (workload == "scroll") while (isActive) {
                        scroll.animateScrollTo(900, tween(4500, easing = LinearEasing))
                        scroll.animateScrollTo(0, tween(4500, easing = LinearEasing))
                    }
                }
                Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Spacer(Modifier.height(64.dp))
                    Text("Weather", color = Color.White, fontSize = 28.sp, modifier = Modifier.conditionTextGlassMaterial())
                    Text("24°", color = Color.White, fontSize = 80.sp, modifier = Modifier.conditionTextGlassMaterial())
                    Text(label, color = Color.White, modifier = Modifier.conditionTextGlassMaterial())
                    Spacer(Modifier.height(110.dp))
                    repeat(10) { index ->
                        InfoCard(title = "Forecast ${index + 1}") {
                            Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                repeat(5) { Text("24°", style = MaterialTheme.typography.headlineSmall) }
                            }
                            Spacer(Modifier.height(72.dp))
                        }
                    }
                }
            }
            if (covered) Box(Modifier.fillMaxSize().background(Color(0xFF192132)))
        }
    }
    }
}

private fun fixture(scene: String): CurrentWeather = CurrentWeather(
    observedAt = "2026-06-21T14:00:00-07:00", tempCelsius = 24, feelsLikeCelsius = 24,
    condition = WeatherCondition(when (scene) { "overcast" -> "104"; "cloudy" -> "101"; "partly_cloudy" -> "103"; "mostly_clear" -> "102"; "storm" -> "302"; "rain" -> "307"; "night" -> "150"; "night_cloudy" -> "151"; else -> "100" }, scene, !scene.startsWith("night")),
    windDegree = 250, windDirection = "SW", windScale = "3", windSpeedKph = 18,
    humidityPercent = 60, precipMillimeters = if (scene == "storm") 6.0 else if (scene == "rain") 3.0 else 0.0,
    pressureHpa = 1013, visibilityKm = if (scene == "rain" || scene == "storm") 8 else 30, cloudPercent = when (scene) { "rain", "storm", "overcast" -> 95; "cloudy" -> 70; "night_cloudy" -> 58; "partly_cloudy" -> 45; "mostly_clear" -> 22; else -> 5 }, dewPointCelsius = 12,
)
