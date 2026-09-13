package org.fundamentalos.weather.ui.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Region
import android.graphics.Typeface
import android.os.Build
import android.os.SystemClock
import android.util.LruCache
import org.osmdroid.util.RectL
import org.osmdroid.util.TileSystem
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay
import java.util.Locale
import kotlin.math.floor

/**
 * The map itself, drawn above the weather field from OpenStreetMap's vector tiles: water, roads,
 * borders, and the names of districts, cities, regions and countries, the way a vector map keeps
 * its ink above weather. Lines are stroked and names are set in the system's own fonts, so they
 * are sharp at any zoom and in the app's language, and no raster tile is drawn at all.
 */
class MapInkOverlay(
    private val map: MapView,
    private val store: InkTileStore,
    dark: Boolean,
    locale: Locale,
) : Overlay() {
    private val density = map.resources.displayMetrics.density
    private val nameKeys = nameKeysFor(locale)

    private val ink = if (dark) Color.rgb(255, 255, 255) else Color.rgb(46, 30, 26)
    private val halo = if (dark) Color.rgb(24, 26, 32) else Color.rgb(255, 252, 248)

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = ink
    }
    private val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        color = halo
        alpha = 215
    }

    private val artPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val waterPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
        color = if (dark) Color.argb(96, 14, 16, 22) else Color.argb(92, 255, 255, 255)
    }
    private val linePaint = Paint(Paint.FILTER_BITMAP_FLAG).apply { color = ink }
    private val art = object : LruCache<String, Bitmap>(ArtBudgetBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    private val viewport = RectL()
    private val tileRect = RectF()
    private val placed = ArrayList<RectF>()
    private val placedNames = HashSet<String>()
    private val candidates = ArrayList<Candidate>()

    private class Candidate(val label: InkLabel, val x: Float, val y: Float, val priority: Int)

    /** Screen rectangles other overlays own, which no name may run into. */
    var reserved: List<RectF> = emptyList()

    private val arrived: () -> Unit = { map.postInvalidate() }

    init {
        store.style = InkStyle(density, TileSystem.getTileSize() * 3 / 2)
        store.addListener(arrived)
    }

    override fun onDetach(mapView: MapView) {
        store.removeListener(arrived)
        super.onDetach(mapView)
    }

    private var lastZoom = Double.NaN
    private var zoomChangedAt = 0L

    override fun draw(canvas: Canvas, projection: Projection) {
        val zoom = projection.zoomLevel
        // A pinch or a zoom animation passes through every level on its way; asking for each
        // one's tiles would fetch whole screens nobody sees. Tiles are only asked for once the
        // zoom has held still, and a frame is scheduled to check back when it has.
        val now = SystemClock.uptimeMillis()
        if (zoom != lastZoom) {
            lastZoom = zoom
            zoomChangedAt = now
        }
        val settled = now - zoomChangedAt >= SettleMillis
        if (!settled) map.postInvalidateDelayed(SettleMillis)
        val inkZoom = floor(zoom).toInt().coerceIn(0, MaxInkZoom)
        val count = 1 shl inkZoom
        val mapSize = TileSystem.MapSize(zoom)
        val tilePx = mapSize / count
        projection.getMercatorViewPort(viewport)
        val marginX = (viewport.right - viewport.left) * Margin
        val marginY = (viewport.bottom - viewport.top) * Margin
        val firstX = floor((viewport.left - marginX) / tilePx).toInt().coerceIn(0, count - 1)
        val lastX = floor((viewport.right + marginX) / tilePx).toInt().coerceIn(0, count - 1)
        val firstY = floor((viewport.top - marginY) / tilePx).toInt().coerceIn(0, count - 1)
        val lastY = floor((viewport.bottom + marginY) / tilePx).toInt().coerceIn(0, count - 1)

        candidates.clear()
        var complete = true
        for (y in firstY..lastY) for (x in firstX..lastX) {
            val key = InkTileKey(inkZoom, x, y)
            val tile = store.peek(key)
            if (tile != null && tile.isSet) {
                drawTile(canvas, projection, key, tile, inkZoom, clipTo = null)
                continue
            }
            complete = false
            // Not set: once the zoom holds still, ask for the tile, or have a baked one set.
            // Not before — a zoom passes through every level on its way, and setting each
            // one's tiles would keep the level it stops at waiting.
            if (settled) { if (tile == null) store.request(key) else store.set(key) }
            drawStandIn(canvas, projection, key)
            // A baked tile has its names already, even without its ink.
            if (tile != null) drawTile(canvas, projection, key, tile, inkZoom, clipTo = null)
        }
        drawLabels(canvas)
        if (complete && settled) bakeAhead(inkZoom, mapSize)
    }

    /**
     * With the view served, bakes the way out: the next coarser levels, each over the screen
     * as it would be at that zoom — a zoom out sees far more than the tiles above this view —
     * and the world itself. Fetched and decoded, in the background, behind anything a pan or a
     * zoom asks for meanwhile, so a zoom out finds its map in memory; the nearest level is set
     * as well, so that a zoom out of one level finds its ink ready.
     */
    private fun bakeAhead(inkZoom: Int, mapSize: Double) {
        val centreX = (viewport.left + viewport.right) / 2.0 / mapSize
        val centreY = (viewport.top + viewport.bottom) / 2.0 / mapSize
        // At any whole zoom a tile is the tile size on screen, so the screen's reach in tiles
        // is the same at every level.
        val tilePx = TileSystem.getTileSize().toDouble()
        val reachX = (viewport.right - viewport.left) * (0.5 + Margin) / tilePx
        val reachY = (viewport.bottom - viewport.top) * (0.5 + Margin) / tilePx
        for (coarser in inkZoom - 1 downTo maxOf(inkZoom - BakedLevels, BakedZoom)) {
            val count = 1 shl coarser
            val firstX = floor(centreX * count - reachX).toInt().coerceIn(0, count - 1)
            val lastX = floor(centreX * count + reachX).toInt().coerceIn(0, count - 1)
            val firstY = floor(centreY * count - reachY).toInt().coerceIn(0, count - 1)
            val lastY = floor(centreY * count + reachY).toInt().coerceIn(0, count - 1)
            for (y in firstY..lastY) for (x in firstX..lastX) {
                val key = InkTileKey(coarser, x, y)
                if (!store.has(key)) store.request(key, background = true)
                else if (coarser == inkZoom - 1) store.set(key, background = true)
            }
        }
        for (z in 0 until BakedZoom) for (y in 0 until (1 shl z)) for (x in 0 until (1 shl z)) {
            val key = InkTileKey(z, x, y)
            if (!store.has(key)) store.request(key, background = true)
        }
    }

    /**
     * While a tile loads, the tile above it — which is what was on screen before a zoom in — or
     * the tiles below it — what was there before a zoom out — keep the map from blinking.
     */
    private val below = ArrayList<Pair<InkTileKey, InkTile>>()

    /**
     * While a tile is not set, what is: the set tiles below it — what was on screen before a
     * zoom out, drawn smaller, so still sharp — and over the rest of its square the nearest set
     * tile above it — what was there before a zoom in — stretched. A tile that is only baked
     * has no ink to stand in with, and is passed over for one that has.
     */
    private fun drawStandIn(canvas: Canvas, projection: Projection, key: InkTileKey) {
        below.clear()
        collectSetBelow(key, StandInDepth, below)
        nearestSetAbove(key)?.let { (aboveKey, above) ->
            val clip = RectF()
            tileRect(projection, key, clip)
            canvas.save()
            canvas.clipRect(clip)
            for ((childKey, _) in below) {
                tileRect(projection, childKey, tileRect)
                canvas.clipOut(tileRect)
            }
            drawTile(canvas, projection, aboveKey, above, key.zoom, clipTo = clip)
            canvas.restore()
        }
        for ((childKey, child) in below) drawTile(canvas, projection, childKey, child, key.zoom, clipTo = null)
    }

    /** The set tiles under [key], down to [depth] levels, where a set one is not found sooner. */
    private fun collectSetBelow(key: InkTileKey, depth: Int, out: MutableList<Pair<InkTileKey, InkTile>>) {
        if (depth == 0 || key.zoom >= MaxInkZoom) return
        for (dy in 0..1) for (dx in 0..1) {
            val child = InkTileKey(key.zoom + 1, key.x * 2 + dx, key.y * 2 + dy)
            val tile = store.peek(child)
            if (tile != null && tile.isSet) out += child to tile else collectSetBelow(child, depth - 1, out)
        }
    }

    /** The nearest set tile over [key], up to [StandInReach] levels up. */
    private fun nearestSetAbove(key: InkTileKey): Pair<InkTileKey, InkTile>? {
        var above = key
        repeat(minOf(StandInReach, key.zoom)) {
            above = InkTileKey(above.zoom - 1, above.x shr 1, above.y shr 1)
            val tile = store.peek(above)
            if (tile != null && tile.isSet) return above to tile
        }
        return null
    }

    private fun Canvas.clipOut(rect: RectF) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) clipOutRect(rect)
        else @Suppress("DEPRECATION") clipRect(rect, Region.Op.DIFFERENCE)
    }

    /**
     * Blits one tile's set ink over its square. Labels are only collected here and placed once
     * every tile has had its say.
     */
    private fun drawTile(
        canvas: Canvas, projection: Projection, key: InkTileKey, tile: InkTile, styleZoom: Int, clipTo: RectF?,
    ) {
        tileRect(projection, key, tileRect)
        val scale = tileRect.width() / tile.extent
        tile.masks?.let { masks ->
            if (clipTo != null) {
                canvas.save()
                canvas.clipRect(clipTo)
            }
            masks.water?.let { canvas.drawBitmap(it, null, tileRect, waterPaint) }
            masks.lines?.let { canvas.drawBitmap(it, null, tileRect, linePaint) }
            if (clipTo != null) canvas.restore()
        }

        for (label in tile.labels) {
            val priority = priorityOf(label, styleZoom) ?: continue
            val x = tileRect.left + label.x * scale
            val y = tileRect.top + label.y * scale
            if (clipTo != null && !clipTo.contains(x, y)) continue
            candidates += Candidate(label, x, y, priority)
        }
    }

    /**
     * Sets the names in order of importance, each one only where it does not run into one
     * already set. A name that two tiles both carry lands on the same spot twice and so is set
     * once.
     */
    private fun drawLabels(canvas: Canvas) {
        candidates.sortWith(compareByDescending<Candidate> { it.priority }.thenByDescending { it.label.size })
        placed.clear()
        placed.addAll(reserved)
        placedNames.clear()
        for (candidate in candidates) {
            val label = candidate.label
            val text = label.text(nameKeys) ?: continue
            // A municipality is both a city and a region in the data; one name is enough.
            if (!placedNames.add(text)) continue
            val art = labelArt(text, styleOf(label))
            val halfWidth = art.width / 2f
            val halfHeight = art.height / 2f
            val rect = RectF(
                candidate.x - halfWidth, candidate.y - halfHeight,
                candidate.x + halfWidth, candidate.y + halfHeight,
            )
            if (placed.any { RectF.intersects(it, rect) }) continue
            placed += rect
            canvas.drawBitmap(art, rect.left, rect.top, artPaint)
        }
    }

    /** How a label is set: size in dp, weight, tracking and opacity. */
    private data class LabelStyle(val sizeDp: Float, val bold: Boolean, val spacing: Float, val alpha: Int)

    private fun styleOf(label: InkLabel): LabelStyle = when {
        label.adminLevel == 2 -> LabelStyle(15f, true, 0.12f, 230)
        label.adminLevel == 4 -> LabelStyle(12f, true, 0.08f, 205)
        label.kind == "capital" || label.kind == "state_capital" -> LabelStyle(15f, true, 0f, 255)
        label.size >= BigCity -> LabelStyle(14f, true, 0f, 255)
        else -> LabelStyle(12.5f, false, 0f, 240)
    }

    /**
     * A name set once, halo and all, into a small bitmap that is then blitted every frame. The
     * halo is stroked text, which the renderer treats as paths and re-tessellates each frame;
     * seventy of them on screen made a pan stutter, where seventy bitmaps do not.
     */
    private fun labelArt(text: String, style: LabelStyle): Bitmap {
        val key = "$text|${style.sizeDp}|${style.bold}|${style.spacing}|${style.alpha}"
        art.get(key)?.let { return it }
        val size = style.sizeDp * density
        val typeface = if (style.bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        textPaint.textSize = size
        textPaint.typeface = typeface
        textPaint.letterSpacing = style.spacing
        textPaint.color = ink
        textPaint.alpha = style.alpha
        haloPaint.textSize = size
        haloPaint.typeface = typeface
        haloPaint.letterSpacing = style.spacing
        haloPaint.strokeWidth = size * 0.22f
        val pad = 4f * density
        val width = (textPaint.measureText(text) + haloPaint.strokeWidth + pad * 2).toInt().coerceAtLeast(1)
        val height = (size * 1.3f + haloPaint.strokeWidth + pad * 2).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val baseline = height / 2f + size * 0.36f
        canvas.drawText(text, width / 2f, baseline, haloPaint)
        canvas.drawText(text, width / 2f, baseline, textPaint)
        art.put(key, bitmap)
        return bitmap
    }

    private fun tileRect(projection: Projection, key: InkTileKey, out: RectF) {
        val tileSystem = MapView.getTileSystem()
        val count = (1 shl key.zoom).toDouble()
        val west = tileSystem.getLongitudeFromX01(key.x / count, false)
        val east = tileSystem.getLongitudeFromX01((key.x + 1) / count, false)
        val north = tileSystem.getLatitudeFromY01(key.y / count, false)
        val south = tileSystem.getLatitudeFromY01((key.y + 1) / count, false)
        out.set(
            projection.getLongPixelXFromLongitude(west).toFloat(),
            projection.getLongPixelYFromLatitude(north).toFloat(),
            projection.getLongPixelXFromLongitude(east).toFloat(),
            projection.getLongPixelYFromLatitude(south).toFloat(),
        )
    }

    private companion object {
        /** A city this size is set larger, whatever its tag says. */
        const val BigCity = 1_000_000.0

        /** Room for a few hundred set names. */
        const val ArtBudgetBytes = 8 * 1024 * 1024

        /** How long the zoom must hold still before tiles are asked for. */
        const val SettleMillis = 200L

        /** How far past the screen's edges tiles are drawn, as a share of the screen. */
        const val Margin = 0.125

        /** How many coarser levels are baked ahead over the screen. */
        const val BakedLevels = 2

        /** Baking over the screen stops at this zoom; below it, the whole world is baked. */
        const val BakedZoom = 2

        /** How many levels down a stand-in is looked for: what was on screen before a zoom out. */
        const val StandInDepth = 2

        /** How many levels up a stand-in is looked for: eight times stretched is still a map. */
        const val StandInReach = 3

        /**
         * The smallest country, in square metres of mercator, named at a zoom: a world view
         * names the large ones and leaves the microstates for when they can be seen.
         */
        fun leastCountryArea(zoom: Int): Double = when {
            zoom <= 2 -> 3e11
            zoom == 3 -> 5e10
            zoom == 4 -> 5e9
            else -> 0.0
        }

        /**
         * How important a label is at this zoom, or null if it is not shown here. In the region
         * grain, countries, regions, capitals and cities of a million; in the city grain, every
         * district and city, which is what the data calls them both.
         */
        fun priorityOf(label: InkLabel, zoom: Int): Int? {
            val city = zoom >= CityZoom
            return when {
                label.adminLevel == 2 -> if (!city && zoom >= 2 && label.size >= leastCountryArea(zoom)) 100 else null
                label.adminLevel == 4 -> if (!city && zoom >= 4) 85 else null
                label.adminLevel != 0 -> null
                label.kind == "capital" -> if (zoom >= 3) 95 else null
                label.kind == "state_capital" -> if (zoom >= 4) 92 else null
                label.kind == "city" -> if (city || (zoom >= 4 && label.size >= BigCity)) 90 else null
                else -> null
            }
        }

        /** The name keys to try for a locale, most specific first, ending in the local name. */
        fun nameKeysFor(locale: Locale): List<String> {
            val language = locale.language.lowercase(Locale.ROOT)
            return when (language) {
                "zh" -> {
                    val traditional = locale.script == "Hant" || locale.country in setOf("TW", "HK", "MO")
                    if (traditional) listOf("name_zh-Hant", "name_zh", "name", "name_en")
                    else listOf("name_zh-Hans", "name_zh", "name", "name_en")
                }
                "en" -> listOf("name_en", "name")
                else -> listOf("name_$language", "name", "name_en")
            }
        }

        /** The first name the label has in the wanted languages. Some carry variants split by ';'. */
        fun InkLabel.text(keys: List<String>): String? {
            for (key in keys) {
                val value = names[key] ?: continue
                return value.substringBefore(';').trim().takeIf { it.isNotEmpty() } ?: continue
            }
            return null
        }
    }
}
