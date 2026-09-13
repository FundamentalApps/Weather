package org.fundamentalos.weather.ui.map

import android.content.Context
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import org.osmdroid.tileprovider.MapTileProviderBase
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.TilesOverlay

/**
 * The base map's ink — its place names, road lines and label halos — drawn again above a field
 * layer so they stay readable through it, the way a vector map keeps its labels above weather.
 *
 * Raster tiles cannot be split into layers, so the ink is read out of them by lightness. The
 * [Ink.Marks] pass keeps what is darker than the map paper: text and road casings. The
 * [Ink.Halos] pass keeps what is brighter than it: the white halos around text and the white
 * minor roads. Each is recoloured for the theme. Both share the map's own tile provider, so
 * they cost no tile traffic, which is also why neither may detach that provider when it goes.
 */
class MapInkOverlay(
    provider: MapTileProviderBase,
    context: Context,
    ink: Ink,
    dark: Boolean,
) : TilesOverlay(provider, context) {
    enum class Ink { Marks, Halos }

    init {
        // A tile still loading draws nothing here; the paper under it is the map's business.
        loadingBackgroundColor = Color.TRANSPARENT
        loadingLineColor = Color.TRANSPARENT
        val color = when (ink) {
            Ink.Marks -> if (dark) LightInk else DarkInk
            Ink.Halos -> if (dark) DarkInk else LightInk
        }
        setColorFilter(ColorMatrixColorFilter(inkMatrix(ink, color)))
    }

    /** The provider belongs to the map; only the map may detach it. */
    override fun onDetach(pMapView: MapView) = Unit

    private companion object {
        val DarkInk = Color.rgb(28, 28, 34)
        val LightInk = Color.rgb(255, 255, 255)

        /** Lightness of the map paper; anything darker is a mark. */
        const val PaperLightness = 0.85f

        /** Lightness above which a pixel is a halo or a white road, above the paper's own. */
        const val HaloLightness = 0.96f

        /**
         * A matrix that paints every pixel in [color] with an alpha taken from its lightness:
         * marks fade in below [PaperLightness], halos above [HaloLightness]. The map paper
         * itself, in between, comes out fully transparent.
         */
        fun inkMatrix(ink: Ink, color: Int): ColorMatrix {
            val r = 0.299f
            val g = 0.587f
            val b = 0.114f
            val alphaRow = when (ink) {
                // alpha = 1 - L / paper
                Ink.Marks -> floatArrayOf(-r / PaperLightness, -g / PaperLightness, -b / PaperLightness, 0f, 255f)
                // alpha = (L - halo) / (1 - halo)
                Ink.Halos -> {
                    val gain = 1f / (1f - HaloLightness)
                    floatArrayOf(r * gain, g * gain, b * gain, 0f, -HaloLightness * gain * 255f)
                }
            }
            return ColorMatrix(
                floatArrayOf(
                    0f, 0f, 0f, 0f, Color.red(color).toFloat(),
                    0f, 0f, 0f, 0f, Color.green(color).toFloat(),
                    0f, 0f, 0f, 0f, Color.blue(color).toFloat(),
                    alphaRow[0], alphaRow[1], alphaRow[2], alphaRow[3], alphaRow[4],
                )
            )
        }
    }
}
