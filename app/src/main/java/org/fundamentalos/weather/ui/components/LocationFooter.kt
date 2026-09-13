package org.fundamentalos.weather.ui.components

import org.fundamentalos.weather.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.fundamentalos.weather.ui.theme.PreviewThemeWithBg
import java.util.Locale

/**
 * The full administrative chain of the place being shown, closing out the page the way Apple Weather
 * does. Street level is not available: the geocoding dataset stops at populated places.
 */
@Composable
fun LocationFooter(
    country: String?,
    province: String?,
    city: String?,
    place: String?,
    modifier: Modifier = Modifier,
) {
    val label = locationChain(country, province, city, place) ?: return
    // Same weight as the headline temperature, not the dimmed treatment the cards use for labels.
    val onSurface = MaterialTheme.colorScheme.onSurface

    Column(
        modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = weatherOfText(label),
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = attributionText(),
            // The material replaces the fill, so the glyphs act as a mask and are drawn opaque.
            modifier = Modifier
                .conditionTextGlassMaterial()
                .padding(horizontal = 16.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (LocalGlassBackdrop.current != null) Color.White else onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

/** TODO: placeholder targets until the real data and map credits have somewhere to point. */
private const val WeatherDataUrl = "https://google.com"
private const val MapDataUrl = "https://google.com"

@Composable
private fun attributionText(): AnnotatedString {
    val link = SpanStyle(textDecoration = TextDecoration.Underline)
    val weather = stringResource(R.string.weather_data)
    val map = stringResource(R.string.map_data)
    val text = stringResource(R.string.attribution, weather, map)
    return buildAnnotatedString {
        append(text)
        listOf(weather to WeatherDataUrl, map to MapDataUrl).forEach { (label, url) ->
            val start = text.indexOf(label)
            if (start >= 0) {
                addStyle(link, start, start + label.length)
                addLink(LinkAnnotation.Url(url), start, start + label.length)
            }
        }
    }
}

@Composable
private fun weatherOfText(chain: List<String>): String {
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val place = if (locale.language == "zh") chain.joinToString("")
        else chain.asReversed().joinToString(", ")
    return stringResource(R.string.weather_in, place)
}

/**
 * Broad to narrow, dropping levels that repeat one another: a municipality is its own province and
 * its own prefecture, and a place often carries the prefecture's name with or without the 市 suffix.
 */
private fun locationChain(
    country: String?,
    province: String?,
    city: String?,
    place: String?,
): List<String>? {
    val chain = mutableListOf<String>()
    for (part in listOf(country, province, city, place)) {
        val value = part?.trim().orEmpty()
        if (value.isEmpty()) continue
        val previous = chain.lastOrNull()
        if (previous != null && (previous.contains(value) || value.contains(previous))) {
            // Keep whichever is more specific, which is the longer of the two.
            if (value.length > previous.length) chain[chain.lastIndex] = value
            continue
        }
        chain += value
    }
    return chain.takeIf { it.isNotEmpty() }
}

/** Country codes arrive as ISO 3166 alpha-2; the platform knows their names in the user's language. */
fun countryDisplayName(code: String?): String? {
    val iso = code?.trim().orEmpty()
    if (iso.length != 2) return code
    return Locale.Builder().setRegion(iso).build()
        .getDisplayCountry(Locale.getDefault())
        .takeIf { it.isNotBlank() && !it.equals(iso, ignoreCase = true) }
}

@Preview
@Composable
private fun Preview() {
    PreviewThemeWithBg {
        LocationFooter(
            country = "中国",
            province = "江苏省",
            city = "南京市",
            place = "南京",
            modifier = Modifier.padding(vertical = 24.dp),
        )
    }
}
