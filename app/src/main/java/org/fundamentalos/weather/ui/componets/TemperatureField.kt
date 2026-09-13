package org.fundamentalos.weather.ui.componets

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import org.fundamentalos.weather.weather.provider.fos.FosMapLegendStop
import java.io.InputStream
import kotlin.math.exp
import kotlin.math.roundToInt

/** Decode categorical source colours into temperatures, smooth values, then apply the legend.
 * Valid data always stays visible. WMS requests include a gutter so adjacent tiles filter the
 * same neighbourhood; missing source data remains transparent rather than becoming cold data.
 */
class TemperatureField(legend: List<FosMapLegendStop>, private val gutter: Int = 0) {
    private val stops = legend.sortedBy { it.value }
    private val colors = IntArray(stops.size) { Color.parseColor("#" + stops[it].color.removePrefix("#")) }
    private val degrees = FloatArray(stops.size) { stops[it].value }
    private val kernel = FloatArray(Radius * 2 + 1) { exp(-((it - Radius) * (it - Radius)) / 72f) }

    init { require(stops.isNotEmpty()) }

    fun decode(stream: InputStream): Bitmap? {
        val source = BitmapFactory.decodeStream(stream) ?: return null
        val w = source.width
        val h = source.height
        if (w <= gutter * 2 || h <= gutter * 2) { source.recycle(); return null }
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)
        source.recycle()
        val values = FloatArray(pixels.size)
        val weights = FloatArray(pixels.size)
        val translated = HashMap<Int, Float>()
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val weight = Color.alpha(pixel) / 255f
            weights[i] = weight
            if (weight > 0f) values[i] = translated.getOrPut(pixel and 0xFFFFFF) {
                degrees[nearestBand(pixel)]
            } * weight
        }
        fun blur(input: FloatArray, horizontal: Boolean): FloatArray {
            val output = FloatArray(input.size)
            for (y in 0 until h) for (x in 0 until w) {
                var sum = 0f
                for (k in -Radius..Radius) {
                    val sx = if (horizontal) (x + k).coerceIn(0, w - 1) else x
                    val sy = if (horizontal) y else (y + k).coerceIn(0, h - 1)
                    sum += input[sy * w + sx] * kernel[k + Radius]
                }
                output[y * w + x] = sum
            }
            return output
        }
        val smoothValues = blur(blur(values, true), false)
        val smoothWeights = blur(blur(weights, true), false)
        val outW = w - gutter * 2
        val outH = h - gutter * 2
        val output = IntArray(outW * outH)
        for (y in 0 until outH) for (x in 0 until outW) {
            val i = (y + gutter) * w + x + gutter
            // Preserve the coverage mask; smoothing must not fabricate missing observations.
            if (weights[i] > 0f && smoothWeights[i] > 0f) {
                output[y * outW + x] = colorAt(smoothValues[i] / smoothWeights[i]) or
                    ((weights[i] * 255).roundToInt() shl 24)
            }
        }
        return Bitmap.createBitmap(output, outW, outH, Bitmap.Config.ARGB_8888)
    }

    private fun colorAt(value: Float): Int {
        if (value <= degrees.first()) return colors.first() and 0xFFFFFF
        for (i in 1 until degrees.size) if (value <= degrees[i]) {
            val t = ((value - degrees[i - 1]) / (degrees[i] - degrees[i - 1])).coerceIn(0f, 1f)
            fun channel(shift: Int): Int {
                val a = (colors[i - 1] shr shift) and 255
                val b = (colors[i] shr shift) and 255
                return (a + (b - a) * t).roundToInt()
            }
            return (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
        }
        return colors.last() and 0xFFFFFF
    }

    private fun nearestBand(pixel: Int): Int = colors.indices.minBy { index ->
        val dr = Color.red(pixel) - Color.red(colors[index])
        val dg = Color.green(pixel) - Color.green(colors[index])
        val db = Color.blue(pixel) - Color.blue(colors[index])
        dr * dr + dg * dg + db * db
    }

    companion object {
        const val Radius = 16
    }
}
