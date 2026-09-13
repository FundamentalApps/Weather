package org.fundamentalos.weather.ui.text

import android.graphics.Typeface
import android.os.Build
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.android.NoFallbackLineSpacingPatch
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.roundToInt

@Composable
fun NoFallbackText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    autoSize: TextAutoSize? = null,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current,
    disableFallbackLineSpacing: Boolean = true
) {
    val mergedStyle =
        style.merge(
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            textAlign = textAlign ?: TextAlign.Unspecified,
            lineHeight = lineHeight,
            fontFamily = fontFamily,
            textDecoration = textDecoration,
            fontStyle = fontStyle,
            letterSpacing = letterSpacing,
        )
    val patchInstalled = disableFallbackLineSpacing && remember { NoFallbackLineSpacingPatch.install() }

    if (patchInstalled) {
        val staticLayoutText =
            remember(text) {
                AnnotatedString(
                    text = text,
                    spanStyle = SpanStyle(baselineShift = BaselineShift(0f))
                )
            }
        androidx.compose.material3.Text(
            text = staticLayoutText,
            modifier = modifier,
            color = color,
            autoSize = autoSize,
            fontSize = fontSize,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing,
            textDecoration = textDecoration,
            textAlign = textAlign,
            lineHeight = lineHeight,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            minLines = minLines,
            onTextLayout = { onTextLayout?.invoke(it) },
            style = style,
        )
        return
    }

    if (
        !disableFallbackLineSpacing ||
            autoSize != null ||
            onTextLayout != null ||
            mergedStyle.fontFamily != null ||
            mergedStyle.letterSpacing.isSpecified
    ) {
        androidx.compose.material3.Text(
            text = text,
            modifier = modifier,
            color = color,
            autoSize = autoSize,
            fontSize = fontSize,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing,
            textDecoration = textDecoration,
            textAlign = textAlign,
            lineHeight = lineHeight,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            minLines = minLines,
            onTextLayout = onTextLayout,
            style = style,
        )
        return
    }

    NoFallbackLineSpacingText(
        text = text,
        modifier = modifier,
        color = color,
        style = mergedStyle,
        maxLines = maxLines,
        minLines = minLines,
        overflow = overflow,
        softWrap = softWrap,
        textAlign = textAlign
    )
}

@Composable
fun NoFallbackLineSpacingText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    textAlign: TextAlign? = null
) {
    val density = LocalDensity.current
    val contentColor = LocalContentColor.current
    val resolvedColor = color.takeOrElse { style.color.takeOrElse { contentColor } }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                includeFontPadding = false
                if (Build.VERSION.SDK_INT >= 28) {
                    setFallbackLineSpacing(false)
                }
            }
        },
        update = { view ->
            view.text = text
            view.includeFontPadding = style.platformStyle?.paragraphStyle?.includeFontPadding ?: false
            if (Build.VERSION.SDK_INT >= 28) {
                view.setFallbackLineSpacing(false)
            }

            if (resolvedColor.isSpecified) {
                view.setTextColor(resolvedColor.toArgb())
            }

            if (style.fontSize.isSpecified) {
                view.setTextSize(TypedValue.COMPLEX_UNIT_PX, with(density) { style.fontSize.toPx() })
            }

            if (style.lineHeight.isSpecified && Build.VERSION.SDK_INT >= 28) {
                view.setLineHeight(with(density) { style.lineHeight.toPx() }.roundToInt())
            }

            view.typeface = style.toTypeface()
            view.maxLines = maxLines
            view.minLines = minLines
            view.setHorizontallyScrolling(!softWrap)
            view.ellipsize =
                when (overflow) {
                    TextOverflow.Ellipsis -> TextUtils.TruncateAt.END
                    else -> null
                }
            view.paint.isUnderlineText = style.textDecoration == TextDecoration.Underline
            view.paint.isStrikeThruText = style.textDecoration == TextDecoration.LineThrough
            val resolvedTextAlign = textAlign ?: style.textAlign
            view.gravity = resolvedTextAlign.toGravity()
            view.textAlignment = resolvedTextAlign.toViewTextAlignment()
        }
    )
}

private fun TextStyle.toTypeface(): Typeface {
    val italic = fontStyle == FontStyle.Italic
    val weight = fontWeight ?: FontWeight.Normal
    return if (Build.VERSION.SDK_INT >= 28) {
        Typeface.create(Typeface.DEFAULT, weight.weight, italic)
    } else {
        val style =
            when {
                italic && weight >= FontWeight.Bold -> Typeface.BOLD_ITALIC
                italic -> Typeface.ITALIC
                weight >= FontWeight.Bold -> Typeface.BOLD
                else -> Typeface.NORMAL
            }
        Typeface.create(Typeface.DEFAULT, style)
    }
}

private fun TextAlign.toGravity(): Int =
    when (this) {
        TextAlign.Center -> Gravity.CENTER
        TextAlign.Right, TextAlign.End -> Gravity.CENTER_VERTICAL or Gravity.END
        TextAlign.Left, TextAlign.Start -> Gravity.CENTER_VERTICAL or Gravity.START
        else -> Gravity.CENTER_VERTICAL or Gravity.START
    }

private fun TextAlign.toViewTextAlignment(): Int =
    when (this) {
        TextAlign.Center -> View.TEXT_ALIGNMENT_CENTER
        TextAlign.Right, TextAlign.End -> View.TEXT_ALIGNMENT_TEXT_END
        TextAlign.Left, TextAlign.Start -> View.TEXT_ALIGNMENT_TEXT_START
        else -> View.TEXT_ALIGNMENT_TEXT_START
    }
