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
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
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

/**
 * The map is drawn from OpenStreetMap's own vector tiles, which stop at this zoom; the map
 * overzooms them past it.
 */
const val MaxInkZoom = 14

/** One OpenStreetMap vector tile. */
data class InkTileKey(val zoom: Int, val x: Int, val y: Int)

/** The road classes worth drawing over a weather field, coarsest first. */
enum class RoadClass(val minZoom: Int, val widthDp: Float) {
    Minor(13, 0.5f),
    Tertiary(11, 0.7f),
    Secondary(10, 0.9f),
    Primary(8, 1.2f),
    Motorway(4, 1.6f),
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
data class InkStyle(val density: Float, val setSizePx: Int)

/** A tile's water, roads by class and borders by level, as paths in tile coordinates. */
class InkPaths(val water: Path?, val roads: Map<RoadClass, Path>, val boundaries: Map<Int, Path>) {
    val isEmpty: Boolean get() = water == null && roads.isEmpty() && boundaries.isEmpty()
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
) {
    @Volatile var masks: InkMasks? = null
        private set

    fun set(style: InkStyle) {
        masks = InkRenderer.render(paths, style, zoom)
    }

    /** What the cache charges for the tile: the masks, plus a guess for the paths and labels. */
    val bytes: Int get() = (masks?.bytes ?: 0) + labels.size * 256 + 200_000

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

        fun from(layers: Map<String, MvtLayer>, zoom: Int): InkTile {
            // The layers of one tile need not share an extent — the ocean often comes at 2048
            // where the labels are at 4096 — so each is scaled onto one grid as it is read.
            fun MvtLayer.grid(): Float = Extent.toFloat() / extent
            val water = Path().apply { fillType = Path.FillType.EVEN_ODD }
            var hasWater = false
            for (name in listOf("ocean", "water_polygons")) {
                val layer = layers[name] ?: continue
                val k = layer.grid()
                for (feature in layer.features) {
                    if (feature.type != 3) continue
                    for (ring in feature.parts) { water.addPolyline(ring, k, close = true); hasWater = true }
                }
            }
            val roads = HashMap<RoadClass, Path>()
            layers["streets"]?.let { layer ->
                val k = layer.grid()
                for (feature in layer.features) {
                    if (feature.type != 2) continue
                    val roadClass = when (feature.tags["kind"]) {
                        "motorway", "trunk" -> RoadClass.Motorway
                        "primary" -> RoadClass.Primary
                        "secondary" -> RoadClass.Secondary
                        "tertiary" -> RoadClass.Tertiary
                        "residential", "unclassified", "living_street", "pedestrian" -> RoadClass.Minor
                        else -> continue
                    }
                    val path = roads.getOrPut(roadClass) { Path() }
                    for (line in feature.parts) path.addPolyline(line, k, close = false)
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
                    for (line in feature.parts) path.addPolyline(line, k, close = false)
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
            return InkTile(Extent, zoom, InkPaths(water.takeIf { hasWater }, roads, boundaries), labels)
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

        private fun Path.addPolyline(points: FloatArray, k: Float, close: Boolean) {
            if (points.size < 4) return
            moveTo(points[0] * k, points[1] * k)
            for (i in 2 until points.size - 1 step 2) lineTo(points[i] * k, points[i + 1] * k)
            if (close) close()
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
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ALPHA_8)
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
            paint.strokeJoin = Paint.Join.ROUND
            paint.strokeCap = Paint.Cap.ROUND
            // A coastline keeps land and sea apart where the field alone would blur them.
            paths.water?.let {
                paint.alpha = 64
                paint.strokeWidth = 0.6f * width
                canvas.drawPath(it, paint)
            }
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
    private val gate = Semaphore(3)
    private val known = HashSet<InkTileKey>()

    /** Tiles that have been set, charged by their bitmaps. */
    private val memory: LruCache<InkTileKey, InkTile> = object : LruCache<InkTileKey, InkTile>(MemoryBudgetBytes) {
        override fun sizeOf(key: InkTileKey, value: InkTile): Int = value.bytes
        override fun entryRemoved(evicted: Boolean, key: InkTileKey, oldValue: InkTile, newValue: InkTile?) {
            if (newValue == null) synchronized(known) { if (pantry.get(key) == null) known.remove(key) }
        }
    }

    /**
     * Tiles baked ahead but not yet set: paths and labels only, so a small count of them is
     * cheap. Kept apart from the set tiles, or the ones nobody has looked at yet would always
     * be the first to go and be baked again and again.
     */
    private val pantry: LruCache<InkTileKey, InkTile> = object : LruCache<InkTileKey, InkTile>(PantryTiles) {
        override fun entryRemoved(evicted: Boolean, key: InkTileKey, oldValue: InkTile, newValue: InkTile?) {
            if (newValue == null) synchronized(known) { if (memory.get(key) == null) known.remove(key) }
        }
    }
    private val inFlight = HashMap<InkTileKey, Job>()
    private var foregroundInFlight = 0
    private val resetting = HashSet<InkTileKey>()
    private val failedAt = HashMap<InkTileKey, Long>()
    private val listeners = CopyOnWriteArraySet<() -> Unit>()

    /** The style tiles are set in. Changing it lets go of every tile set in the old one. */
    @Volatile
    var style: InkStyle? = null
        set(value) {
            if (field == value) return
            field = value
            memory.evictAll()
            pantry.evictAll()
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
            val failed = failedAt[key]
            if (failed != null && SystemClock.elapsedRealtime() - failed < RetryAfterMillis) return
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
                            if (tile.masks != null) memory.put(key, tile) else pantry.put(key, tile)
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
     * Sets a tile that was baked ahead, in the background, now that it is looked at. Nothing
     * happens if it is set already or being set.
     */
    fun set(key: InkTileKey) {
        val style = style ?: return
        val tile = peek(key) ?: return
        synchronized(inFlight) {
            if (tile.masks != null || !resetting.add(key)) return
            scope.launch {
                try {
                    gate.withPermit { tile.set(style) }
                    // Move it among the set tiles, so the cache charges for the masks.
                    synchronized(inFlight) {
                        if (peek(key) === tile) {
                            memory.remove(key)
                            pantry.remove(key)
                            synchronized(known) { known.add(key) }
                            memory.put(key, tile)
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
        val tile = InkTile.from(MvtDecoder.decode(bytes, InkTile.Layers), key.zoom)
        if (set) tile.set(style)
        Log.d(
            Tag, "tile ${key.zoom}/${key.x},${key.y}: ${bytes.size} bytes " +
                "${if (fetched) "fetched" else "from disk"} in ${read - started} ms, " +
                "decoded and set in ${SystemClock.elapsedRealtime() - read} ms, ${tile.labels.size} labels"
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
         * this is a few screens' worth, with their margins.
         */
        private const val MemoryBudgetBytes = 48 * 1024 * 1024

        /** Baked tiles kept without a bitmap: a view's coarser levels and the world, twice over. */
        private const val PantryTiles = 96
        /** Coastlines, borders and roads change slowly; a month is fine. */
        private const val DiskTtlMillis = 30L * 24 * 60 * 60 * 1000
        private const val RetryAfterMillis = 10_000L
        private const val ConnectTimeoutMillis = 15_000
        private const val ReadTimeoutMillis = 30_000
    }
}
