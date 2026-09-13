package org.fundamentalos.weather.ui.componets

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import org.fundamentalos.weather.R

/**
 * Map a warning `typeName` to one of the existing warning drawables. Keyword match is
 * substring-based; the order below matters — more specific keywords (e.g. 雷暴大风) must come
 * before looser ones (大风) so they win.
 */
@DrawableRes
fun warningDrawableFor(typeName: String?): Int? {
    if (typeName.isNullOrBlank()) return null
    return when {
        "台风" in typeName -> R.drawable.warning_typhoon_24dp
        "龙卷" in typeName -> R.drawable.warning_tornado_24dp
        "雷暴大风" in typeName -> R.drawable.warning_thunderstorm_gale_24dp
        "下击暴流" in typeName -> R.drawable.warning_downburst_24dp
        "雷电" in typeName -> R.drawable.warning_lightning_24dp
        "暴雨" in typeName || "强降雨" in typeName -> R.drawable.warning_rainstorm_24dp
        "暴雪" in typeName -> R.drawable.warning_snow_storm_24dp
        "寒潮" in typeName || "寒冷" in typeName || "低温" in typeName || "冰冻" in typeName ->
            R.drawable.warning_cold_wave_24dp
        "大风" in typeName -> R.drawable.warning_gale_24dp
        "高温" in typeName || "酷暑" in typeName -> R.drawable.warning_heat_wave_24dp
        "雪崩" in typeName -> R.drawable.warning_avalanche_24dp
        "冰雹" in typeName -> R.drawable.warning_hail_24dp
        "霜冻" in typeName -> R.drawable.warning_frost_24dp
        "大雾" in typeName || "浓雾" in typeName -> R.drawable.warning_heavy_fog_24dp
        "沙尘" in typeName -> R.drawable.warning_sandstorm_24dp
        else -> null
    }
}

@Composable
fun warningIconPainter(typeName: String?): Painter {
    val drawable = warningDrawableFor(typeName)
    return if (drawable != null) {
        painterResource(drawable)
    } else {
        rememberVectorPainter(Icons.Rounded.Warning)
    }
}
