package org.fundamentalos.weather.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.SystemClock
import android.util.Log
import android.util.LruCache
import androidx.core.graphics.createBitmap
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.zip.GZIPInputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.fundamentalos.weather.BuildConfig
import kotlin.math.abs

/**
 * The map is drawn from OpenStreetMap's own vector tiles, which stop at this zoom; the map
 * overzooms them past it.
 */
const val MaxInkZoom = 14

/** One OpenStreetMap vector tile. */
data class InkTileKey(val zoom: Int, val x: Int, val y: Int)

/**
 * The map has two grains. Below [CityZoom] it is a region: coasts, borders, motorways from a
 * provincial zoom, and the names of countries, regions and big cities. From it on it is a
 * city: trunk roads and the names of districts join in. Nothing finer than a trunk road is
 * ever drawn; a weather map is not for finding a street.
 */
const val CityZoom = 8

/** The road classes worth drawing over a weather field, coarsest first. */
enum class RoadClass(val minZoom: Int, val widthDp: Float) {
    Trunk(CityZoom, 1.2f),
    Motorway(6, 1.6f),
}

/**
 * A place of at least district rank, or a country or region from the boundary layer when
 * [adminLevel] is set. Towns, villages and neighbourhoods are not kept: a weather map names
 * the administrative units, not the settlements inside them.
 */
class InkLabel(
    val x: Float,
    val y: Float,
    val names: Map<String, String>,
    val kind: String,
    /** People for a place, square metres of mercator for a country or region: the label's weight. */
    val size: Double,
    val adminLevel: Int,
)

/**
 * How the ink is set: the screen density, for line widths, and the size a tile is set at. A
 * tile is drawn between one and two times its whole-zoom size, so it is set at one and a half
 * times that: never magnified by more than a third, never shrunk by more than a third.
 */
data class InkStyle(val density: Float, val setSizePx: Int) {
    /** The finest detail a mask can show, in units of the tile grid: just over half a pixel. */
    val grain: Float get() = InkTile.Extent.toFloat() / setSizePx * 0.6f
}

/**
 * A tile's water, roads by class and borders by level, as paths in tile coordinates, and how
 * many points they hold between them, which is what they cost to keep.
 */
class InkPaths(val water: Path?, val roads: Map<RoadClass, Path>, val boundaries: Map<Int, Path>, val points: Int) {
    val isEmpty: Boolean get() = water == null && roads.isEmpty() && boundaries.isEmpty()

    companion object {
        val None = InkPaths(null, emptyMap(), emptyMap(), 0)
    }
}

/**
 * A tile's ink as two coverage masks: one of its water, one of its lines. Masks carry no
 * colour, so the theme's colours go on at draw time and a tile set once serves both themes.
 */
class InkMasks(val water: Bitmap?, val lines: Bitmap?) {
    val bytes: Int get() = (water?.allocationByteCount ?: 0) + (lines?.allocationByteCount ?: 0)
}

/**
 * A vector tile reduced to what the map draws: its paths, its labels in tile coordinates, and
 * the paths set into masks. Built once, off the main thread; the masks are blitted every frame.
 * Stroking the roads on every frame instead — which is what drawing the paths would mean —
 * cost the render thread two hundred milliseconds a frame at a regional zoom.
 */
class InkTile(
    val extent: Int,
    val zoom: Int,
    val paths: InkPaths,
    val labels: List<InkLabel>,
    val masks: InkMasks? = null,
    /** Whether the masks have been set; a tile with nothing to draw is set and has none. */
    val isSet: Boolean = false,
) {
    /**
     * The same tile with its masks set, and without its paths: nothing reads them once they
     * are set, and a busy coast's run to megabytes. A new object rather than a change to this
     * one: a tile in the cache must not change size under the cache, which throws when it
     * notices.
     */
    fun set(style: InkStyle): InkTile =
        InkTile(extent, zoom, InkPaths.None, labels, InkRenderer.render(paths, style, zoom), isSet = true)

    /** What the cache charges for the tile: the masks, and an estimate for the paths and labels. */
    val bytes: Int = (masks?.bytes ?: 0) + paths.points * BytesPerPoint + labels.size * 512 + 4096

    companion object {
        /** The layers the map reads; the rest of the tile is not even decoded. */
        val Layers = setOf("ocean", "water_polygons", "streets", "boundaries", "boundary_labels", "place_labels")

        /**
         * The place kinds that count as a district or above. OpenStreetMap tags districts and
         * counties as cities the world over (金坛区 and 嘉善县 are both `city`), while towns and
         * everything smaller are not.
         */
        private val PlaceKinds = setOf("capital", "state_capital", "city")

        /** The name keys kept per label: the local name, English, and the app's own languages. */
        private val NameKeys = setOf(
            "name", "name_en", "name_de", "name_fr", "name_pl", "name_ru", "name_zh", "name_zh-Hans", "name_zh-Hant",
        )

        /** The grid every layer is brought to. */
        const val Extent = 4096

        /** What a path costs per point: two floats and a verb, with some room. */
        const val BytesPerPoint = 10

        /**
         * Reads the tile's layers into paths and labels. Points closer than [grain] — in
         * units of the grid — to the last one kept are dropped: a coast is drawn with far
         * more points than the mask can show, and stroking them costs the most of setting
         * a tile.
         */
        fun from(layers: Map<String, MvtLayer>, zoom: Int, grain: Float): InkTile {
            // The layers of one tile need not share an extent — the ocean often comes at 2048
            // where the labels are at 4096 — so each is scaled onto one grid as it is read.
            fun MvtLayer.grid(): Float = Extent.toFloat() / extent
            var points = 0
            val water = Path().apply { fillType = Path.FillType.EVEN_ODD }
            var hasWater = false
            for (name in listOf("ocean", "water_polygons")) {
                val layer = layers[name] ?: continue
                val k = layer.grid()
                for (feature in layer.features) {
                    if (feature.type != 3) continue
                    for (ring in feature.parts) { points += water.addPolyline(ring, k, grain, close = true); hasWater = true }
                }
            }
            val roads = HashMap<RoadClass, Path>()
            layers["streets"]?.let { layer ->
                val k = layer.grid()
                for (feature in layer.features) {
                    if (feature.type != 2) continue
                    val roadClass = when (feature.tags["kind"]) {
                        "motorway" -> RoadClass.Motorway
                        "trunk" -> RoadClass.Trunk
                        else -> continue
                    }
                    val path = roads.getOrPut(roadClass) { Path() }
                    for (line in feature.parts) points += path.addPolyline(line, k, grain, close = false)
                }
            }
            val boundaries = HashMap<Int, Path>()
            layers["boundaries"]?.let { layer ->
                val k = layer.grid()
                for (feature in layer.features) {
                    if (feature.type != 2) continue
                    if (feature.tags["maritime"] == true) continue
                    val level = (feature.tags["admin_level"] as? Long)?.toInt() ?: continue
                    if (level != 2 && level != 4) continue
                    val path = boundaries.getOrPut(level) { Path() }
                    for (line in feature.parts) points += path.addPolyline(line, k, grain, close = false)
                }
            }
            val labels = ArrayList<InkLabel>()
            layers["place_labels"]?.let { layer ->
                val k = layer.grid()
                for (feature in layer.features) {
                    if (feature.tags["kind"] !in PlaceKinds) continue
                    labels += feature.toLabels(adminLevel = 0, k) ?: continue
                }
            }
            layers["boundary_labels"]?.let { layer ->
                val k = layer.grid()
                for (feature in layer.features) {
                    val level = (feature.tags["admin_level"] as? Long)?.toInt() ?: continue
                    if (level != 2 && level != 4) continue
                    labels += feature.toLabels(adminLevel = level, k) ?: continue
                }
            }
            return InkTile(Extent, zoom, InkPaths(water.takeIf { hasWater }, roads, boundaries, points), labels)
        }

        private fun MvtFeature.toLabels(adminLevel: Int, k: Float): List<InkLabel>? {
            if (type != 1) return null
            val names = HashMap<String, String>()
            for ((key, value) in tags) if (key in NameKeys && value is String && value.isNotBlank()) names[key] = value
            if (names.isEmpty()) return null
            val kind = tags["kind"] as? String ?: ""
            val size = if (adminLevel == 0) ((tags["population"] as? Long) ?: 0L).toDouble()
            else (tags["way_area"] as? Number)?.toDouble() ?: 0.0
            val out = ArrayList<InkLabel>()
            for (part in parts) for (i in 0 until part.size - 1 step 2) {
                out += InkLabel(part[i] * k, part[i + 1] * k, names, kind, size, adminLevel)
            }
            return out
        }

        /**
         * Adds the line, less the points within [grain] of the one before, and returns how
         * many points it kept. The last point of an open line is always kept.
         */
        private fun Path.addPolyline(points: FloatArray, k: Float, grain: Float, close: Boolean): Int {
            if (points.size < 4) return 0
            var lastX = points[0] * k
            var lastY = points[1] * k
            moveTo(lastX, lastY)
            var kept = 1
            val end = points.size - 2
            for (i in 2 until points.size - 1 step 2) {
                val x = points[i] * k
                val y = points[i + 1] * k
                if (i != end && abs(x - lastX) + abs(y - lastY) < grain) continue
                lineTo(x, y)
                lastX = x
                lastY = y
                kept++
            }
            if (close) close()
            return kept
        }
    }
}

/** Sets a tile's water, roads and borders into coverage masks of the style's size. */
object InkRenderer {
    /**
     * Line widths are set for the middle of a tile's range of drawn sizes, so they read a
     * little thin at a whole zoom and a little heavy just before the next.
     */
    private const val WidthAtSetSize = 1.2f

    fun render(paths: InkPaths, style: InkStyle, zoom: Int): InkMasks? {
        if (paths.isEmpty) return null
        val size = style.setSizePx
        val scale = size.toFloat() / InkTile.Extent
        val width = style.density * WidthAtSetSize / scale
        fun mask(draw: (Canvas, Paint) -> Unit): Bitmap {
            val bitmap = createBitmap(size, size, Bitmap.Config.ALPHA_8)
            val canvas = Canvas(bitmap)
            canvas.scale(scale, scale)
            // Tiles carry a margin of their neighbours' geometry; without the clip it is drawn twice.
            canvas.clipRect(0f, 0f, InkTile.Extent.toFloat(), InkTile.Extent.toFloat())
            draw(canvas, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK })
            return bitmap
        }
        val water = paths.water?.let { water ->
            mask { canvas, paint ->
                paint.style = Paint.Style.FILL
                canvas.drawPath(water, paint)
            }
        }
        val hasLines = paths.water != null || paths.roads.isNotEmpty() || paths.boundaries.isNotEmpty()
        val lines = if (!hasLines) null else mask { canvas, paint ->
            paint.style = Paint.Style.STROKE
            // A coastline keeps land and sea apart where the field alone would blur them. It
            // is drawn as a hairline — one pixel of the mask, whatever the width — which the
            // renderer draws without building a stroke around each of its hundred thousand
            // corners: a coast that cost the most of setting a tile now costs a tenth of it.
            paths.water?.let {
                paint.alpha = 90
                paint.strokeWidth = 0f
                canvas.drawPath(it, paint)
            }
            paint.strokeJoin = Paint.Join.ROUND
            paint.strokeCap = Paint.Cap.ROUND
            for ((level, path) in paths.boundaries) {
                if (level == 4 && zoom < 4) continue
                paint.alpha = if (level == 2) 150 else 105
                paint.strokeWidth = (if (level == 2) 1.3f else 0.8f) * width
                canvas.drawPath(path, paint)
            }
            paint.alpha = 62
            for (roadClass in RoadClass.entries) {
                if (zoom < roadClass.minZoom) continue
                val path = paths.roads[roadClass] ?: continue
                paint.strokeWidth = roadClass.widthDp * width
                canvas.drawPath(path, paint)
            }
        }
        return InkMasks(water, lines)
    }
}

/**
 * Loads and keeps the map's vector tiles. Held for the life of the app so that a place seen
 * once is drawn again at once; raw tiles are also kept on disk for a month.
 */
class InkTileStore(context: Context) {
    private val dir = File(context.cacheDir, "osm-ink")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    /** How many tiles load at once. */
    private val gate = Semaphore(3)
    /**
     * How many baked tiles are set at once. Its own gate: setting is a moment's work that
     * must not wait behind the loads, which mostly wait on the network.
     */
    private val settingGate = Semaphore(2)
    /** Setting ahead of a zoom out takes one core, and leaves the rest to what is looked at. */
    private val backgroundSettingGate = Semaphore(1)
    private val known = HashSet<InkTileKey>()

    /**
     * Tiles that have been set, charged by their bitmaps. A tile is in this or in the pantry,
     * never both, so either one letting go of a tile means it is no longer known. Neither
     * cache looks into the other from inside its own lock: each locks itself, and two threads
     * evicting at once would wait on each other.
     */
    private val memory: LruCache<InkTileKey, InkTile> = object : LruCache<InkTileKey, InkTile>(MemoryBudgetBytes) {
        override fun sizeOf(key: InkTileKey, value: InkTile): Int = value.bytes
        override fun entryRemoved(evicted: Boolean, key: InkTileKey, oldValue: InkTile, newValue: InkTile?) {
            if (newValue == null) synchronized(known) { known.remove(key) }
        }
    }

    /**
     * Tiles baked ahead but not yet set: paths and labels only, so a screen of them is cheap.
     * Kept apart from the set tiles, or the ones nobody has looked at yet would always be the
     * first to go and be baked again and again.
     */
    private val pantry: LruCache<InkTileKey, InkTile> = object : LruCache<InkTileKey, InkTile>(PantryBudgetBytes) {
        override fun sizeOf(key: InkTileKey, value: InkTile): Int = value.bytes
        override fun entryRemoved(evicted: Boolean, key: InkTileKey, oldValue: InkTile, newValue: InkTile?) {
            if (newValue == null) synchronized(known) { known.remove(key) }
            if (evicted) crowdedOutAt[key] = SystemClock.elapsedRealtime()
        }
    }
    private val inFlight = HashMap<InkTileKey, Job>()
    private var foregroundInFlight = 0
    private val resetting = HashSet<InkTileKey>()
    private val failedAt = HashMap<InkTileKey, Long>()
    /**
     * When a baked tile was crowded out of the pantry. It is not baked again for a while: if
     * a bake does not fit, its tiles would otherwise crowd each other out and be baked over
     * and over, a decode at a time.
     */
    private val crowdedOutAt = ConcurrentHashMap<InkTileKey, Long>()
    private val listeners = CopyOnWriteArraySet<() -> Unit>()

    /** The style tiles are set in. Changing it lets go of every tile set in the old one. */
    @Volatile
    var style: InkStyle? = null
        set(value) {
            if (field == value) return
            field = value
            memory.evictAll()
            pantry.evictAll()
            crowdedOutAt.clear()
        }

    /** The tile if it is already in memory, set or only baked. Never loads. */
    fun peek(key: InkTileKey): InkTile? = memory.get(key) ?: pantry.get(key)

    /** Whether the tile is in memory, without counting as a use of it. */
    fun has(key: InkTileKey): Boolean = synchronized(known) { key in known }

    /**
     * Starts bringing the tile into memory unless it is there, on its way, or failed a moment
     * ago. Every listener hears when it lands. A [background] request is one nobody is looking
     * at yet — a coarser level baked ahead of a zoom out; it waits its turn until nothing in the
     * foreground is loading, and is simply dropped meanwhile, to be asked for again by a later
     * draw. A background tile is fetched and decoded but not set: its paths are kept, small,
     * and set once the tile is looked at.
     */
    fun request(key: InkTileKey, background: Boolean = false) {
        val style = style ?: return
        synchronized(inFlight) {
            if (has(key) || inFlight.containsKey(key)) return
            if (background && foregroundInFlight > 0) return
            val now = SystemClock.elapsedRealtime()
            val failed = failedAt[key]
            if (failed != null && now - failed < RetryAfterMillis) return
            val crowdedOut = crowdedOutAt[key]
            if (background && crowdedOut != null && now - crowdedOut < BakeAgainAfterMillis) return
            if (!background) foregroundInFlight++
            inFlight[key] = scope.launch {
                val tile = try {
                    gate.withPermit { load(key, style, set = !background) }
                } catch (e: Throwable) {
                    Log.w(Tag, "tile $key failed: $e")
                    null
                }
                synchronized(inFlight) {
                    inFlight.remove(key)
                    if (!background) foregroundInFlight--
                    if (tile != null) {
                        // A tile set in a style that has since changed is not kept.
                        if (this@InkTileStore.style == style) {
                            synchronized(known) { known.add(key) }
                            if (tile.isSet) memory.put(key, tile) else pantry.put(key, tile)
                        }
                        failedAt.remove(key)
                    } else {
                        failedAt[key] = SystemClock.elapsedRealtime()
                    }
                }
                if (tile != null) withContext(Dispatchers.Main) { listeners.forEach { it() } }
            }
        }
    }

    /**
     * Sets a tile that was baked ahead, now that it is looked at — or, in the [background],
     * before it is, one at a time, so that a zoom out finds the next level's ink ready. Nothing
     * happens if it is set already or being set.
     */
    fun set(key: InkTileKey, background: Boolean = false) {
        val style = style ?: return
        val tile = peek(key) ?: return
        synchronized(inFlight) {
            if (tile.isSet || !resetting.add(key)) return
            scope.launch {
                try {
                    val started = SystemClock.elapsedRealtime()
                    val gate = if (background) backgroundSettingGate else settingGate
                    val done = gate.withPermit { tile.set(style) }
                    Log.d(Tag, "tile ${key.zoom}/${key.x},${key.y}: set from the pantry in ${SystemClock.elapsedRealtime() - started} ms")
                    // Swap it in among the set tiles, so the cache charges for the masks.
                    synchronized(inFlight) {
                        if (peek(key) === tile && this@InkTileStore.style == style) {
                            pantry.remove(key)
                            memory.remove(key)
                            synchronized(known) { known.add(key) }
                            memory.put(key, done)
                        }
                    }
                    withContext(Dispatchers.Main) { listeners.forEach { it() } }
                } catch (e: Throwable) {
                    Log.w(Tag, "tile $key could not be set: $e")
                } finally {
                    synchronized(inFlight) { resetting.remove(key) }
                }
            }
        }
    }

    fun addListener(listener: () -> Unit) { listeners.add(listener) }
    fun removeListener(listener: () -> Unit) { listeners.remove(listener) }

    /** Lets go of all but the most recent tiles, for when the map is no longer on screen. */
    fun trim() {
        memory.trimToSize(MemoryBudgetBytes / 4)
    }

    /** Whether any set tile is in memory; for the map to decide whether it owes a fade. */
    val isEmpty: Boolean get() = memory.size() == 0 && pantry.size() == 0

    private fun load(key: InkTileKey, style: InkStyle, set: Boolean): InkTile {
        val file = File(File(dir, key.zoom.toString()), "${key.x}_${key.y}.mvt")
        val started = SystemClock.elapsedRealtime()
        var bytes: ByteArray? = null
        if (file.isFile && System.currentTimeMillis() - file.lastModified() < DiskTtlMillis) {
            bytes = runCatching { file.readBytes() }.getOrNull()
        }
        val fetched: Boolean
        if (bytes == null) {
            bytes = fetch(key)
            fetched = true
            scope.launch {
                runCatching {
                    file.parentFile?.mkdirs()
                    val temp = File(file.path + ".part")
                    temp.writeBytes(bytes)
                    if (!temp.renameTo(file)) temp.delete()
                }.onFailure { Log.w(Tag, "could not keep tile $key: ${it.message}") }
            }
        } else {
            fetched = false
        }
        val read = SystemClock.elapsedRealtime()
        val decoded = InkTile.from(MvtDecoder.decode(bytes, InkTile.Layers), key.zoom, style.grain)
        val tile = if (set) decoded.set(style) else decoded
        Log.d(
            Tag, "tile ${key.zoom}/${key.x},${key.y}: ${bytes.size} bytes " +
                "${if (fetched) "fetched" else "from disk"} in ${read - started} ms, " +
                "decoded${if (set) " and set" else ""} in ${SystemClock.elapsedRealtime() - read} ms, " +
                "${tile.paths.points} points, ${tile.labels.size} labels, ${tile.bytes / 1024} KB"
        )
        return tile
    }

    private fun fetch(key: InkTileKey): ByteArray {
        val url = TileUrl.replace("{z}", key.zoom.toString())
            .replace("{x}", key.x.toString())
            .replace("{y}", key.y.toString())
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = ConnectTimeoutMillis
        connection.readTimeout = ReadTimeoutMillis
        // The tile policy asks for a User-Agent that names the app.
        connection.setRequestProperty("User-Agent", BuildConfig.APPLICATION_ID)
        connection.setRequestProperty("Accept-Encoding", "gzip")
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("HTTP ${connection.responseCode}")
            }
            val raw = connection.inputStream.use { it.readBytes() }
            val gzipped = connection.contentEncoding.equals("gzip", ignoreCase = true) ||
                (raw.size > 2 && raw[0] == 0x1F.toByte() && raw[1] == 0x8B.toByte())
            return if (gzipped) GZIPInputStream(raw.inputStream()).use { it.readBytes() } else raw
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val Tag = "InkTileStore"

        /**
         * OpenStreetMap's own vector tiles, under the same usage policy as the raster tiles the
         * map used to be drawn from.
         */
        private const val TileUrl = "https://vector.openstreetmap.org/shortbread_v1/{z}/{x}/{y}.mvt"

        /**
         * A screen of set tiles is about two and a quarter times the screen's own pixels in
         * mask bytes (two one-byte masks at one and a half times the size), whatever the zoom;
         * this holds three levels' screens with their margins — the one looked at, the finer
         * one it came from, and the coarser one set ahead of it.
         */
        private const val MemoryBudgetBytes = 96 * 1024 * 1024

        /**
         * Baked tiles, kept as paths and labels: a few screens of the coarser levels and the
         * world. A tile of a busy coast at a regional zoom is about a quarter of a megabyte.
         */
        private const val PantryBudgetBytes = 32 * 1024 * 1024
        /** Coastlines, borders and roads change slowly; a month is fine. */
        private const val DiskTtlMillis = 30L * 24 * 60 * 60 * 1000
        private const val RetryAfterMillis = 10_000L
        private const val BakeAgainAfterMillis = 60_000L
        private const val ConnectTimeoutMillis = 15_000
        private const val ReadTimeoutMillis = 30_000
    }
}
