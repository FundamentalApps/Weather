package org.fundamentalos.weather.ui.map

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.SystemClock
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

    private val waterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = if (dark) Color.argb(96, 14, 16, 22) else Color.argb(92, 255, 255, 255)
    }
    private val roadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = ink
        alpha = if (dark) 66 else 58
    }
    private val boundaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        color = ink
        alpha = if (dark) 120 else 130
    }
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

    private val viewport = RectL()
    private val tileRect = RectF()
    private val placed = ArrayList<RectF>()
    private val candidates = ArrayList<Candidate>()

    private class Candidate(val label: InkLabel, val x: Float, val y: Float, val priority: Int)

    /** Screen rectangles other overlays own, which no name may run into. */
    var reserved: List<RectF> = emptyList()

    private val arrived: () -> Unit = { map.postInvalidate() }

    init {
        store.addListener(arrived)
    }

    override fun onDetach(mapView: MapView) {
        store.removeListener(arrived)
        store.trim()
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
        val tilePx = TileSystem.MapSize(zoom) / count
        projection.getMercatorViewPort(viewport)
        val marginX = (viewport.right - viewport.left) / 4
        val marginY = (viewport.bottom - viewport.top) / 4
        val firstX = floor((viewport.left - marginX) / tilePx).toInt().coerceIn(0, count - 1)
        val lastX = floor((viewport.right + marginX) / tilePx).toInt().coerceIn(0, count - 1)
        val firstY = floor((viewport.top - marginY) / tilePx).toInt().coerceIn(0, count - 1)
        val lastY = floor((viewport.bottom + marginY) / tilePx).toInt().coerceIn(0, count - 1)

        candidates.clear()
        for (y in firstY..lastY) for (x in firstX..lastX) {
            val key = InkTileKey(inkZoom, x, y)
            val tile = store.peek(key)
            if (tile == null) {
                if (settled) store.request(key)
                drawStandIn(canvas, projection, key)
                continue
            }
            drawTile(canvas, projection, key, tile, inkZoom, clipTo = null)
        }
        drawLabels(canvas)
    }

    /**
     * While a tile loads, the tile above it — which is what was on screen before a zoom in — or
     * the tiles below it — what was there before a zoom out — keep the map from blinking.
     */
    private fun drawStandIn(canvas: Canvas, projection: Projection, key: InkTileKey) {
        if (key.zoom > 0) {
            val parent = InkTileKey(key.zoom - 1, key.x shr 1, key.y shr 1)
            store.peek(parent)?.let {
                val clip = RectF()
                tileRect(projection, key, clip)
                drawTile(canvas, projection, parent, it, key.zoom, clipTo = clip)
                return
            }
        }
        if (key.zoom < MaxInkZoom) {
            for (dy in 0..1) for (dx in 0..1) {
                val child = InkTileKey(key.zoom + 1, key.x * 2 + dx, key.y * 2 + dy)
                store.peek(child)?.let { drawTile(canvas, projection, child, it, key.zoom, clipTo = null) }
            }
        }
    }

    /**
     * Draws one tile's water, borders and roads by mapping the canvas onto tile coordinates, so
     * the paths are drawn as built; stroke widths are divided by the same scale to stay in
     * pixels. Labels are only collected here and placed once every tile has had its say.
     */
    private fun drawTile(
        canvas: Canvas, projection: Projection, key: InkTileKey, tile: InkTile, styleZoom: Int, clipTo: RectF?,
    ) {
        tileRect(projection, key, tileRect)
        val scale = tileRect.width() / tile.extent
        canvas.save()
        if (clipTo != null) canvas.clipRect(clipTo)
        canvas.translate(tileRect.left, tileRect.top)
        canvas.scale(scale, scale)
        // Tiles carry a margin of their neighbours' geometry; without the clip it is drawn twice.
        canvas.clipRect(0f, 0f, tile.extent.toFloat(), tile.extent.toFloat())
        tile.water?.let { canvas.drawPath(it, waterPaint) }
        for ((level, path) in tile.boundaries) {
            boundaryPaint.strokeWidth = (if (level == 2) 1.2f else 0.8f) * density / scale
            canvas.drawPath(path, boundaryPaint)
        }
        for (roadClass in RoadClass.entries) {
            if (styleZoom < roadClass.minZoom) continue
            val path = tile.roads[roadClass] ?: continue
            roadPaint.strokeWidth = roadClass.widthDp * density / scale
            canvas.drawPath(path, roadPaint)
        }
        canvas.restore()

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
        for (candidate in candidates) {
            val label = candidate.label
            val text = label.text(nameKeys) ?: continue
            style(label)
            val width = textPaint.measureText(text)
            val height = textPaint.textSize
            val pad = 4f * density
            val rect = RectF(
                candidate.x - width / 2 - pad, candidate.y - height / 2 - pad,
                candidate.x + width / 2 + pad, candidate.y + height / 2 + pad,
            )
            if (placed.any { RectF.intersects(it, rect) }) continue
            placed += rect
            val baseline = candidate.y + height * 0.36f
            canvas.drawText(text, candidate.x, baseline, haloPaint)
            canvas.drawText(text, candidate.x, baseline, textPaint)
        }
    }

    private fun style(label: InkLabel) {
        val sizeDp: Float
        val bold: Boolean
        var spacing = 0f
        var alpha = 255
        when {
            label.adminLevel == 2 -> { sizeDp = 15f; bold = true; spacing = 0.12f; alpha = 230 }
            label.adminLevel == 4 -> { sizeDp = 12f; bold = true; spacing = 0.08f; alpha = 205 }
            label.kind == "capital" || label.kind == "state_capital" -> { sizeDp = 15f; bold = true }
            label.size >= BigCity -> { sizeDp = 14f; bold = true }
            else -> { sizeDp = 12.5f; bold = false; alpha = 240 }
        }
        val size = sizeDp * density
        textPaint.textSize = size
        textPaint.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        textPaint.letterSpacing = spacing
        textPaint.color = ink
        textPaint.alpha = alpha
        haloPaint.textSize = size
        haloPaint.typeface = textPaint.typeface
        haloPaint.letterSpacing = spacing
        haloPaint.strokeWidth = size * 0.22f
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

        /** How long the zoom must hold still before tiles are asked for. */
        const val SettleMillis = 200L

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
         * The smallest city named at a zoom, by population. Districts and counties are cities
         * too in the data, so a regional view would otherwise fill with them; they wait for
         * the zoom at which a prefecture has room to show its parts.
         */
        fun leastCityPopulation(zoom: Int): Double = when {
            zoom <= 7 -> 1_000_000.0
            zoom == 8 -> 500_000.0
            zoom == 9 -> 200_000.0
            else -> 0.0
        }

        /**
         * How important a label is at this zoom, or null if it is not shown here: countries go
         * once the map is close enough for their regions, and regions once cities fill the view.
         */
        fun priorityOf(label: InkLabel, zoom: Int): Int? = when {
            label.adminLevel == 2 -> if (zoom in 2..7 && label.size >= leastCountryArea(zoom)) 100 else null
            label.adminLevel == 4 -> if (zoom in 4..9) 85 else null
            label.adminLevel != 0 -> null
            label.kind == "capital" -> if (zoom >= 3) 95 else null
            label.kind == "state_capital" -> if (zoom >= 4) 92 else null
            label.kind == "city" -> if (zoom >= 4 && label.size >= leastCityPopulation(zoom)) 90 else null
            else -> null
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
