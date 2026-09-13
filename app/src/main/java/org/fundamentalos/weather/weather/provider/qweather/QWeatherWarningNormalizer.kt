package org.fundamentalos.weather.weather.provider.qweather

// QWeather raw title: "上海中心气象台发布大风蓝色预警[Ⅳ级/一般]"
// Normalized:         "大风蓝色预警"
private val TITLE_REGEX = Regex("""([\u4e00-\u9fa5]+)(白|蓝|绿|黄|橙|红|黑)色预警""")

// QWeather raw text: "某气象台2023年04月03日10时30分发布大风蓝色预警[Ⅳ级/一般]：正文..."
// Strip up to and including the first full-width/ASCII colon after "预警".
private val TEXT_PREFIX_REGEX = Regex("""^[^：:]*?发布[^：:]*?预警(信号)?\s*(\[[^\]]*\])?\s*[：:]\s*""")

// 防御指南 section — appears in body as "防御指南：..." or just "防御指南\n..."
private val DEFENSE_GUIDE_REGEX = Regex("""防御(指南|措施|建议)[：:\s]*""")

data class NormalizedWarning(val title: String, val body: String, val defenseGuide: String?)

fun normalizeQWeatherWarning(rawTitle: String, rawText: String): NormalizedWarning {
    val title = normalizeTitle(rawTitle)
    val (body, guide) = normalizeText(rawText)
    return NormalizedWarning(title = title, body = body, defenseGuide = guide)
}

private fun normalizeTitle(raw: String): String {
    // QWeather titles often include the issuer name before the hazard type:
    // e.g. "上海中心气象台发布大风蓝色预警[Ⅳ级/一般]"
    // The greedy ([\u4e00-\u9fa5]+) would swallow the entire prefix otherwise,
    // so strip everything up to and including "发布" first.
    val candidate = if ("发布" in raw) raw.substringAfterLast("发布") else raw
    val match = TITLE_REGEX.find(candidate) ?: TITLE_REGEX.find(raw) ?: return raw
    return "${match.groupValues[1]}${match.groupValues[2]}色预警"
}

private fun normalizeText(raw: String): Pair<String, String?> {
    val stripped = TEXT_PREFIX_REGEX.replaceFirst(raw, "").trim()
    val guideMatch = DEFENSE_GUIDE_REGEX.find(stripped) ?: return Pair(stripped, null)
    val body = stripped.substring(0, guideMatch.range.first).trim()
    val guide = stripped.substring(guideMatch.range.last + 1).trim().takeIf { it.isNotBlank() }
    return Pair(body, guide)
}
