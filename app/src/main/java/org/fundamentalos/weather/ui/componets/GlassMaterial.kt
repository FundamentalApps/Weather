package org.fundamentalos.weather.ui.componets

import android.os.Build
import androidx.annotation.RequiresApi
import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.BitmapShader
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private const val Tag = "GlassMaterial"
private const val GlassBackdropBlurInputScale = 0.3334f

private fun androidx.compose.ui.geometry.Size.toLayerIntSize(): IntSize =
    IntSize(
        width = width.roundToInt().coerceAtLeast(1),
        height = height.roundToInt().coerceAtLeast(1),
    )

@Immutable
data class GlassMaterialSpec(
    val saturation: Float,
    val fillGray: Float,
    val fillAlpha: Float,
)

/** [channelLift] is signed: text lifts off a dark backdrop and cuts down into a bright one. */
@Immutable
data class GlassTextMaterialSpec(
    val channelLift: Float,
)

val AppleWeatherCardMaterial = GlassMaterialSpec(
    saturation = 1.582f,
    fillGray = 0x3B / 255f,
    fillAlpha = 0.258f,
)

val AppleWeatherConditionTextMaterial = GlassTextMaterialSpec(
    channelLift = 140.95f / 255f,
)

/**
 * The same material for a daytime sky. Lifting a bright backdrop clamps every channel to white,
 * which leaves a flat white shape with none of the backdrop in it; cutting down by the same amount
 * keeps the relationship and the contrast.
 */
val AppleWeatherConditionTextMaterialLight = GlassTextMaterialSpec(
    channelLift = -140.95f / 255f,
)

/**
 * Material for card-title labels and 1-px hairline dividers. Sampled (bg, title) pairs land on
 * `title = bg + 68/255`, channel-uniform — same shader as condition text, smaller lift. Source
 * is the already-rendered [GlassBackdropState.cardLayer], not the bare blurred sky: title/
 * divider live on top of the card material, so no second blur pass is needed.
 */
val AppleWeatherCardTitleMaterial = GlassTextMaterialSpec(
    channelLift = 68f / 255f,
)

/** Card titles on a light card, cutting down for the same reason as the condition text. */
val AppleWeatherCardTitleMaterialLight = GlassTextMaterialSpec(
    channelLift = -68f / 255f,
)

/**
 * Apple Weather card material as an AGSL shader.
 *
 * This keeps the model close to a system blur material: blur the shared backdrop once, boost the
 * blurred color's saturation, then apply a neutral gray fill. The shader is intentionally simple
 * because the sampled card color includes a large-radius blur neighborhood, not just the single
 * background pixel measured beside the card.
 */
private const val CardMaterialShaderSource = """
    uniform shader content;
    uniform float saturation;
    uniform float fillGray;
    uniform float fillAlpha;

    half4 main(float2 coord) {
        half4 src = content.eval(coord);
        half3 rgb = src.rgb;
        half  Y   = dot(rgb, half3(0.2126, 0.7152, 0.0722));
        half3 boosted = Y + (rgb - Y) * saturation;
        half3 dst = mix(boosted, half3(fillGray), fillAlpha);
        return half4(clamp(dst, 0.0, 1.0), src.a);
    }
"""

/**
 * Apple Weather condition text material. Sampled pairs are almost exactly `text = bg + 141`,
 * clamped per channel, which matches a simple additive lift over the shared blurred backdrop.
 */
private const val ConditionTextMaterialShaderSource = """
    uniform shader content;
    uniform float channelLift;

    half4 main(float2 coord) {
        half4 src = content.eval(coord);
        half3 dst = clamp(src.rgb + channelLift, half3(0.0), half3(1.0));
        return half4(dst, src.a);
    }
"""

@Stable
class GlassBackdropState internal constructor(
    internal val sourceLayer: GraphicsLayer,
    internal val blurredLayer: GraphicsLayer,
    internal val cardLayer: GraphicsLayer,
    internal val conditionTextLayer: GraphicsLayer,
    internal val cardTitleTextLayer: GraphicsLayer,
    internal val blurInputScale: Float,
) {
    internal var origin: Offset = Offset.Zero
}

private class OffsetHolder(var value: Offset)

private fun Modifier.trackRootPosition(holder: OffsetHolder): Modifier =
    this.then(RootPositionTrackingElement(holder))

private data class RootPositionTrackingElement(
    private val holder: OffsetHolder,
) : ModifierNodeElement<RootPositionTrackingNode>() {
    override fun create(): RootPositionTrackingNode = RootPositionTrackingNode(holder)

    override fun update(node: RootPositionTrackingNode) {
        node.holder = holder
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "trackRootPosition"
        properties["holder"] = holder
    }
}

private class RootPositionTrackingNode(
    var holder: OffsetHolder,
) : Modifier.Node(), GlobalPositionAwareModifierNode, DrawModifierNode {
    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val nextPosition = coordinates.positionInRoot()
        if (holder.value != nextPosition) {
            holder.value = nextPosition
            invalidateDraw()
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
    }
}

/**
 * Records the weather background once, blurs it once, then derives material-specific layers from
 * that shared blur. Cards and condition text sample their own material layers without re-running
 * the expensive backdrop blur.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun rememberGlassBackdropState(
    blurRadius: Dp = 150.dp,
    blurInputScale: Float = GlassBackdropBlurInputScale,
    cardMaterial: GlassMaterialSpec = AppleWeatherCardMaterial,
    conditionTextMaterial: GlassTextMaterialSpec = AppleWeatherConditionTextMaterial,
    cardTitleMaterial: GlassTextMaterialSpec = AppleWeatherCardTitleMaterial,
): GlassBackdropState {
    val source = rememberGraphicsLayer()
    val blurred = rememberGraphicsLayer()
    val card = rememberGraphicsLayer()
    val conditionText = rememberGraphicsLayer()
    val cardTitleText = rememberGraphicsLayer()
    val blurRadiusPx = with(LocalDensity.current) { blurRadius.toPx() }
    val resolvedBlurInputScale = blurInputScale.coerceIn(0.1f, 1f)
    val scaledBlurRadiusPx = blurRadiusPx * resolvedBlurInputScale

    val blurEffect = remember(scaledBlurRadiusPx) {
        AndroidRenderEffect.createBlurEffect(
            scaledBlurRadiusPx, scaledBlurRadiusPx, Shader.TileMode.CLAMP,
        ).asComposeRenderEffect()
    }
    val cardEffect = remember(cardMaterial) {
        try {
            val shader = RuntimeShader(CardMaterialShaderSource).apply {
                setFloatUniform("saturation", cardMaterial.saturation)
                setFloatUniform("fillGray", cardMaterial.fillGray)
                setFloatUniform("fillAlpha", cardMaterial.fillAlpha)
            }
            AndroidRenderEffect.createRuntimeShaderEffect(
                shader,
                "content",
            ).asComposeRenderEffect()
        } catch (error: RuntimeException) {
            Log.w(Tag, "Falling back to blur-only glass; material shader failed to compile.", error)
            null
        }
    }
    val conditionTextEffect = remember(conditionTextMaterial) {
        try {
            val shader = RuntimeShader(ConditionTextMaterialShaderSource).apply {
                setFloatUniform("channelLift", conditionTextMaterial.channelLift)
            }
            AndroidRenderEffect.createRuntimeShaderEffect(
                shader,
                "content",
            ).asComposeRenderEffect()
        } catch (error: RuntimeException) {
            Log.w(Tag, "Falling back to blur-only condition text; material shader failed to compile.", error)
            null
        }
    }
    val cardTitleTextEffect = remember(cardTitleMaterial) {
        try {
            val shader = RuntimeShader(ConditionTextMaterialShaderSource).apply {
                setFloatUniform("channelLift", cardTitleMaterial.channelLift)
            }
            AndroidRenderEffect.createRuntimeShaderEffect(
                shader,
                "content",
            ).asComposeRenderEffect()
        } catch (error: RuntimeException) {
            Log.w(Tag, "Falling back to no-op card title material; shader failed to compile.", error)
            null
        }
    }
    SideEffect {
        blurred.renderEffect = blurEffect
        card.renderEffect = cardEffect
        conditionText.renderEffect = conditionTextEffect
        cardTitleText.renderEffect = cardTitleTextEffect
    }

    return remember(
        source,
        blurred,
        card,
        conditionText,
        cardTitleText,
        resolvedBlurInputScale,
    ) {
        GlassBackdropState(
            source,
            blurred,
            card,
            conditionText,
            cardTitleText,
            resolvedBlurInputScale,
        )
    }
}

/**
 * Marks this composable as the source content for the backdrop. Must wrap whatever draws the
 * sky on the outside (`Modifier.glassBackdropSource(state).background(brush)`) so the brush
 * paints inside the source layer's record block.
 */
fun Modifier.glassBackdropSource(state: GlassBackdropState): Modifier = this
    .onGloballyPositioned { state.origin = it.positionInRoot() }
    .drawWithContent {
        val blurInputScale = state.blurInputScale
        val layerSize = size.toLayerIntSize()
        val scaledLayerSize = (size * blurInputScale).toLayerIntSize()

        state.sourceLayer.record { this@drawWithContent.drawContent() }
        state.blurredLayer.record(size = scaledLayerSize) {
            scale(blurInputScale, pivot = Offset.Zero) {
                drawLayer(state.sourceLayer)
            }
        }
        state.cardLayer.record(size = layerSize) {
            scale(1f / blurInputScale, pivot = Offset.Zero) {
                drawLayer(state.blurredLayer)
            }
        }
        state.conditionTextLayer.record(size = layerSize) {
            scale(1f / blurInputScale, pivot = Offset.Zero) {
                drawLayer(state.blurredLayer)
            }
        }
        state.cardTitleTextLayer.record(size = layerSize) { drawLayer(state.cardLayer) }
        drawLayer(state.sourceLayer)
    }

val LocalGlassBackdrop = compositionLocalOf<GlassBackdropState?> { null }

/**
 * Renders this composable's content as an alpha mask over the condition-text material. The caller
 * should draw any normal dark text shadow separately; this mask should contain only the glyph fill.
 * The offscreen compositing layer keeps [BlendMode.DstIn] from affecting anything already drawn
 * behind this composable.
 */
@Composable
fun Modifier.conditionTextGlassMaterial(): Modifier {
    val backdrop = LocalGlassBackdrop.current ?: return this
    val maskLayer = rememberGraphicsLayer()
    val pos = remember { OffsetHolder(Offset.Zero) }

    return this
        .graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        }
        .trackRootPosition(pos)
        .drawWithContent {
            val layerSize = size.toLayerIntSize()
            maskLayer.blendMode = BlendMode.DstIn
            maskLayer.record(size = layerSize) { this@drawWithContent.drawContent() }

            val dx = pos.value.x - backdrop.origin.x
            val dy = pos.value.y - backdrop.origin.y
            translate(left = -dx, top = -dy) {
                drawLayer(backdrop.conditionTextLayer)
            }
            drawLayer(maskLayer)
        }
}

/**
 * Same alpha-mask pattern as [conditionTextGlassMaterial], but samples the card-title material
 * (a +68/255 lift over the already-shaded card surface). Use for card labels and 1-px hairline
 * dividers — anything that sits on top of the card material and needs a slight brightening lift.
 * If the caller draws a text shadow, render it in a separate transparent pass first; the mask
 * here should contain only the glyph/line alpha.
 */
@Composable
fun Modifier.cardTitleGlassMaterial(): Modifier {
    val backdrop = LocalGlassBackdrop.current ?: return this
    val maskLayer = rememberGraphicsLayer()
    val pos = remember { OffsetHolder(Offset.Zero) }

    return this
        .graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        }
        .trackRootPosition(pos)
        .drawWithContent {
            val layerSize = size.toLayerIntSize()
            maskLayer.blendMode = BlendMode.DstIn
            maskLayer.record(size = layerSize) { this@drawWithContent.drawContent() }

            val dx = pos.value.x - backdrop.origin.x
            val dy = pos.value.y - backdrop.origin.y
            translate(left = -dx, top = -dy) {
                drawLayer(backdrop.cardTitleTextLayer)
            }
            drawLayer(maskLayer)
        }
}

