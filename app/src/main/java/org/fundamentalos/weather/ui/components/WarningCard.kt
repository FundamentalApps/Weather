package org.fundamentalos.weather.ui.components

import org.fundamentalos.weather.R
import androidx.compose.ui.res.stringResource
import org.fundamentalos.weather.ui.text.localizedWarningTime
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kyant.shapes.RoundedRectangle
import com.kyant.shapes.Capsule
import org.fundamentalos.weather.ui.theme.ColorFamily
import org.fundamentalos.weather.ui.theme.PreviewThemeWithBg
import org.fundamentalos.weather.ui.theme.colorFamily
import org.fundamentalos.weather.ui.theme.harmonized
import org.fundamentalos.weather.weather.domain.WarningSeverity
import org.fundamentalos.weather.weather.domain.WeatherWarning
import kotlinx.datetime.Instant
import kotlin.time.Clock

@Composable
fun WeatherWarningsSection(
    warnings: List<WeatherWarning>,
    modifier: Modifier = Modifier,
) {
    if (warnings.isEmpty()) return

    val sorted = warnings.sortedByDescending { it.publishedAt ?: Instant.DISTANT_PAST }
    val expanded = sorted.firstOrNull()?.takeIf { sorted.size % 2 != 0 }
    val collapsed = if (expanded != null) sorted.drop(1) else sorted

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        expanded?.let {
            WarningCardExpanded(it, it.colorFamily().harmonized())
        }
        collapsed.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { warning ->
                    WarningCardCollapsed(
                        warning = warning,
                        colors = warning.colorFamily().harmonized(),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun WarningCardExpanded(
    warning: WeatherWarning,
    colors: ColorFamily,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedRectangle(24.dp))
            .background(colors.color)
            .padding(16.dp),
    ) {
        WarningHeader(warning = warning, colors = colors)
        Spacer(Modifier.height(12.dp))
        Text(
            text = warning.text,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onColor,
        )
        warning.sender?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onColor.copy(alpha = 0.72f),
                modifier = Modifier
                    .align(Alignment.End),
            )
        }
    }
}

@Composable
private fun WarningCardCollapsed(
    warning: WeatherWarning,
    colors: ColorFamily,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedRectangle(24.dp))
            .background(colors.color)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WarningIconBadge(warning = warning, colors = colors, size = 40.dp)
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = formatWarningTitle(warning),
                style = MaterialTheme.typography.titleSmall,
                color = colors.onColor,
                maxLines = 1,
            )
            Text(
                text = formatWarningMeta(warning),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onColor.copy(alpha = 0.72f),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun WarningHeader(
    warning: WeatherWarning,
    colors: ColorFamily,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        WarningIconBadge(warning = warning, colors = colors, size = 48.dp)
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = formatWarningTitle(warning),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onColor,
            )
            Text(
                text = formatWarningMeta(warning),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onColor.copy(alpha = 0.72f),
            )
        }
    }
}

@Composable
private fun WarningIconBadge(
    warning: WeatherWarning,
    colors: ColorFamily,
    size: androidx.compose.ui.unit.Dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(Capsule())
            .background(colors.colorContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = warningIconPainter(warning.typeName),
            contentDescription = warning.typeName,
            tint = colors.onColorContainer,
            modifier = Modifier.size(size * 0.55f),
        )
    }
}

private val warningTitleSuffixRegex = Regex("""(白|蓝|绿|黄|橙|红|黑)色预警(信号)?(\s*\[[^\]]*])?$""")

private fun formatWarningTitle(warning: WeatherWarning): String =
    warning.title
        .replace(warningTitleSuffixRegex, "")
        .trim()
        .ifBlank { warning.typeName.orEmpty().ifBlank { warning.title } }

@Composable
private fun formatWarningMeta(warning: WeatherWarning): String =
    listOfNotNull(
        warningColorLabel(warning),
        formatWarningTime(warning.publishedAt).takeIf { it.isNotBlank() },
    ).joinToString(" · ")

@Composable
private fun warningColorLabel(warning: WeatherWarning): String? =
    warning.severityColor
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.let { warningColorName(it) }
        ?: warningTitleSuffixRegex.find(warning.title)?.groupValues?.getOrNull(1)?.let { warningColorName(it) }

@Composable
private fun warningColorName(value: String): String =
    when (value.lowercase()) {
        "white", "白", "白色" -> stringResource(R.string.color_white)
        "blue", "蓝", "蓝色" -> stringResource(R.string.color_blue)
        "green", "绿", "绿色" -> stringResource(R.string.color_green)
        "yellow", "黄", "黄色" -> stringResource(R.string.color_yellow)
        "orange", "橙", "橙色" -> stringResource(R.string.color_orange)
        "red", "红", "红色" -> stringResource(R.string.color_red)
        "black", "黑", "黑色" -> stringResource(R.string.color_black)
        else -> value
    }

@Composable
private fun formatWarningTime(instant: Instant?): String {
    if (instant == null) return ""
    return localizedWarningTime(instant)

}

@Preview
@Composable
private fun PreviewWarnings() {
    val now = Clock.System.now()
    PreviewThemeWithBg {
        Column(Modifier.padding(16.dp)) {
            WeatherWarningsSection(
                warnings = listOf(
                    sample("1", "大风蓝色预警", "Blue", "大风", now, body = "受冷空气影响，预计4月1日白天至前半夜我市有3、4级偏北风，阵风7级左右，山区阵风可达8级以上，请注意防范。", sender = "北京市气象局"),
                    sample("2", "大风橙色预警", "Orange", "大风", now),
                    sample("3", "大风黄色预警", "Yellow", "大风", now),
                    sample("4", "大风红色预警", "Red", "大风", now),
                    sample("5", "大风白色预警", "White", "大风", now),
                    sample("6", "大风绿色预警", "Green", "大风", now),
                    sample("7", "大风黑色预警", "Black", "大风", now),
                ),
            )
        }
    }
}

private fun sample(
    id: String,
    title: String,
    color: String,
    type: String,
    publishedAt: Instant,
    body: String = "",
    sender: String? = null,
) = WeatherWarning(
    id = id,
    title = title,
    text = body,
    defenseGuide = null,
    typeName = type,
    severity = WarningSeverity.Unknown,
    severityColor = color,
    sender = sender,
    status = "active",
    publishedAt = publishedAt,
    startTime = null,
    endTime = null,
)
