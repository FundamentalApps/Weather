package org.fundamentalos.weather.ui.componets

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.util.fastForEach
import org.fundamentalos.weather.ui.componets.MultilayerIcon.ColorTag
import org.fundamentalos.weather.ui.theme.cloud
import org.fundamentalos.weather.ui.theme.cloudDark
import org.fundamentalos.weather.ui.theme.dust
import org.fundamentalos.weather.ui.theme.dustDark
import org.fundamentalos.weather.ui.theme.harmonized
import org.fundamentalos.weather.ui.theme.hot
import org.fundamentalos.weather.ui.theme.hotDark
import org.fundamentalos.weather.ui.theme.moon
import org.fundamentalos.weather.ui.theme.moonDark
import org.fundamentalos.weather.ui.theme.rain
import org.fundamentalos.weather.ui.theme.rainDark
import org.fundamentalos.weather.ui.theme.sun
import org.fundamentalos.weather.ui.theme.sunDark

fun multilayerIcon(block: MultiLayerIconBuilderScope.() -> Unit): MultilayerIcon {
    val result = BuilderImpl().apply(block).build()
    return result
}

interface MultiLayerIconBuilderScope {
    fun layer(resId: Int, color: Color = Color.Unspecified)
    fun layerHarmonize(resId: Int, color: Color)
    fun layerTag(resId: Int, color: ColorTag)
    fun layer(resId: Int, computeColor: @Composable () -> Color)
}

@Composable
fun MultilayerIcon(icon: MultilayerIcon, contentDescription: String?, modifier: Modifier = Modifier) {
    Box(modifier) {
        icon.layers.fastForEach {
            val color = when(it.color) {
                is MultilayerIcon.IconColor.Normal -> it.color.color
                is MultilayerIcon.IconColor.Tag -> getColorByTag(it.color.tag).harmonized()
                is MultilayerIcon.IconColor.Harmonize -> it.color.color.harmonized()
                is MultilayerIcon.IconColor.Compute -> it.color.f()
            }

            Icon(painterResource(it.resId), tint = color, contentDescription = contentDescription)
        }
    }
}

@Composable
private fun getColorByTag(tag: ColorTag): Color {
    // Icons sit on weather-tinted cards whose brightness is independent of the system theme.
    return if (!LocalUseDarkCards.current)
    when (tag) {
        ColorTag.SUN -> sun
        ColorTag.MOON -> moon
        ColorTag.RAIN -> rain
        ColorTag.CLOUD -> cloud
        ColorTag.DUST -> dust
        ColorTag.HOT -> hot
    } else when (tag) {
        ColorTag.SUN -> sunDark
        ColorTag.MOON -> moonDark
        ColorTag.RAIN -> rainDark
        ColorTag.CLOUD -> cloudDark
        ColorTag.DUST -> dustDark
        ColorTag.HOT -> hotDark
    }
}

private class BuilderImpl: MultiLayerIconBuilderScope {
    private val layers  = mutableListOf<MultilayerIcon.Layer>()

    override fun layer(resId: Int, color: Color) {
        layers.add(MultilayerIcon.Layer(resId, MultilayerIcon.IconColor.Normal(color)))
    }

    override fun layerTag(resId: Int, color: ColorTag) {
        layers.add(MultilayerIcon.Layer(resId, MultilayerIcon.IconColor.Tag(color)))
    }

    override fun layer(
        resId: Int,
        block: @Composable (() -> Color)
    ) {
        layers.add(MultilayerIcon.Layer(resId, MultilayerIcon.IconColor.Compute(block)))
    }

    override fun layerHarmonize(resId: Int, color: Color) {
        layers.add(MultilayerIcon.Layer(resId, MultilayerIcon.IconColor.Harmonize(color)))
    }

    fun build(): MultilayerIcon {
        return MultilayerIcon(layers)
    }
}

class MultilayerIcon(
    internal val layers: List<Layer>
) {
    class Layer(
        val resId: Int,
        val color: IconColor
    )

    sealed class IconColor {
        data class Normal(val color: Color) : IconColor()
        data class Harmonize(val color: Color): IconColor()
        data class Tag(val tag: ColorTag) : IconColor()
        data class Compute(val f: @Composable () -> Color) : IconColor()
    }

    enum class ColorTag {
        SUN, MOON, RAIN, CLOUD, DUST, HOT
    }
}