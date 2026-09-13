package org.fundamentalos.weather.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.core.graphics.toColorInt
import org.fundamentalos.weather.weather.provider.fos.FosMapLegendStop
import java.io.InputStream
import kotlin.math.roundToInt

/**
 * Turns a categorical temperature image into a smooth colour field.
 *
 * The source paints every model cell in one of the [legend]'s band colours, so each pixel is
 * read back as the middle of its band, the values are blurred over about one model cell, and
 * the result is coloured again along the [palette]: the app's own temperature colours, so the
 * map agrees with the forecast. Valid data always stays visible: the source's own coverage is
 * kept as the alpha channel, so missing data stays transparent rather than turning into cold air.
 */
class TemperatureField(legend: List<FosMapLegendStop>, palette: List<Pair<Float, Int>>) {
    private val stops = legend.sortedBy { it.value }
    private val colors = IntArray(stops.size) { ("#" + stops[it].color.removePrefix("#")).toColorInt() }
    private val degrees = FloatArray(stops.size) { stops[it].value }

    private val paletteStops = palette.sortedBy { it.first }
    private val paletteDegrees = FloatArray(paletteStops.size) { paletteStops[it].first }
    private val paletteColors = IntArray(paletteStops.size) { paletteStops[it].second }

    /** The palette sampled every [LutStep] degrees, so colouring a pixel is one lookup. */
    private val lut: IntArray
    private val lutMin: Float = paletteDegrees.first()

    init {
        require(stops.isNotEmpty())
        require(paletteStops.isNotEmpty())
        val count = (((paletteDegrees.last() - lutMin) / LutStep).roundToInt() + 1).coerceAtLeast(1)
        lut = IntArray(count) { colorAt(lutMin + it * LutStep) }
    }

    /**
     * Decodes [stream], a source image [gutter] pixels wider on every side than the area wanted,
     * smooths it with three box passes of [radius] (a Gaussian of about 1.1 × [radius]), and
     * returns the inner area. The gutter lets neighbouring chunks blur over the same pixels, so
     * two of them meet without a seam as long as [gutter] covers the blur's reach, 3 × [radius].
     */
    fun decode(stream: InputStream, gutter: Int = 0, radius: Int = 0): Bitmap? {
        val source = BitmapFactory.decodeStream(stream) ?: return null
        val w = source.width
        val h = source.height
        if (w <= gutter * 2 || h <= gutter * 2) { source.recycle(); return null }
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)
        source.recycle()

        var values = FloatArray(pixels.size)
        var weights = FloatArray(pixels.size)
        // A source has a few dozen colours at most and neighbouring pixels usually share one, so
        // the translation is a small flat table with the last hit checked first. A HashMap here
        // would box a key per pixel, which is most of the cost of a million-pixel chunk.
        var knownColors = IntArray(64)
        var knownValues = FloatArray(64)
        var known = 0
        var lastColor = -1
        var lastValue = 0f
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val weight = Color.alpha(pixel) / 255f
            weights[i] = weight
            if (weight == 0f) continue
            val rgb = pixel and 0xFFFFFF
            if (rgb != lastColor) {
                var found = -1
                for (k in 0 until known) if (knownColors[k] == rgb) { found = k; break }
                if (found < 0) {
                    if (known == knownColors.size) {
                        knownColors = knownColors.copyOf(known * 2)
                        knownValues = knownValues.copyOf(known * 2)
                    }
                    knownColors[known] = rgb
                    knownValues[known] = degrees[nearestBand(rgb)]
                    found = known++
                }
                lastColor = rgb
                lastValue = knownValues[found]
            }
            values[i] = lastValue * weight
        }
        val coverage = weights
        if (radius > 0) {
            val scratch = FloatArray(pixels.size)
            values = boxBlur(values, scratch, w, h, radius)
            weights = boxBlur(weights.copyOf(), scratch, w, h, radius)
        }

        val outW = w - gutter * 2
        val outH = h - gutter * 2
        val output = IntArray(outW * outH)
        for (y in 0 until outH) {
            val row = (y + gutter) * w + gutter
            val outRow = y * outW
            for (x in 0 until outW) {
                val i = row + x
                // Preserve the coverage mask; smoothing must not fabricate missing observations.
                if (coverage[i] > 0f && weights[i] > 0f) {
                    output[outRow + x] = (colorFor(values[i] / weights[i]) and 0xFFFFFF) or
                        ((coverage[i] * 255).roundToInt() shl 24)
                }
            }
        }
        return Bitmap.createBitmap(output, outW, outH, Bitmap.Config.ARGB_8888)
    }

    /**
     * Three passes of a running-sum box filter in each direction. That is within a few percent of
     * a Gaussian, and its cost does not grow with the radius, which matters for a chunk of a
     * million pixels on a phone. Edges are clamped. The result lands in [input]; [scratch] is
     * the same size and is clobbered.
     */
    private fun boxBlur(input: FloatArray, scratch: FloatArray, w: Int, h: Int, radius: Int): FloatArray {
        var src = input
        var dst = scratch
        repeat(3) {
            boxPass(src, dst, w, h, radius, horizontal = true)
            boxPass(dst, src, w, h, radius, horizontal = false)
        }
        return src
    }

    private fun boxPass(src: FloatArray, dst: FloatArray, w: Int, h: Int, radius: Int, horizontal: Boolean) {
        val length = if (horizontal) w else h
        val lines = if (horizontal) h else w
        val stride = if (horizontal) 1 else w
        val norm = 1f / (radius * 2 + 1)
        for (line in 0 until lines) {
            val start = if (horizontal) line * w else line
            // Seed the window with the clamped left edge, then slide it along the line.
            var sum = src[start] * (radius + 1)
            for (k in 1..radius) sum += src[start + minOf(k, length - 1) * stride]
            for (i in 0 until length) {
                dst[start + i * stride] = sum * norm
                val enter = minOf(i + radius + 1, length - 1)
                val leave = maxOf(i - radius, 0)
                sum += src[start + enter * stride] - src[start + leave * stride]
            }
        }
    }

    /** The legend's colour for a temperature, opaque. */
    fun colorFor(value: Float): Int {
        val index = ((value - lutMin) / LutStep).roundToInt().coerceIn(0, lut.size - 1)
        return lut[index] or (0xFF shl 24)
    }

    private fun colorAt(value: Float): Int {
        if (value <= paletteDegrees.first()) return paletteColors.first() and 0xFFFFFF
        for (i in 1 until paletteDegrees.size) if (value <= paletteDegrees[i]) {
            val t = ((value - paletteDegrees[i - 1]) / (paletteDegrees[i] - paletteDegrees[i - 1])).coerceIn(0f, 1f)
            fun channel(shift: Int): Int {
                val a = (paletteColors[i - 1] shr shift) and 255
                val b = (paletteColors[i] shr shift) and 255
                return (a + (b - a) * t).roundToInt()
            }
            return (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
        }
        return paletteColors.last() and 0xFFFFFF
    }

    private fun nearestBand(pixel: Int): Int = colors.indices.minBy { index ->
        val dr = Color.red(pixel) - Color.red(colors[index])
        val dg = Color.green(pixel) - Color.green(colors[index])
        val db = Color.blue(pixel) - Color.blue(colors[index])
        dr * dr + dg * dg + db * db
    }

    companion object {
        private const val LutStep = 0.1f
    }
}
