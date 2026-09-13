package org.fundamentalos.weather.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.SystemClock
import android.util.Log
import android.util.LruCache
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CopyOnWriteArraySet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.fundamentalos.weather.BuildConfig
import org.fundamentalos.weather.ui.componets.TemperatureField
import org.fundamentalos.weather.weather.provider.fos.FosMapLayer
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sinh

/**
 * A field layer is fetched in square chunks of the world, one request each. Level 0 is the whole
 * world in one image; each level up quarters it, and level [MaxChunkLevel] cuts it into 8 × 8
 * squares of 45°. The map picks the level from its zoom so a chunk is always a few screens wide:
 * a city view sits inside one, a pan across a province never asks for another, and a view of
 * the whole world is one image rather than sixty-four.
 */
const val MaxChunkLevel = 3

/** Pixels across one chunk, at every level. At level 3 a model cell is a handful of pixels. */
const val ChunkSize = 1024

/** One square of the world at [level], for one edition of one layer. */
data class FieldChunkKey(val stamp: String, val level: Int, val x: Int, val y: Int) {
    /** How many chunks span the world at this level. */
    val count: Int get() = 1 shl level

    /** The chunk's centre latitude, which sets how many pixels one model cell spans here. */
    val centreLatitude: Double
        get() {
            val y01 = (y + 0.5) / count
            return Math.toDegrees(atan(sinh(Math.PI * (1 - 2 * y01))))
        }
}

/** Where a layer's chunks come from and how they are read. */
class FieldLayerSource(val layer: FosMapLayer, val field: TemperatureField) {
    /** Names one edition of the layer: a new model run or a moved tile path is a new drawer. */
    val stamp: String = "${layer.id}-${layer.observedAt ?: 0}-" +
        layer.urlTemplate.hashCode().toUInt().toString(16)
}

/**
 * Loads, processes and keeps field chunks. Held for the life of the app, so coming back to the
 * map shows what was there before; the processed chunks are also written to the cache directory,
 * which is what makes a cold start of the map screen a decode rather than a download.
 */
class WeatherFieldStore(context: Context) {
    private val dir = File(context.cacheDir, "weather-field")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Processing a chunk holds five of them in scratch arrays; two at once is the safe count. */
    private val gate = Semaphore(2)
    private val memory = object : LruCache<FieldChunkKey, Bitmap>(MemoryBudgetBytes) {
        override fun sizeOf(key: FieldChunkKey, value: Bitmap): Int = value.allocationByteCount
    }
    private val inFlight = HashMap<FieldChunkKey, Job>()
    private var foregroundInFlight = 0
    private val failedAt = HashMap<FieldChunkKey, Long>()
    private val swept = HashSet<String>()
    private val latestStamp = HashMap<String, String>()
    private val listeners = CopyOnWriteArraySet<() -> Unit>()

    /**
     * The layer list as the server last described it. Coming back to the map builds its overlays
     * from this at once and only swaps them if the fresh answer differs.
     */
    @Volatile
    var layers: List<FosMapLayer>? = null

    /** The chunk if it is already in memory. Never loads. */
    fun peek(key: FieldChunkKey): Bitmap? = memory.get(key)

    /**
     * The same square from the edition of the layer that was last shown, if that is still in
     * memory. It stands in while the current edition is on its way, so a new model run does not
     * blank the map before it fills it again.
     */
    fun peekPrevious(layerId: String, key: FieldChunkKey): Bitmap? {
        val stamp = synchronized(latestStamp) { latestStamp[layerId] } ?: return null
        if (stamp == key.stamp) return null
        return memory.get(key.copy(stamp = stamp))
    }

    /**
     * Starts bringing the chunk into memory unless it is there, on its way, or failed a moment
     * ago. Every listener hears when it lands. A [background] request is one nobody is looking
     * at yet; it waits its turn until nothing in the foreground is loading, and is simply
     * dropped meanwhile, to be asked for again by a later draw.
     */
    fun request(key: FieldChunkKey, source: FieldLayerSource, background: Boolean = false) {
        synchronized(inFlight) {
            if (memory.get(key) != null || inFlight.containsKey(key)) return
            if (background && foregroundInFlight > 0) return
            val failed = failedAt[key]
            if (failed != null && SystemClock.elapsedRealtime() - failed < RetryAfterMillis) return
            if (!background) foregroundInFlight++
            inFlight[key] = scope.launch {
                val bitmap = try {
                    gate.withPermit { load(key, source) }
                } catch (e: Throwable) {
                    // An OutOfMemoryError from a chunk is a chunk to try again later, not a
                    // reason to take the app down.
                    Log.w(Tag, "chunk $key failed: $e")
                    null
                }
                synchronized(inFlight) {
                    inFlight.remove(key)
                    if (!background) foregroundInFlight--
                    if (bitmap != null) {
                        memory.put(key, bitmap)
                        failedAt.remove(key)
                        synchronized(latestStamp) { latestStamp[source.layer.id] = key.stamp }
                    } else {
                        failedAt[key] = SystemClock.elapsedRealtime()
                    }
                }
                if (bitmap != null) withContext(Dispatchers.Main) { listeners.forEach { it() } }
            }
        }
    }

    fun addListener(listener: () -> Unit) { listeners.add(listener) }
    fun removeListener(listener: () -> Unit) { listeners.remove(listener) }

    /** Lets go of all but the most recent chunks, for when the map is no longer on screen. */
    fun trim() {
        memory.trimToSize(MemoryBudgetBytes / 2)
    }

    private fun load(key: FieldChunkKey, source: FieldLayerSource): Bitmap? {
        sweep(source)
        val file = File(File(dir, key.stamp), "${key.level}_${key.x}_${key.y}.png")
        if (file.isFile) {
            val started = SystemClock.elapsedRealtime()
            BitmapFactory.decodeFile(file.path)?.let {
                Log.d(Tag, "chunk ${key.level}/${key.x},${key.y} from disk in ${SystemClock.elapsedRealtime() - started} ms")
                return it
            }
            file.delete()
        }
        val bitmap = fetch(key, source) ?: return null
        // Publish first; the write is for the next visit, not this one.
        scope.launch {
            runCatching {
                file.parentFile?.mkdirs()
                val temp = File(file.path + ".part")
                FileOutputStream(temp).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                if (!temp.renameTo(file)) temp.delete()
            }.onFailure { Log.w(Tag, "could not keep chunk $key: ${it.message}") }
        }
        return bitmap
    }

    private fun fetch(key: FieldChunkKey, source: FieldLayerSource): Bitmap? {
        val radius = blurRadius(key)
        val url = chunkUrl(source.layer, key)
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = ConnectTimeoutMillis
        connection.readTimeout = ReadTimeoutMillis
        connection.setRequestProperty("User-Agent", BuildConfig.APPLICATION_ID)
        val started = SystemClock.elapsedRealtime()
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("HTTP ${connection.responseCode}")
            }
            // Read the whole response first so the network and the processing are timed apart.
            val bytes = connection.inputStream.use { it.readBytes() }
            val fetched = SystemClock.elapsedRealtime()
            val bitmap = source.field.decode(bytes.inputStream(), Gutter, radius)
            Log.d(
                Tag, "chunk ${key.level}/${key.x},${key.y}: ${bytes.size} bytes in ${fetched - started} ms, " +
                    "processed in ${SystemClock.elapsedRealtime() - fetched} ms (radius $radius)"
            )
            return bitmap
        } finally {
            connection.disconnect()
        }
    }

    /** Drops the drawers of older editions of the layer, once per edition. */
    private fun sweep(source: FieldLayerSource) {
        synchronized(swept) { if (!swept.add(source.stamp)) return }
        val prefix = "${source.layer.id}-"
        dir.listFiles()?.forEach { drawer ->
            if (drawer.name.startsWith(prefix) && drawer.name != source.stamp) drawer.deleteRecursively()
        }
    }

    companion object {
        private const val Tag = "WeatherFieldStore"

        /**
         * Room for eight chunks. A view needs four at most, and that leaves the level it just
         * left and the edition it is replacing to stand in while the new ones load.
         */
        private const val MemoryBudgetBytes = 8 * ChunkSize * ChunkSize * 4

        private const val RetryAfterMillis = 10_000L
        private const val ConnectTimeoutMillis = 15_000
        private const val ReadTimeoutMillis = 30_000

        /** Blur radius at the equator at level [MaxChunkLevel], in pixels: about one 0.25° cell. */
        private const val BaseRadius = 6
        private const val MaxRadius = 12

        /** Extra source pixels on each side so the blur reaches over the edge into the neighbour. */
        const val Gutter = 3 * MaxRadius

        /** Half the width of the web mercator world, in metres; a WMS layer asks for its square in these. */
        private const val WorldEdge = 20037508.342789244

        /**
         * Blur over about one model cell. A cell is half as many pixels at each level down, and
         * mercator stretches it towards the poles; the result never exceeds what the gutter covers.
         */
        fun blurRadius(key: FieldChunkKey): Int {
            val atLevel = BaseRadius / (1 shl (MaxChunkLevel - key.level)).toDouble()
            return (atLevel / cos(Math.toRadians(key.centreLatitude))).roundToInt().coerceIn(1, MaxRadius)
        }

        /** The WMS request for a chunk: its square in mercator metres, padded by the gutter. */
        fun chunkUrl(layer: FosMapLayer, key: FieldChunkKey): String {
            val span = 2 * WorldEdge / key.count
            val pad = span * Gutter / ChunkSize
            val minX = -WorldEdge + key.x * span
            val maxY = WorldEdge - key.y * span
            val bbox = "${minX - pad},${maxY - span - pad},${minX + span + pad},${maxY + pad}"
            val size = ChunkSize + Gutter * 2
            return layer.urlTemplate.replace("{bbox}", bbox)
                .replace(Regex("(?i)([?&](?:width|height)=)\\d+")) { it.groupValues[1] + size }
        }
    }
}
