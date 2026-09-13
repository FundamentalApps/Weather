package org.fundamentalos.weather.ui.map

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.graphics.RectF
import android.graphics.SweepGradient
import android.graphics.Typeface
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay
import kotlin.math.cos
import kotlin.math.sin

/**
 * The place the home screen is showing, marked the way Apple Weather marks it: a dot on the
 * spot, a white bubble above it with the temperature now and today's low and high, and around
 * the bubble a ring that runs the legend's colours from the low to the high, with a marker at
 * where now sits between them. Under the dot, what the place is.
 */
class LocationBubbleOverlay(
    private val map: MapView,
    private val colorFor: (Float) -> Int,
) : Overlay() {
    private val density = map.resources.displayMetrics.density

    var point: GeoPoint? = null
    var tempCelsius: Int? = null
    var minCelsius: Int? = null
    var maxCelsius: Int? = null
    var caption: String = ""
    var placeName: String = ""

    private val screen = Point()
    private val ringRect = RectF()
    private val tail = Path()

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.argb(28, 0, 0, 0)
    }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val marker = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val markerEdge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val bigText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        color = Color.rgb(20, 20, 24)
    }
    private val smallText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val captionText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = Color.WHITE
    }
    private val captionHalo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        color = Color.argb(150, 40, 24, 16)
    }

    /** The screen rectangle the bubble and its captions cover, for the map's labels to avoid. */
    val footprint = RectF()

    override fun draw(canvas: Canvas, projection: Projection) {
        val point = point ?: run { footprint.setEmpty(); return }
        projection.toPixels(point, screen)
        val x = screen.x.toFloat()
        val y = screen.y.toFloat()
        val dp = density

        // The dot.
        canvas.drawCircle(x, y, 6.5f * dp, edge.apply { strokeWidth = 2f * dp })
        canvas.drawCircle(x, y, 6f * dp, fill)

        // The bubble, its tail reaching down to just above the dot.
        val radius = 34f * dp
        val cx = x
        val cy = y - 16f * dp - radius - 10f * dp
        tail.reset()
        tail.moveTo(cx - 12f * dp, cy + radius * 0.82f)
        tail.lineTo(cx, y - 14f * dp)
        tail.lineTo(cx + 12f * dp, cy + radius * 0.82f)
        tail.close()
        canvas.drawPath(tail, fill)
        canvas.drawCircle(cx, cy, radius, fill)

        val temp = tempCelsius
        val low = minCelsius
        val high = maxCelsius
        if (temp != null) {
            bigText.textSize = 26f * dp
            canvas.drawText("$temp°", cx + 2f * dp, cy + 4f * dp, bigText)
        }
        if (low != null && high != null) {
            smallText.textSize = 13f * dp
            smallText.color = colorFor(low.toFloat())
            canvas.drawText(low.toString(), cx - 11f * dp, cy + 21f * dp, smallText)
            smallText.color = colorFor(high.toFloat())
            canvas.drawText(high.toString(), cx + 11f * dp, cy + 21f * dp, smallText)

            // The ring: the legend's colours from the low to the high, leaving the bottom open
            // where the tail is, and a marker at now.
            val ringRadius = radius + 7f * dp
            ringRect.set(cx - ringRadius, cy - ringRadius, cx + ringRadius, cy + ringRadius)
            val start = 135f
            val sweep = 270f
            val steps = 24
            val colors = IntArray(steps + 1) { colorFor(low + (high - low) * it / steps.toFloat()) }
            // The gradient runs 0..sweep from 3 o'clock and is turned to the arc's start; a
            // gradient laid out past a full turn would wrap and paint the arc's end in the
            // colour of its beginning.
            val positions = FloatArray(steps + 1) { sweep / 360f * it / steps }
            ring.shader = SweepGradient(cx, cy, colors, positions).apply {
                setLocalMatrix(Matrix().apply { setRotate(start, cx, cy) })
            }
            ring.strokeWidth = 5f * dp
            canvas.drawArc(ringRect, start, sweep, false, ring)
            ring.shader = null
            if (temp != null && high > low) {
                val at = ((temp - low).toFloat() / (high - low)).coerceIn(0f, 1f)
                val angle = Math.toRadians((start + sweep * at).toDouble())
                val mx = cx + ringRadius * cos(angle).toFloat()
                val my = cy + ringRadius * sin(angle).toFloat()
                markerEdge.color = colorFor(temp.toFloat())
                markerEdge.strokeWidth = 2f * dp
                canvas.drawCircle(mx, my, 4.5f * dp, marker)
                canvas.drawCircle(mx, my, 4.5f * dp, markerEdge)
            }
        }

        // What this place is, under the dot.
        var bottom = y + 8f * dp
        if (caption.isNotEmpty()) {
            captionText.textSize = 12f * dp
            captionText.typeface = Typeface.DEFAULT
            captionHalo.textSize = captionText.textSize
            captionHalo.typeface = captionText.typeface
            captionHalo.strokeWidth = captionText.textSize * 0.2f
            bottom += captionText.textSize
            canvas.drawText(caption, x, bottom, captionHalo)
            canvas.drawText(caption, x, bottom, captionText)
        }
        if (placeName.isNotEmpty()) {
            captionText.textSize = 15f * dp
            captionText.typeface = Typeface.DEFAULT_BOLD
            captionHalo.textSize = captionText.textSize
            captionHalo.typeface = captionText.typeface
            captionHalo.strokeWidth = captionText.textSize * 0.2f
            bottom += captionText.textSize + 2f * dp
            canvas.drawText(placeName, x, bottom, captionHalo)
            canvas.drawText(placeName, x, bottom, captionText)
        }
        footprint.set(cx - radius - 12f * dp, cy - radius - 12f * dp, cx + radius + 12f * dp, bottom + 6f * dp)
    }
}
