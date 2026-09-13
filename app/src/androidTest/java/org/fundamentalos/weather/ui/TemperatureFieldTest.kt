package org.fundamentalos.weather.ui

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.fundamentalos.weather.ui.componets.TemperatureField
import org.fundamentalos.weather.weather.provider.fos.FosMapLegendStop
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
class TemperatureFieldTest {
    @Test fun uniformTemperatureRemainsVisibleWithoutReferenceRequest() {
        val source = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        source.eraseColor(Color.GREEN)
        val bytes = ByteArrayOutputStream().also { source.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
        val field = TemperatureField(listOf(FosMapLegendStop(20f, "#00FF00")))
        val result = field.decode(bytes.inputStream())
        assertNotNull("A valid tile must not wait for an average reference", result)
        assertEquals(255, Color.alpha(result!!.getPixel(128, 128)))
        source.recycle(); result.recycle()
    }

    @Test fun smoothsTemperaturesThroughIntermediateLegendColors() {
        val source = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        for (y in 0 until 256) for (x in 0 until 256)
            source.setPixel(x, y, if (x < 128) Color.BLUE else Color.RED)
        val field = TemperatureField(listOf(FosMapLegendStop(-10f, "#0000FF"),
            FosMapLegendStop(0f, "#00FF00"), FosMapLegendStop(10f, "#FF0000")))
        val bytes = ByteArrayOutputStream().also { source.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
        val result = field.decode(bytes.inputStream())!!
        assertTrue("Intermediate temperature must be green, not a purple RGB mixture",
            Color.green(result.getPixel(128, 128)) > 180)
        assertEquals(255, Color.alpha(result.getPixel(128, 128)))
        source.recycle(); result.recycle()
    }

    @Test fun adjacentGutteredTilesMatchOneContinuousField() {
        val field = TemperatureField(listOf(FosMapLegendStop(0f, "#0000FF"),
            FosMapLegendStop(20f, "#FF0000")), TemperatureField.Radius)
        fun decode(start: Int, width: Int): Bitmap {
            val bitmap = Bitmap.createBitmap(width, 288, Bitmap.Config.ARGB_8888)
            for (y in 0 until 288) for (x in 0 until width)
                bitmap.setPixel(x, y, if (x + start < 256) Color.BLUE else Color.RED)
            val bytes = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
            bitmap.recycle()
            return field.decode(bytes.inputStream())!!
        }
        val whole = decode(-16, 544)
        val left = decode(-16, 288)
        val right = decode(240, 288)
        for (x in 0 until 256) {
            assertEquals(whole.getPixel(x, 128), left.getPixel(x, 128))
            assertEquals(whole.getPixel(256 + x, 128), right.getPixel(x, 128))
        }
        whole.recycle(); left.recycle(); right.recycle()
    }
}
