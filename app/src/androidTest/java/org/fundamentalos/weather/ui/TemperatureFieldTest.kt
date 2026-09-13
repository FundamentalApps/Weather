package org.fundamentalos.weather.ui

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.fundamentalos.weather.ui.components.TemperatureField
import org.fundamentalos.weather.weather.provider.fos.FosMapLegendStop
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
class TemperatureFieldTest {
    private val radius = 6

    /** The legend's own colours as the output palette, so a band reads back as its colour. */
    private fun List<FosMapLegendStop>.asPalette() = map { it.value to Color.parseColor("#" + it.color.removePrefix("#")) }
    private val gutter = 3 * radius

    private fun png(width: Int, height: Int, color: (x: Int, y: Int) -> Int): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (y in 0 until height) for (x in 0 until width) bitmap.setPixel(x, y, color(x, y))
        val bytes = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return bytes.toByteArray()
    }

    @Test fun uniformTemperatureRemainsVisibleWithoutReferenceRequest() {
        val legend = listOf(FosMapLegendStop(20f, "#00FF00"))
        val field = TemperatureField(legend, legend.asPalette())
        val result = field.decode(png(256, 256) { _, _ -> Color.GREEN }.inputStream(), gutter, radius)
        assertNotNull("A valid chunk must not wait for an average reference", result)
        assertEquals(256 - gutter * 2, result!!.width)
        assertEquals(255, Color.alpha(result.getPixel(100, 100)))
        result.recycle()
    }

    @Test fun smoothsTemperaturesThroughIntermediateLegendColors() {
        val legend = listOf(FosMapLegendStop(-10f, "#0000FF"),
            FosMapLegendStop(0f, "#00FF00"), FosMapLegendStop(10f, "#FF0000"))
        val field = TemperatureField(legend, legend.asPalette())
        val bytes = png(256, 256) { x, _ -> if (x < 128) Color.BLUE else Color.RED }
        val result = field.decode(bytes.inputStream(), gutter, radius)!!
        val middle = result.getPixel(128 - gutter, 100)
        assertTrue("Intermediate temperature must be green, not a purple RGB mixture",
            Color.green(middle) > 180)
        assertEquals(255, Color.alpha(middle))
        result.recycle()
    }

    @Test fun missingDataStaysTransparent() {
        val legend = listOf(FosMapLegendStop(0f, "#0000FF"), FosMapLegendStop(20f, "#FF0000"))
        val field = TemperatureField(legend, legend.asPalette())
        val bytes = png(256, 256) { x, _ -> if (x < 128) Color.RED else Color.TRANSPARENT }
        val result = field.decode(bytes.inputStream(), gutter, radius)!!
        assertEquals(0, Color.alpha(result.getPixel(200 - gutter, 100)))
        assertEquals(255, Color.alpha(result.getPixel(60 - gutter, 100)))
        // Data next to a hole keeps its own value rather than blending towards cold.
        assertEquals(Color.RED and 0xFFFFFF, result.getPixel(126 - gutter, 100) and 0xFFFFFF)
        result.recycle()
    }

    @Test fun adjacentGutteredChunksMatchOneContinuousField() {
        val legend = listOf(FosMapLegendStop(0f, "#0000FF"), FosMapLegendStop(20f, "#FF0000"))
        val field = TemperatureField(legend, legend.asPalette())
        fun decode(start: Int, width: Int): Bitmap {
            val bytes = png(width, 256) { x, _ -> if (x + start < 256) Color.BLUE else Color.RED }
            return field.decode(bytes.inputStream(), gutter, radius)!!
        }
        val whole = decode(-gutter, 512 + gutter * 2)
        val left = decode(-gutter, 256 + gutter * 2)
        val right = decode(256 - gutter, 256 + gutter * 2)
        for (x in 0 until 256) {
            assertEquals(whole.getPixel(x, 100), left.getPixel(x, 100))
            assertEquals(whole.getPixel(256 + x, 100), right.getPixel(x, 100))
        }
        whole.recycle(); left.recycle(); right.recycle()
    }
}
