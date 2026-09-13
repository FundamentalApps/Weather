package org.fundamentalos.weather.ui.sky

/** Texture-based atmospheric layers calibrated against the supplied phone references. */
internal const val SKY_SHADER = """
uniform float2 resolution;
uniform float2 textureSize;
uniform shader density;
uniform float time;
uniform float daylight;
uniform float dusk;
uniform float2 sun;
uniform float cover;
uniform float precip;
uniform float haze;
uniform float dust;
uniform float moon;
uniform float cumulus;
uniform float storm;
uniform float2 wind;
uniform float layers;
layout(color) uniform half4 topColor;
layout(color) uniform half4 middleColor;
layout(color) uniform half4 bottomColor;

float sampleCloud(float2 p) { return density.eval(p * textureSize).r; }

float2 cloudCoordinates(float2 uv, float scale, float speed, float seed) {
    float2 puffy = float2(uv.x * 0.68, uv.y * 1.05) * scale;
    float2 wispy = float2(uv.x * 0.22, (uv.y + uv.x * 0.16) * 3.6) * scale;
    float form = max(cumulus, storm * 0.85);
    // Surface calm does not imply stationary high cloud. Keep a gentle drift,
    // with depth-dependent layer speeds, visible over a few seconds.
    float windSpeed = length(wind);
    float2 flow = windSpeed > 0.001 ? wind * (max(windSpeed, 0.18) / windSpeed) : float2(0.18, 0.025);
    return mix(wispy, puffy, form) + float2(seed, seed * 0.37) - flow * time * speed * 4.0;
}

// Density and opacity are shared by visible clouds and source occlusion.
float2 cloudSample(float2 uv, float scale, float speed, float seed, float strength) {
    float d = sampleCloud(cloudCoordinates(uv, scale, speed, seed));
    float threshold = mix(0.95, 0.15, cover);
    float form = max(cumulus, storm * 0.85);
    // Terminate the cloud volume through its density contour, not a screen-wide
    // opacity fade. Dense lobes extend farther down; gaps expose untouched sky.
    float bankDensity = d - max(uv.y - 0.22, 0.0) * 4.0;
    float edgeWidth = mix(0.14, 0.065, form);
    float alpha = smoothstep(threshold - edgeWidth, threshold + edgeWidth, bankDensity) * strength;
    alpha *= smoothstep(0.06, 0.22, cover);
    return float2(d, alpha);
}

float4 cloudLayer(float2 uv, float scale, float speed, float seed, float strength) {
    float2 p = cloudCoordinates(uv, scale, speed, seed);
    float2 field = cloudSample(uv, scale, speed, seed, strength);
    float d = field.x;
    float2 lightDir = normalize(sun - uv + float2(0.0001));
    float toward = sampleCloud(p + lightDir * 0.035);
    float rim = clamp((d - toward) * 0.65, -0.08, 0.10);
    float3 light = mix(float3(0.21, 0.21, 0.30), float3(0.78, 0.83, 0.89), daylight);
    light = mix(light, float3(0.55, 0.63, 0.70), storm * daylight);
    light += (rim + d * 0.07) * mix(0.45, 1.0, daylight);
    light = mix(light, float3(0.88, 0.69, 0.58), dusk * 0.22);
    return float4(light, field.y);
}

// Sample the source itself, not the pixels at the ray tips. A cloud over the
// core must suppress all diffraction rays, including tips outside that cloud.
float cloudTransmission(float2 point) {
    if (cover <= 0.06 || point.y >= 0.50) return 1.0;
    float transmittance = 1.0 - cloudSample(point, 1.15, 0.00055, 0.13, 0.42).y;
    transmittance *= 1.0 - cloudSample(point, 0.70, 0.0011, 0.57, 0.70).y;
    if (layers > 2.5) transmittance *= 1.0 - cloudSample(point, 0.48, 0.0017, 1.21, 0.64).y;
    return transmittance;
}

float sourceTransmission(float aspect) {
    float center = cloudTransmission(sun);
    float2 dx = float2(0.016 / aspect, 0.0);
    float2 dy = float2(0.0, 0.016);
    float area = (center * 2.0 + cloudTransmission(sun + dx) + cloudTransmission(sun - dx)
        + cloudTransmission(sun + dy) + cloudTransmission(sun - dy)) / 6.0;
    return min(center, area);
}

// Optical effects are composited over clouds: they belong to the camera/lens.
float3 lensOptics(float2 uv, float2 solarPosition, float aspect, float haloVisibility, float ghostVisibility) {
    float2 offset = (uv - solarPosition) * float2(aspect, 1.0);
    float2 opticalAxis = (float2(0.58, 0.53) - solarPosition) * float2(aspect, 1.0);
    float2 axisDirection = normalize(opticalAxis + float2(0.0001));
    float2 across = float2(-axisDirection.y, axisDirection.x);
    // An off-axis lens shifts and stretches the halo; no GPS-seeded random wobble.
    float2 shifted = offset - opticalAxis * 0.055;
    float2 projected = float2(dot(shifted, across) * 1.055, dot(shifted, axisDirection) * 0.945);
    float theta = atan(projected.y, projected.x);
    float tilt = atan(opticalAxis.y, opticalAxis.x);
    float deformation = 1.0 + 0.047 * sin(theta * 3.0 + tilt) + 0.023 * cos(theta * 5.0 - tilt);
    float radius = length(projected) / deformation;
    // Broad, low-saturation spectral halo. The lower arc is most visible in the reference.
    // Scene art direction calibrated to the two supplied captures, not a physical cloud law.
    float thinCloud = smoothstep(0.08, 0.24, cover);
    float haloRadius = mix(0.272, 0.218, thinCloud);
    float redBand = exp(-pow((radius - haloRadius + 0.016) / 0.018, 2.0));
    float greenBand = exp(-pow((radius - haloRadius) / 0.019, 2.0));
    float blueBand = exp(-pow((radius - haloRadius - 0.019) / 0.022, 2.0));
    float arc = (0.35 + 0.65 * smoothstep(-0.02, 0.10, offset.y)) * (0.81 + 0.19 * cos(theta - tilt + 0.6));
    float3 glow = float3(redBand * 0.085 + greenBand * 0.010,
        greenBand * 0.070 + redBand * 0.032,
        blueBand * 0.075 + greenBand * 0.012) * arc * mix(0.68, 0.88, thinCloud);
    float3 reflections = float3(0.0);
    float2 axis = float2(0.58, 0.53) - solarPosition;
    for (int i = 0; i < 5; ++i) {
        float fi = float(i);
        float position = i == 0 ? 0.17 : (i == 1 ? 0.30 : (i == 2 ? 0.47 : (i == 3 ? 0.71 : 0.94)));
        float discRadius = i == 0 ? 0.010 : (i == 1 ? 0.023 : (i == 2 ? 0.017 : (i == 3 ? 0.026 : 0.015)));
        float2 center = solarPosition + axis * position;
        float2 displacement = (uv - center) * float2(aspect, 1.0);
        float d = length(displacement);
        float disc = 1.0 - smoothstep(discRadius * 0.75, discRadius * 1.15, d);
        float rim = exp(-pow((d - discRadius) / (discRadius * 0.13), 2.0));
        float strength = i == 1 ? 0.055 : (i == 3 ? 0.025 : 0.040);
        float3 tint = i == 0 ? float3(0.80, 0.40, 1.0) : float3(0.53, 0.80, 1.0);
        reflections += tint * (disc + rim * 0.22) * strength;
        // Defocused violet highlight immediately below the source, with a soft lateral streak.
        if (i == 0) {
            float blur = exp(-dot(displacement, displacement) / 0.00032);
            float streak = exp(-pow(displacement.x / 0.025, 2.0) - pow(displacement.y / 0.0035, 2.0));
            reflections += float3(0.48, 0.27, 0.72) * (blur * 0.085 + streak * 0.075);
        }
    }
    return glow * haloVisibility + reflections * ghostVisibility;
}

half4 main(float2 coord) {
    float2 uv = coord / resolution;
    float aspect = resolution.x / resolution.y;
    float3 sky = mix(float3(topColor.rgb), float3(middleColor.rgb), clamp(uv.y * 2.0, 0.0, 1.0));
    sky = mix(sky, float3(bottomColor.rgb), clamp((uv.y - 0.5) * 2.0, 0.0, 1.0));
    float2 delta = (uv - sun) * float2(aspect, 1.0);
    float distance = length(delta);
    // Patchy clouds elsewhere must not dim an otherwise unobstructed solar disc.
    float visibility = (1.0 - smoothstep(0.65, 0.90, cover)) * daylight;
    float source = visibility > 0.001 ? sourceTransmission(aspect) : 0.0;
    float edgeDistance = min(min(sun.x, 1.0 - sun.x) * aspect, min(sun.y, 1.0 - sun.y));
    float sourceGate = smoothstep(0.35, 0.85, source) * smoothstep(0.0, 0.025, edgeDistance);
    float solarVisibility = visibility * sourceGate;
    // Rays/lens ghosts retire before the core becomes unreadable through a cloud.
    float directExposure = smoothstep(0.60, 0.92, source);
    float opticsVisibility = solarVisibility * directExposure;
    if (solarVisibility > 0.001) {
    float angle = atan(delta.y, delta.x);
    // Broad, stable asymmetry rather than noisy texture or a sixfold halo.
    // Fade deformation out at the source so the white core stays centered.
    float scatterAxis = atan(0.53 - sun.y, (0.58 - sun.x) * aspect);
    float scatterShape = 0.12 * sin(angle - scatterAxis + 0.7)
        + 0.075 * cos(3.0 * angle + scatterAxis + 0.4)
        + 0.035 * sin(5.0 * angle - 1.1);
    float scatterDistance = distance / (1.0 + scatterShape * smoothstep(0.022, 0.11, distance));
    // Neutral aerial scattering removes the blue cast around a bright solar source.
    float aureole = exp(-scatterDistance * scatterDistance / 0.025);
    sky = mix(sky, float3(0.71, 0.71, 0.69), aureole * solarVisibility * 0.58);
    // One continuous emitter: the six lobes extend the disc rather than overlaying it.
    float orientation = 0.3 + (sun.x - 0.315) * 0.8;
    float aperture = abs(cos(angle * 3.0 + orientation));
    // Broad roots merge into the aureole; only the outer tips become narrow.
    float raySharpness = mix(7.0, 26.0, smoothstep(0.022, 0.09, distance));
    float rays = pow(aperture, raySharpness);
    float rayFalloff = exp(-max(distance - 0.033, 0.0) / 0.024);
    rayFalloff *= 1.0 - smoothstep(0.092, 0.118, distance);
    float core = 1.0 - smoothstep(0.022, 0.052, distance);
    float diffraction = rays * rayFalloff * directExposure;
    // A smooth union hides all angular structure inside the fully lit core.
    float emission = core + (1.0 - core) * diffraction;
    // Uneven, soft scattering fills the spaces between the diffraction lobes.
    // A wider, faint skirt blends into the sky without enlarging the white core.
    float innerScatter = exp(-scatterDistance * scatterDistance / 0.0080) * 0.92;
    float outerScatter = exp(-scatterDistance * scatterDistance / 0.0240) * 0.24;
    float bloom = innerScatter + (1.0 - innerScatter) * outerScatter;
    float softLobes = pow(aperture, 8.0) * rayFalloff * directExposure;
    float scattering = clamp(bloom + softLobes * (1.0 - core) * 0.10, 0.0, 1.0);
    sky = mix(sky, float3(0.99, 0.945, 0.85), scattering * solarVisibility);
    float3 emissionColor = mix(float3(1.0, 0.992, 0.975), float3(1.0, 0.97, 0.89),
        smoothstep(0.018, 0.10, distance));
    sky = mix(sky, emissionColor, emission * solarVisibility);
    }
    // Randomized cell positions avoid a visible star grid. No flashing star animation.
    float2 grid = uv * float2(52.0, 110.0);
    float2 cell = floor(grid);
    float hash = fract(sin(dot(cell, float2(127.1, 311.7))) * 43758.5453);
    float hash2 = fract(hash * 135.78);
    float2 pos = float2(0.2 + hash * 0.6, 0.2 + hash2 * 0.6);
    float star = exp(-dot(fract(grid) - pos, fract(grid) - pos) / mix(0.001, 0.012, hash2));
    float visibleStars = step(0.956, hash) * (1.0 - daylight) * (1.0 - haze * 0.7);
    float3 starColor = mix(float3(0.55, 0.69, 0.92), float3(1.0, 0.94, 0.73), hash2);
    sky += starColor * star * visibleStars * (0.35 + hash2 * 0.5);
    if (uv.y < 0.50 && cover > 0.06) {
    float4 farCloud = cloudLayer(uv, 1.15, 0.00055, 0.13, 0.42);
    sky = mix(sky, farCloud.rgb, farCloud.a);
    float4 midCloud = cloudLayer(uv, 0.70, 0.0011, 0.57, 0.70);
    sky = mix(sky, midCloud.rgb, midCloud.a);
    if (layers > 2.5) {
        float4 nearCloud = cloudLayer(uv, 0.48, 0.0017, 1.21, 0.64);
        sky = mix(sky, nearCloud.rgb, nearCloud.a);
    }
    }
    float3 fog = mix(float3(0.10, 0.13, 0.19), float3(0.64, 0.70, 0.75), daylight);
    fog = mix(fog, float3(0.56, 0.43, 0.28), dust * 0.8);
    float fogAmount = clamp(haze * (0.06 + 0.10 * uv.y) * (1.0 - precip * 0.75) + dust * 0.3, 0.0, 0.65);
    sky = mix(sky, fog, fogAmount);
    if (solarVisibility > 0.001) sky += lensOptics(uv, sun, aspect, solarVisibility, opticsVisibility);
    return half4(half3(clamp(sky, 0.0, 1.0)), 1.0);
}
"""
