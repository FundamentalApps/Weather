package org.fundamentalos.weather.ui.componets

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import org.fundamentalos.weather.ui.LocalScreenCovered
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

const val FLUID_SHADER_SRC = """
    uniform float2 iResolution;
    uniform float iTime;

    // Precomputed on the CPU once per frame: these depend only on time, and evaluating them per
    // pixel cost sixteen trig calls per fragment for no reason.
    uniform float2 uBlob1;
    uniform float2 uBlob2;
    uniform float2 uBlob3;
    uniform float2 uBlob4;
    uniform float2 uRot;      // (cos, sin) of the drift rotation
    uniform float2 uMixBias;  // time wobble on the base gradient split

    layout(color) uniform half4 color1;
    layout(color) uniform half4 color2;
    layout(color) uniform half4 color3;
    layout(color) uniform half4 color4;

    float2 warp(float2 p, float t) {
        float2 q = p;
        q += 0.10 * float2(
            sin(p.y * 2.0 + t * 0.32),
            cos(p.x * 2.1 - t * 0.29)
        );
        q += 0.055 * float2(
            sin((p.x + p.y) * 3.0 - t * 0.18),
            cos((p.x - p.y) * 2.8 + t * 0.21)
        );
        return q;
    }

    float blob(float2 p, float2 center, float radius) {
        float2 d = p - center;
        return exp(-dot(d, d) / (radius * radius));
    }

    float3 saturateColor(float3 color) {
        float luma = dot(color, float3(0.299, 0.587, 0.114));
        return clamp(mix(float3(luma), color, 1.08), 0.0, 1.0);
    }

    half4 main(float2 fragCoord) {
        float minSide = max(1.0, min(iResolution.x, iResolution.y));
        float maxSide = max(iResolution.x, iResolution.y);
        float sceneScale = mix(minSide, maxSide, 0.36);
        float2 p = (fragCoord - 0.5 * iResolution.xy) / sceneScale;
        float t = iTime;

        float2 drift = float2(uRot.x * p.x - uRot.y * p.y, uRot.y * p.x + uRot.x * p.y);
        float2 q = warp(drift, t);

        float w1 = blob(q, uBlob1, 0.70);
        float w2 = blob(q, uBlob2, 0.74);
        float w3 = blob(q, uBlob3, 0.82);
        float w4 = blob(q, uBlob4, 0.78);

        float mixX = smoothstep(-1.05, 1.05, drift.x + uMixBias.x);
        float mixY = smoothstep(-1.10, 1.10, drift.y + uMixBias.y);
        float3 top = mix(float3(color1.rgb), float3(color2.rgb), mixX);
        float3 bottom = mix(float3(color3.rgb), float3(color4.rgb), mixX);
        float3 base = mix(top, bottom, mixY);

        float total = w1 + w2 + w3 + w4 + 0.0001;
        float3 blobs = (
            w1 * float3(color1.rgb) +
            w2 * float3(color2.rgb) +
            w3 * float3(color3.rgb) +
            w4 * float3(color4.rgb)
        ) / total;

        float3 color = mix(base, blobs, 0.78);
        float glow = smoothstep(1.45, 0.18, length(p));
        color *= mix(0.96, 1.06, glow);
        color = saturateColor(color);

        return half4(color.r, color.g, color.b, 1.0);
    }
"""

/**
 * The field has no detail finer than a few dozen pixels, so it is rasterised at a fraction of the
 * screen and scaled up: nine times fewer fragment shader invocations, no visible difference.
 */
private const val DownscaleFactor = 3f

/** Slight overscale so rounding the downscaled size can never leave a seam at the edges. */
private const val CoverageSlack = 1.02f

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun FluidGradientBackground(
    color1: Color,
    color2: Color,
    color3: Color,
    color4: Color,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
) {
    val shader = remember { RuntimeShader(FLUID_SHADER_SRC) }
    val shaderBrush = remember(shader) { ShaderBrush(shader) }
    val time by rememberFluidTime(animated)

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .clipToBounds(),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            Modifier
                .size(maxWidth / DownscaleFactor, maxHeight / DownscaleFactor)
                .graphicsLayer {
                    scaleX = DownscaleFactor * CoverageSlack
                    scaleY = DownscaleFactor * CoverageSlack
                }
        ) {
            shader.setFloatUniform("iResolution", size.width, size.height)
            shader.setFloatUniform("iTime", time)
            shader.setFluidFrameUniforms(time)
            shader.setColorUniform("color1", color1)
            shader.setColorUniform("color2", color2)
            shader.setColorUniform("color3", color3)
            shader.setColorUniform("color4", color4)
            drawRect(brush = shaderBrush)
        }
    }
}

@Composable
fun FluidGradientBackgroundCompat(
    color1: Color,
    color2: Color,
    color3: Color,
    color4: Color,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
) {
    var time by rememberFluidTime(animated)
    val baseGradientColors = remember(color1, color2, color3, color4) {
        listOf(color1, color2, color4, color3)
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val minSide = min(size.width, size.height)
        val sceneScale = minSide + (max(size.width, size.height) - minSide) * 0.36f

        drawRect(
            brush = Brush.linearGradient(
                colors = baseGradientColors,
                start = Offset.Zero,
                end = Offset(size.width, size.height)
            )
        )

        drawFluidBlob(
            color = color1,
            center = movingBlobCenter(seed = 0.4f, time = time, offset = Offset(-0.34f, -0.24f)),
            radius = sceneScale * 0.82f
        )
        drawFluidBlob(
            color = color2,
            center = movingBlobCenter(seed = 2.2f, time = time, offset = Offset(0.36f, -0.26f)),
            radius = sceneScale * 0.86f
        )
        drawFluidBlob(
            color = color3,
            center = movingBlobCenter(seed = 4.0f, time = time, offset = Offset(-0.32f, 0.32f)),
            radius = sceneScale * 0.94f
        )
        drawFluidBlob(
            color = color4,
            center = movingBlobCenter(seed = 5.9f, time = time, offset = Offset(0.34f, 0.34f)),
            radius = sceneScale * 0.90f
        )

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.06f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.5f, size.height * 0.42f),
                radius = sceneScale * 0.92f
            )
        )
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun RuntimeShader.setColorUniform(uniformName: String, color: Color) {
    // The packed-int overload, so this does not allocate an android.graphics.Color every frame.
    setColorUniform(uniformName, color.toArgb())
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun RuntimeShader.setFluidFrameUniforms(t: Float) {
    setBlobUniform("uBlob1", seed = 0.4f, t = t, offsetX = -0.34f, offsetY = -0.24f)
    setBlobUniform("uBlob2", seed = 2.2f, t = t, offsetX = 0.36f, offsetY = -0.26f)
    setBlobUniform("uBlob3", seed = 4.0f, t = t, offsetX = -0.32f, offsetY = 0.32f)
    setBlobUniform("uBlob4", seed = 5.9f, t = t, offsetX = 0.34f, offsetY = 0.34f)

    val angle = sin(t * 0.035f) * 0.18f
    setFloatUniform("uRot", cos(angle), sin(angle))
    setFloatUniform("uMixBias", 0.08f * sin(t * 0.07f), 0.08f * cos(t * 0.06f))
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun RuntimeShader.setBlobUniform(
    uniformName: String,
    seed: Float,
    t: Float,
    offsetX: Float,
    offsetY: Float,
) {
    val a = t * (0.16f + seed * 0.011f) + seed
    val b = t * (0.10f + seed * 0.017f) + seed * 1.7f
    setFloatUniform(
        uniformName,
        sin(a) * 0.62f + sin(b * 0.73f) * 0.24f + offsetX,
        cos(b) * 0.56f + sin(a * 0.61f) * 0.22f + offsetY,
    )
}

private const val FluidFrameIntervalMs = 33L

@Composable
private fun rememberFluidTime(animated: Boolean): androidx.compose.runtime.MutableFloatState {
    val state = remember { mutableFloatStateOf(0f) }
    val covered = LocalScreenCovered.current

    LaunchedEffect(animated, covered) {
        // Frozen: the field keeps whatever phase it had and stops invalidating.
        if (!animated) return@LaunchedEffect
        // Elapsed rather than wall clock, so a screen pushed over this one costs nothing while it
        // is up and the blobs carry on from where they were rather than jumping ahead.
        var elapsed = 0L
        var lastEmit = -FluidFrameIntervalMs
        while (isActive) {
            // Nothing under a screen that covers it needs a frame at all.
            snapshotFlow { covered() }.first { !it }
            var previousFrame = withInfiniteAnimationFrameMillis { it }
            while (isActive && !covered()) {
                withInfiniteAnimationFrameMillis { frameTime ->
                    elapsed += frameTime - previousFrame
                    previousFrame = frameTime
                    // The blobs drift a few percent of the screen per second, so 30fps is
                    // indistinguishable from 120 while each skipped update saves a full-screen
                    // redraw and a re-blur of everything sampling this background.
                    if (elapsed - lastEmit >= FluidFrameIntervalMs) {
                        lastEmit = elapsed
                        state.floatValue = elapsed / 1000f
                    }
                }
            }
        }
    }

    return state
}

private fun DrawScope.movingBlobCenter(
    seed: Float,
    time: Float,
    offset: Offset
): Offset {
    val a = time * (0.16f + seed * 0.011f) + seed
    val b = time * (0.10f + seed * 0.017f) + seed * 1.7f
    val normalized = Offset(
        x = sin(a) * 0.62f + sin(b * 0.73f) * 0.24f + offset.x,
        y = cos(b) * 0.56f + sin(a * 0.61f) * 0.22f + offset.y
    )
    val minSide = min(size.width, size.height)
    val sceneScale = minSide + (max(size.width, size.height) - minSide) * 0.36f
    return Offset(
        x = size.width * 0.5f + normalized.x * sceneScale,
        y = size.height * 0.5f + normalized.y * sceneScale
    )
}

private fun DrawScope.drawFluidBlob(
    color: Color,
    center: Offset,
    radius: Float
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = 0.82f),
                color.copy(alpha = 0.38f),
                color.copy(alpha = 0.0f)
            ),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}
