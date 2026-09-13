package org.fundamentalos.weather.ui.map

import android.content.Context
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
    Motorway(5, 1.6f),
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
 * A vector tile reduced to what the map draws, as paths in tile coordinates: water, roads by
 * class, boundaries by level, and the labels. Built once, off the main thread; drawn many times.
 */
class InkTile(
    val extent: Int,
    val water: Path?,
    val roads: Map<RoadClass, Path>,
    val boundaries: Map<Int, Path>,
    val labels: List<InkLabel>,
) {
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

        fun from(layers: Map<String, MvtLayer>): InkTile {
            val extent = layers.values.firstOrNull()?.extent ?: 4096
            val water = Path().apply { fillType = Path.FillType.EVEN_ODD }
            var hasWater = false
            for (name in listOf("ocean", "water_polygons")) {
                for (feature in layers[name]?.features.orEmpty()) {
                    if (feature.type != 3) continue
                    for (ring in feature.parts) { water.addPolyline(ring, close = true); hasWater = true }
                }
            }
            val roads = HashMap<RoadClass, Path>()
            for (feature in layers["streets"]?.features.orEmpty()) {
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
                for (line in feature.parts) path.addPolyline(line, close = false)
            }
            val boundaries = HashMap<Int, Path>()
            for (feature in layers["boundaries"]?.features.orEmpty()) {
                if (feature.type != 2) continue
                if (feature.tags["maritime"] == true) continue
                val level = (feature.tags["admin_level"] as? Long)?.toInt() ?: continue
                if (level != 2 && level != 4) continue
                val path = boundaries.getOrPut(level) { Path() }
                for (line in feature.parts) path.addPolyline(line, close = false)
            }
            val labels = ArrayList<InkLabel>()
            for (feature in layers["place_labels"]?.features.orEmpty()) {
                if (feature.tags["kind"] !in PlaceKinds) continue
                labels += feature.toLabels(adminLevel = 0) ?: continue
            }
            for (feature in layers["boundary_labels"]?.features.orEmpty()) {
                val level = (feature.tags["admin_level"] as? Long)?.toInt() ?: continue
                if (level != 2 && level != 4) continue
                labels += feature.toLabels(adminLevel = level) ?: continue
            }
            return InkTile(extent, water.takeIf { hasWater }, roads, boundaries, labels)
        }

        private fun MvtFeature.toLabels(adminLevel: Int): List<InkLabel>? {
            if (type != 1) return null
            val names = HashMap<String, String>()
            for ((key, value) in tags) if (key in NameKeys && value is String && value.isNotBlank()) names[key] = value
            if (names.isEmpty()) return null
            val kind = tags["kind"] as? String ?: ""
            val size = if (adminLevel == 0) ((tags["population"] as? Long) ?: 0L).toDouble()
            else (tags["way_area"] as? Number)?.toDouble() ?: 0.0
            val out = ArrayList<InkLabel>()
            for (part in parts) for (i in 0 until part.size - 1 step 2) {
                out += InkLabel(part[i], part[i + 1], names, kind, size, adminLevel)
            }
            return out
        }

        private fun Path.addPolyline(points: FloatArray, close: Boolean) {
            if (points.size < 4) return
            moveTo(points[0], points[1])
            for (i in 2 until points.size - 1 step 2) lineTo(points[i], points[i + 1])
            if (close) close()
        }
    }
}

/**
 * Loads and keeps the map's vector tiles. Held for the life of the app so that a place seen
 * once is drawn again at once; raw tiles are also kept on disk for a week.
 */
class InkTileStore(context: Context) {
    private val dir = File(context.cacheDir, "osm-ink")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gate = Semaphore(3)
    private val memory = LruCache<InkTileKey, InkTile>(MemoryTiles)
    private val inFlight = HashMap<InkTileKey, Job>()
    private val failedAt = HashMap<InkTileKey, Long>()
    private val listeners = CopyOnWriteArraySet<() -> Unit>()

    /** The tile if it is already in memory. Never loads. */
    fun peek(key: InkTileKey): InkTile? = memory.get(key)

    /** Starts bringing the tile into memory unless it is there, on its way, or failed a moment ago. */
    fun request(key: InkTileKey) {
        synchronized(inFlight) {
            if (memory.get(key) != null || inFlight.containsKey(key)) return
            val failed = failedAt[key]
            if (failed != null && SystemClock.elapsedRealtime() - failed < RetryAfterMillis) return
            inFlight[key] = scope.launch {
                val tile = try {
                    gate.withPermit { load(key) }
                } catch (e: Throwable) {
                    Log.w(Tag, "tile $key failed: $e")
                    null
                }
                synchronized(inFlight) {
                    inFlight.remove(key)
                    if (tile != null) {
                        memory.put(key, tile)
                        failedAt.remove(key)
                    } else {
                        failedAt[key] = SystemClock.elapsedRealtime()
                    }
                }
                if (tile != null) withContext(Dispatchers.Main) { listeners.forEach { it() } }
            }
        }
    }

    fun addListener(listener: () -> Unit) { listeners.add(listener) }
    fun removeListener(listener: () -> Unit) { listeners.remove(listener) }

    /** Lets go of all but the most recent tiles, for when the map is no longer on screen. */
    fun trim() {
        memory.trimToSize(MemoryTiles / 4)
    }

    private fun load(key: InkTileKey): InkTile {
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
        val tile = InkTile.from(MvtDecoder.decode(bytes, InkTile.Layers))
        Log.d(
            Tag, "tile ${key.zoom}/${key.x},${key.y}: ${bytes.size} bytes " +
                "${if (fetched) "fetched" else "from disk"} in ${read - started} ms, " +
                "decoded in ${SystemClock.elapsedRealtime() - read} ms, ${tile.labels.size} labels"
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

        private const val MemoryTiles = 48
        private const val DiskTtlMillis = 7L * 24 * 60 * 60 * 1000
        private const val RetryAfterMillis = 10_000L
        private const val ConnectTimeoutMillis = 15_000
        private const val ReadTimeoutMillis = 30_000
    }
}
