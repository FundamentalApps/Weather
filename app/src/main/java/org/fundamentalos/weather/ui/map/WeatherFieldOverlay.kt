package org.fundamentalos.weather.ui.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import org.osmdroid.util.RectL
import org.osmdroid.util.TileSystem
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Draws a field layer from whole chunks instead of per-zoom tiles. A chunk is one bitmap for a
 * square of the world; the projection stretches it to wherever the map is, filtered, so a pan or
 * a zoom is a redraw and never a request. The chunk level follows the zoom so a chunk is always
 * a few screens wide, and when the level changes, the chunks of the level just left stand in
 * until the new ones have arrived and faded in.
 */
class WeatherFieldOverlay(
    private val map: MapView,
    private val source: FieldLayerSource,
    private val store: WeatherFieldStore,
) : Overlay() {
    private val paint = Paint().apply { isFilterBitmap = true }
    private val viewport = RectL()
    private val dst = RectF()
    private val clip = RectF()

    /** When each chunk was first drawn, for the fade; 0 means it was there from the start. */
    private val shownAt = HashMap<FieldChunkKey, Long>()
    private var drawnOnce = false

    private val arrived: () -> Unit = { map.postInvalidate() }

    init {
        store.addListener(arrived)
    }

    override fun onDetach(mapView: MapView) {
        store.removeListener(arrived)
        store.trim()
        super.onDetach(mapView)
    }

    override fun draw(canvas: Canvas, projection: Projection) {
        val zoom = projection.zoomLevel
        // A chunk is 2^(zoom - level) density-scaled tiles wide; this keeps it between 8 and 16.
        val level = (floor(zoom).toInt() - LevelOffset).coerceIn(0, MaxChunkLevel)
        val count = 1 shl level
        val chunkPx = TileSystem.MapSize(zoom) / count
        projection.getMercatorViewPort(viewport)
        // Ask for what is a half-screen away as well, so crossing into the next chunk is ready.
        val marginX = (viewport.right - viewport.left) / 2
        val marginY = (viewport.bottom - viewport.top) / 2
        val firstX = floor((viewport.left - marginX) / chunkPx).toInt().coerceIn(0, count - 1)
        val lastX = floor((viewport.right + marginX) / chunkPx).toInt().coerceIn(0, count - 1)
        val firstY = floor((viewport.top - marginY) / chunkPx).toInt().coerceIn(0, count - 1)
        val lastY = floor((viewport.bottom + marginY) / chunkPx).toInt().coerceIn(0, count - 1)

        val now = SystemClock.uptimeMillis()
        var animating = false
        for (y in firstY..lastY) for (x in firstX..lastX) {
            val key = FieldChunkKey(source.stamp, level, x, y)
            val bitmap = store.peek(key)
            if (bitmap == null) {
                store.request(key, source)
                drawStandIn(canvas, projection, key)
                continue
            }
            val since = shownAt.getOrPut(key) { if (drawnOnce) now else 0L }
            val fade = if (since == 0L) 1f else ((now - since).toFloat() / FadeMillis).coerceIn(0f, 1f)
            if (fade < 1f) {
                animating = true
                // Cross-fade over what was there rather than over a hole.
                drawStandIn(canvas, projection, key)
            }
            drawChunk(canvas, projection, bitmap, key, fade)
        }
        drawnOnce = true
        if (animating) map.postInvalidateOnAnimation()
    }

    /**
     * Covers the square of [key] with whatever is already in memory: the previous edition of the
     * same chunk, else the nearest ancestor from a coarser level, else the children from the
     * finer one. Nothing is drawn if none of those is there yet.
     */
    private fun drawStandIn(canvas: Canvas, projection: Projection, key: FieldChunkKey) {
        store.peekPrevious(source.layer.id, key)?.let {
            drawChunk(canvas, projection, it, key, 1f)
            return
        }
        for (level in key.level - 1 downTo 0) {
            val shift = key.level - level
            val ancestor = FieldChunkKey(key.stamp, level, key.x shr shift, key.y shr shift)
            val bitmap = store.peek(ancestor) ?: store.peekPrevious(source.layer.id, ancestor) ?: continue
            // The ancestor is larger than the square; clip so the layer's opacity is not doubled
            // where a neighbour is already drawn.
            chunkRect(projection, key, clip)
            canvas.save()
            canvas.clipRect(clip)
            drawChunk(canvas, projection, bitmap, ancestor, 1f)
            canvas.restore()
            return
        }
        if (key.level < MaxChunkLevel) {
            for (dy in 0..1) for (dx in 0..1) {
                val child = FieldChunkKey(key.stamp, key.level + 1, key.x * 2 + dx, key.y * 2 + dy)
                val bitmap = store.peek(child) ?: store.peekPrevious(source.layer.id, child) ?: continue
                drawChunk(canvas, projection, bitmap, child, 1f)
            }
        }
    }

    private fun chunkRect(projection: Projection, key: FieldChunkKey, out: RectF) {
        val tileSystem = MapView.getTileSystem()
        val count = key.count.toDouble()
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

    private fun drawChunk(canvas: Canvas, projection: Projection, bitmap: Bitmap, key: FieldChunkKey, fade: Float) {
        chunkRect(projection, key, dst)
        paint.alpha = (source.layer.opacity * fade * 255).roundToInt()
        canvas.drawBitmap(bitmap, null, dst, paint)
    }

    private companion object {
        const val FadeMillis = 300f

        /** Level = floor(zoom) - this, so a chunk spans 8 to 16 density-scaled tiles. */
        const val LevelOffset = 3
    }
}
