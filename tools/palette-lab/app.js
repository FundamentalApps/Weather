const controls = [
  "sunAltitude",
  "sunProgress",
  "cloudCover",
  "rainAmount",
  "hazeAmount",
  "dustAmount",
  "temperature",
  "saturation",
  "brightness",
  "cohesion",
  "accentAmount",
  "accentFocus",
  "motion"
];

const swatchMeta = [
  { key: "skyA", label: "天空 A" },
  { key: "skyB", label: "天空 B" },
  { key: "massC", label: "云影 C" },
  { key: "lightD", label: "光色 D" }
];

const presets = {
  clearMorning: {
    name: "晴天早晨",
    sunAltitude: 12,
    sunProgress: 18,
    cloudCover: 10,
    rainAmount: 0,
    hazeAmount: 0,
    dustAmount: 0,
    temperature: 18,
    saturation: 92,
    brightness: 0,
    cohesion: 92,
    accentAmount: 34,
    accentFocus: 0,
    motion: 68
  },
  clearNoon: {
    name: "晴天正午",
    sunAltitude: 60,
    sunProgress: 50,
    cloudCover: 4,
    rainAmount: 0,
    hazeAmount: 0,
    dustAmount: 0,
    temperature: 29,
    saturation: 90,
    brightness: 1,
    cohesion: 94,
    accentAmount: 26,
    accentFocus: -1,
    motion: 52
  },
  cloudyDay: {
    name: "多云",
    sunAltitude: 48,
    sunProgress: 50,
    cloudCover: 58,
    rainAmount: 0,
    hazeAmount: 6,
    dustAmount: 0,
    temperature: 24,
    saturation: 90,
    brightness: 0,
    cohesion: 92,
    accentAmount: 34,
    accentFocus: -1,
    motion: 58
  },
  clearNight: {
    name: "晴夜",
    sunAltitude: -10,
    sunProgress: 50,
    cloudCover: 8,
    rainAmount: 0,
    hazeAmount: 0,
    dustAmount: 0,
    temperature: 8,
    saturation: 82,
    brightness: -2,
    cohesion: 92,
    accentAmount: 32,
    accentFocus: -2,
    motion: 38
  },
  goldenHour: {
    name: "薄云黄昏",
    sunAltitude: 4,
    sunProgress: 88,
    cloudCover: 28,
    rainAmount: 0,
    hazeAmount: 8,
    dustAmount: 0,
    temperature: 23,
    saturation: 96,
    brightness: -1,
    cohesion: 84,
    accentAmount: 46,
    accentFocus: 6,
    motion: 64
  },
  overcast: {
    name: "阴天",
    sunAltitude: 22,
    sunProgress: 42,
    cloudCover: 88,
    rainAmount: 8,
    hazeAmount: 12,
    dustAmount: 0,
    temperature: 17,
    saturation: 78,
    brightness: -4,
    cohesion: 96,
    accentAmount: 22,
    accentFocus: 0,
    motion: 44
  },
  rain: {
    name: "雨天",
    sunAltitude: 18,
    sunProgress: 44,
    cloudCover: 94,
    rainAmount: 74,
    hazeAmount: 18,
    dustAmount: 0,
    temperature: 14,
    saturation: 82,
    brightness: -7,
    cohesion: 94,
    accentAmount: 46,
    accentFocus: -3,
    motion: 92
  },
  mist: {
    name: "雾",
    sunAltitude: 16,
    sunProgress: 24,
    cloudCover: 70,
    rainAmount: 10,
    hazeAmount: 90,
    dustAmount: 0,
    temperature: 9,
    saturation: 68,
    brightness: 4,
    cohesion: 98,
    accentAmount: 16,
    accentFocus: 2,
    motion: 34
  },
  snow: {
    name: "雪天",
    sunAltitude: 18,
    sunProgress: 44,
    cloudCover: 86,
    rainAmount: 72,
    hazeAmount: 18,
    dustAmount: 0,
    temperature: -4,
    saturation: 72,
    brightness: 5,
    cohesion: 98,
    accentAmount: 22,
    accentFocus: -3,
    motion: 40
  },
  snowNight: {
    name: "夜雪",
    sunAltitude: -8,
    sunProgress: 50,
    cloudCover: 92,
    rainAmount: 66,
    hazeAmount: 22,
    dustAmount: 0,
    temperature: -5,
    saturation: 70,
    brightness: -1,
    cohesion: 98,
    accentAmount: 28,
    accentFocus: -4,
    motion: 28
  },
  freezingRain: {
    name: "冻雨",
    sunAltitude: 10,
    sunProgress: 42,
    cloudCover: 96,
    rainAmount: 78,
    hazeAmount: 34,
    dustAmount: 0,
    temperature: 0,
    saturation: 72,
    brightness: -4,
    cohesion: 96,
    accentAmount: 34,
    accentFocus: -5,
    motion: 48
  },
  dust: {
    name: "浮尘",
    sunAltitude: 28,
    sunProgress: 55,
    cloudCover: 48,
    rainAmount: 0,
    hazeAmount: 28,
    dustAmount: 58,
    temperature: 24,
    saturation: 74,
    brightness: -2,
    cohesion: 96,
    accentAmount: 24,
    accentFocus: 6,
    motion: 36
  },
  sandstorm: {
    name: "沙尘暴",
    sunAltitude: 18,
    sunProgress: 58,
    cloudCover: 72,
    rainAmount: 0,
    hazeAmount: 52,
    dustAmount: 94,
    temperature: 27,
    saturation: 78,
    brightness: -8,
    cohesion: 92,
    accentAmount: 54,
    accentFocus: 8,
    motion: 82
  },
  storm: {
    name: "雷雨",
    sunAltitude: 34,
    sunProgress: 62,
    cloudCover: 100,
    rainAmount: 96,
    hazeAmount: 20,
    dustAmount: 0,
    temperature: 25,
    saturation: 78,
    brightness: -5,
    cohesion: 86,
    accentAmount: 44,
    accentFocus: 1,
    motion: 118
  },
  stormDusk: {
    name: "雷雨黄昏",
    sunAltitude: 4,
    sunProgress: 88,
    cloudCover: 100,
    rainAmount: 92,
    hazeAmount: 24,
    dustAmount: 0,
    temperature: 23,
    saturation: 88,
    brightness: -7,
    cohesion: 78,
    accentAmount: 56,
    accentFocus: 10,
    motion: 104
  },
  nightRain: {
    name: "夜雨",
    sunAltitude: -9,
    sunProgress: 50,
    cloudCover: 96,
    rainAmount: 66,
    hazeAmount: 24,
    dustAmount: 0,
    temperature: 11,
    saturation: 86,
    brightness: -3,
    cohesion: 92,
    accentAmount: 50,
    accentFocus: -4,
    motion: 106
  }
};

const state = {
  values: readControls(),
  palette: [],
  manualPalette: null,
  gl: null,
  uniforms: null,
  program: null,
  buffer: null,
  startTime: performance.now(),
  lastSummary: ""
};

const fragmentShaderSource = `
precision highp float;

uniform vec2 uResolution;
uniform float uTime;
uniform vec3 uColor1;
uniform vec3 uColor2;
uniform vec3 uColor3;
uniform vec3 uColor4;

vec2 rotate2d(vec2 v, float a) {
  float s = sin(a);
  float c = cos(a);
  return vec2(c * v.x - s * v.y, s * v.x + c * v.y);
}

vec2 trajectory(float seed, float t, vec2 offset) {
  float a = t * (0.16 + seed * 0.011) + seed;
  float b = t * (0.10 + seed * 0.017) + seed * 1.7;
  return vec2(
    sin(a) * 0.62 + sin(b * 0.73) * 0.24,
    cos(b) * 0.56 + sin(a * 0.61) * 0.22
  ) + offset;
}

vec2 warp(vec2 p, float t) {
  vec2 q = p;
  q += 0.10 * vec2(
    sin(p.y * 2.0 + t * 0.32),
    cos(p.x * 2.1 - t * 0.29)
  );
  q += 0.055 * vec2(
    sin((p.x + p.y) * 3.0 - t * 0.18),
    cos((p.x - p.y) * 2.8 + t * 0.21)
  );
  return q;
}

float blob(vec2 p, vec2 center, float radius) {
  vec2 d = p - center;
  return exp(-dot(d, d) / (radius * radius));
}

vec3 saturateColor(vec3 color) {
  float luma = dot(color, vec3(0.299, 0.587, 0.114));
  return clamp(mix(vec3(luma), color, 1.08), 0.0, 1.0);
}

void main() {
  float minSide = max(1.0, min(uResolution.x, uResolution.y));
  float maxSide = max(uResolution.x, uResolution.y);
  float sceneScale = mix(minSide, maxSide, 0.36);
  vec2 uv = (gl_FragCoord.xy - 0.5 * uResolution.xy) / sceneScale;
  float t = uTime;

  vec2 drift = rotate2d(uv, sin(t * 0.035) * 0.18);
  vec2 q = warp(drift, t);

  vec2 c1 = trajectory(0.4, t, vec2(-0.34, -0.24));
  vec2 c2 = trajectory(2.2, t, vec2(0.36, -0.26));
  vec2 c3 = trajectory(4.0, t, vec2(-0.32, 0.32));
  vec2 c4 = trajectory(5.9, t, vec2(0.34, 0.34));

  float w1 = blob(q, c1, 0.70);
  float w2 = blob(q, c2, 0.74);
  float w3 = blob(q, c3, 0.82);
  float w4 = blob(q, c4, 0.78);

  float mixX = smoothstep(-1.05, 1.05, drift.x + 0.08 * sin(t * 0.07));
  float mixY = smoothstep(-1.10, 1.10, drift.y + 0.08 * cos(t * 0.06));
  vec3 top = mix(uColor1, uColor2, mixX);
  vec3 bottom = mix(uColor3, uColor4, mixX);
  vec3 base = mix(top, bottom, mixY);

  float total = w1 + w2 + w3 + w4 + 0.0001;
  vec3 blobs = (
    w1 * uColor1 +
    w2 * uColor2 +
    w3 * uColor3 +
    w4 * uColor4
  ) / total;

  vec3 color = mix(base, blobs, 0.78);
  float glow = smoothstep(1.45, 0.18, length(uv));
  color *= mix(0.96, 1.06, glow);
  color = saturateColor(color);

  gl_FragColor = vec4(color, 1.0);
}
`;

const vertexShaderSource = `
attribute vec2 aPosition;
void main() {
  gl_Position = vec4(aPosition, 0.0, 1.0);
}
`;

function $(id) {
  return document.getElementById(id);
}

function clamp(value, min, max) {
  return Math.min(max, Math.max(min, value));
}

function smoothstep(edge0, edge1, x) {
  const t = clamp((x - edge0) / (edge1 - edge0), 0, 1);
  return t * t * (3 - 2 * t);
}

function gaussian(x, center, width) {
  const d = (x - center) / width;
  return Math.exp(-(d * d));
}

function lerp(a, b, t) {
  return a + (b - a) * t;
}

function lerpHue(a, b, t) {
  let d = ((b - a + 540) % 360) - 180;
  return (a + d * t + 360) % 360;
}

function averageHue(colors, weights) {
  let x = 0;
  let y = 0;

  colors.forEach((color, index) => {
    const weight = weights[index] || 0;
    x += Math.cos(color.h * Math.PI / 180) * weight;
    y += Math.sin(color.h * Math.PI / 180) * weight;
  });

  return (Math.atan2(y, x) * 180 / Math.PI + 360) % 360;
}

function hueDistance(a, b) {
  return Math.abs(((b - a + 540) % 360) - 180);
}

function tuneColor(color, saturation, brightness, hueShift = 0) {
  return {
    l: clamp(color.l + brightness, 0.08, 0.96),
    c: clamp(color.c * saturation, 0.0, 0.24),
    h: (color.h + hueShift + 360) % 360
  };
}

function oklchToHex(color) {
  const hr = color.h * Math.PI / 180;
  const a = Math.cos(hr) * color.c;
  const b = Math.sin(hr) * color.c;

  const l_ = color.l + 0.3963377774 * a + 0.2158037573 * b;
  const m_ = color.l - 0.1055613458 * a - 0.0638541728 * b;
  const s_ = color.l - 0.0894841775 * a - 1.2914855480 * b;

  const l3 = l_ * l_ * l_;
  const m3 = m_ * m_ * m_;
  const s3 = s_ * s_ * s_;

  const linear = {
    r: +4.0767416621 * l3 - 3.3077115913 * m3 + 0.2309699292 * s3,
    g: -1.2684380046 * l3 + 2.6097574011 * m3 - 0.3413193965 * s3,
    b: -0.0041960863 * l3 - 0.7034186147 * m3 + 1.7076147010 * s3
  };

  const encode = (value) => {
    const x = clamp(value, 0, 1);
    return x <= 0.0031308 ? 12.92 * x : 1.055 * Math.pow(x, 1 / 2.4) - 0.055;
  };

  return "#" + [linear.r, linear.g, linear.b]
    .map((value) => Math.round(encode(value) * 255).toString(16).padStart(2, "0"))
    .join("")
    .toUpperCase();
}

function hexToRgb(hex) {
  const clean = hex.replace("#", "");
  return {
    r: parseInt(clean.slice(0, 2), 16) / 255,
    g: parseInt(clean.slice(2, 4), 16) / 255,
    b: parseInt(clean.slice(4, 6), 16) / 255
  };
}

function hexToOklch(hex) {
  const rgb = hexToRgb(hex);
  const decode = (value) => value <= 0.04045
    ? value / 12.92
    : Math.pow((value + 0.055) / 1.055, 2.4);

  const r = decode(rgb.r);
  const g = decode(rgb.g);
  const b = decode(rgb.b);

  const l = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b);
  const m = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b);
  const s = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b);

  const labL = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s;
  const labA = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s;
  const labB = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s;

  return {
    l: labL,
    c: Math.sqrt(labA * labA + labB * labB),
    h: (Math.atan2(labB, labA) * 180 / Math.PI + 360) % 360
  };
}

function hexToRgba(hex, alpha) {
  const rgb = hexToRgb(hex);
  return `rgba(${Math.round(rgb.r * 255)}, ${Math.round(rgb.g * 255)}, ${Math.round(rgb.b * 255)}, ${alpha})`;
}

function weightedOklch(colors, weights) {
  const totalWeight = weights.reduce((sum, weight) => sum + weight, 0) || 1;
  let l = 0;
  let c = 0;

  colors.forEach((color, index) => {
    const weight = weights[index] || 0;
    l += color.l * weight;
    c += color.c * weight;
  });

  return {
    l: l / totalWeight,
    c: c / totalWeight,
    h: averageHue(
      colors,
      weights.map((weight, index) => weight * Math.max(colors[index]?.c || 0, 0.006))
    )
  };
}

function computeMaterialColors(palette, values = {}) {
  const atmosphere = weightedOklch(palette, [0.42, 0.35, 0.23, 0]);
  const averageTone = atmosphere.l;
  const backgroundTones = palette.slice(0, 3).map((color) => color.l);
  const minTone = Math.min(...backgroundTones);
  const maxTone = Math.max(...backgroundTones);
  const sunAltitude = Number.isFinite(values.sunAltitude)
    ? values.sunAltitude
    : lerp(-10, 42, clamp((averageTone - 0.18) / 0.60, 0, 1));
  const dayAmount = smoothstep(4, 22, sunAltitude);
  const nightAmount = 1 - smoothstep(-10, 2, sunAltitude);
  const twilightAmount = smoothstep(-8, 4, sunAltitude) * (1 - smoothstep(10, 24, sunAltitude));
  const useDarkCards = smoothstep(-2, 8, sunAltitude) < 0.45;
  const warmLowLightAnchor = palette[3].l < 0.70 &&
    palette[3].c > atmosphere.c * 1.55 &&
    hueDistance(atmosphere.h, palette[3].h) > 70;
  const materialHue = warmLowLightAnchor
    ? lerpHue(atmosphere.h, palette[3].h, 0.82)
    : atmosphere.h;
  const surfaceChroma = clamp(atmosphere.c * 0.14, 0.0025, 0.009);
  const targetChroma = clamp(atmosphere.c * 1.10 + palette[3].c * 0.18, 0.018, 0.052);
  const elevatedTone = useDarkCards ? 0.280 : 0.905;
  const cardIsDark = useDarkCards;
  const outlineTone = cardIsDark
    ? clamp(elevatedTone + 0.19, 0.42, 0.72)
    : clamp(elevatedTone - 0.19, 0.45, 0.80);

  const card = { l: elevatedTone, c: surfaceChroma, h: materialHue };
  const cardElevated = { l: elevatedTone, c: surfaceChroma * 1.05, h: materialHue };
  const cardOutline = { l: outlineTone, c: surfaceChroma * 1.12, h: materialHue };
  const onCard = {
    l: cardIsDark ? 0.93 : 0.16,
    c: clamp(surfaceChroma * 0.45, 0.0015, 0.006),
    h: materialHue
  };
  const onCardVariant = {
    l: cardIsDark ? 0.76 : 0.38,
    c: clamp(surfaceChroma * 0.62, 0.002, 0.008),
    h: materialHue
  };
  const harmonizeTarget = {
    l: useDarkCards ? 0.68 : 0.58,
    c: targetChroma,
    h: materialHue
  };

  const cardHex = oklchToHex(card);
  const cardElevatedHex = oklchToHex(cardElevated);
  const cardOutlineHex = oklchToHex(cardOutline);
  const onCardHex = oklchToHex(onCard);

  return {
    backgroundTone: Math.round(averageTone * 100),
    backgroundToneRange: [Math.round(minTone * 100), Math.round(maxTone * 100)],
    sunSurface: {
      altitude: Math.round(sunAltitude),
      day: Math.round(dayAmount * 100),
      twilight: Math.round(twilightAmount * 100),
      night: Math.round(nightAmount * 100)
    },
    darkBackground: useDarkCards,
    surfaceStrategy: useDarkCards ? "solar-dark-surface" : "solar-light-surface",
    surfaceToneGap: Math.round((useDarkCards ? minTone - elevatedTone : elevatedTone - maxTone) * 100),
    harmonizeTarget: oklchToHex(harmonizeTarget),
    card: cardHex,
    cardElevated: cardElevatedHex,
    cardOutline: cardOutlineHex,
    onCard: onCardHex,
    onCardVariant: oklchToHex(onCardVariant),
    contentBlend: useDarkCards ? "normal" : "multiply",
    preview: {
      card: cardElevatedHex,
      onCard: onCardHex
    }
  };
}

function applyMaterialPreview(material) {
  document.documentElement.style.setProperty("--preview-card-bg", material.preview.card);
  document.documentElement.style.setProperty("--preview-card-on", material.preview.onCard);
  document.documentElement.style.setProperty("--preview-content-blend", material.contentBlend);
}

function weightedPalette(entries) {
  const totalWeight = entries.reduce((sum, entry) => sum + entry.weight, 0) || 1;

  return [0, 1, 2, 3].map((index) => {
    let l = 0;
    let c = 0;
    let x = 0;
    let y = 0;

    entries.forEach((entry) => {
      const color = entry.palette[index];
      l += color.l * entry.weight;
      c += color.c * entry.weight;
      x += Math.cos(color.h * Math.PI / 180) * entry.weight;
      y += Math.sin(color.h * Math.PI / 180) * entry.weight;
    });

    return {
      l: l / totalWeight,
      c: c / totalWeight,
      h: (Math.atan2(y, x) * 180 / Math.PI + 360) % 360
    };
  });
}

function readControls() {
  return Object.fromEntries(controls.map((id) => [id, Number($(id).value)]));
}

function mixOklch(a, b, t) {
  const amount = clamp(t, 0, 1);
  return {
    l: lerp(a.l, b.l, amount),
    c: lerp(a.c, b.c, amount),
    h: lerpHue(a.h, b.h, amount)
  };
}

function mixPalette(a, b, t) {
  return a.map((color, index) => mixOklch(color, b[index], t));
}

function weightedPaletteByWeights(entries) {
  const totalWeight = entries.reduce((sum, entry) => sum + entry.weight, 0) || 1;

  return [0, 1, 2, 3].map((index) => {
    let l = 0;
    let c = 0;
    let x = 0;
    let y = 0;

    entries.forEach((entry) => {
      const color = entry.palette[index];
      l += color.l * entry.weight;
      c += color.c * entry.weight;
      x += Math.cos(color.h * Math.PI / 180) * entry.weight;
      y += Math.sin(color.h * Math.PI / 180) * entry.weight;
    });

    return {
      l: l / totalWeight,
      c: c / totalWeight,
      h: (Math.atan2(y, x) * 180 / Math.PI + 360) % 360
    };
  });
}

const solarPalettes = {
  night: [
    { l: 0.28, c: 0.032, h: 252 },
    { l: 0.23, c: 0.034, h: 234 },
    { l: 0.19, c: 0.030, h: 214 },
    { l: 0.66, c: 0.060, h: 246 }
  ],
  sunrise: [
    { l: 0.82, c: 0.028, h: 242 },
    { l: 0.74, c: 0.036, h: 226 },
    { l: 0.61, c: 0.042, h: 310 },
    { l: 0.82, c: 0.072, h: 56 }
  ],
  morning: [
    { l: 0.89, c: 0.035, h: 224 },
    { l: 0.82, c: 0.043, h: 212 },
    { l: 0.70, c: 0.044, h: 220 },
    { l: 0.85, c: 0.066, h: 82 }
  ],
  noon: [
    { l: 0.88, c: 0.036, h: 226 },
    { l: 0.80, c: 0.046, h: 214 },
    { l: 0.68, c: 0.050, h: 218 },
    { l: 0.89, c: 0.048, h: 76 }
  ],
  sunset: [
    { l: 0.76, c: 0.030, h: 252 },
    { l: 0.68, c: 0.040, h: 300 },
    { l: 0.55, c: 0.058, h: 30 },
    { l: 0.76, c: 0.092, h: 64 }
  ]
};

const weatherPalettes = {
  overcast: [
    { l: 0.72, c: 0.007, h: 224 },
    { l: 0.64, c: 0.010, h: 214 },
    { l: 0.50, c: 0.012, h: 204 },
    { l: 0.65, c: 0.020, h: 194 }
  ],
  rain: [
    { l: 0.46, c: 0.024, h: 236 },
    { l: 0.36, c: 0.028, h: 221 },
    { l: 0.27, c: 0.032, h: 206 },
    { l: 0.60, c: 0.054, h: 194 }
  ],
  nightRain: [
    { l: 0.24, c: 0.034, h: 258 },
    { l: 0.20, c: 0.032, h: 238 },
    { l: 0.16, c: 0.030, h: 214 },
    { l: 0.62, c: 0.064, h: 204 }
  ],
  storm: [
    { l: 0.48, c: 0.016, h: 252 },
    { l: 0.40, c: 0.018, h: 238 },
    { l: 0.29, c: 0.020, h: 224 },
    { l: 0.66, c: 0.038, h: 284 }
  ],
  stormDusk: [
    { l: 0.54, c: 0.040, h: 60 },
    { l: 0.43, c: 0.050, h: 50 },
    { l: 0.28, c: 0.048, h: 38 },
    { l: 0.66, c: 0.090, h: 48 }
  ],
  mist: [
    { l: 0.82, c: 0.004, h: 214 },
    { l: 0.75, c: 0.005, h: 206 },
    { l: 0.66, c: 0.006, h: 196 },
    { l: 0.72, c: 0.014, h: 146 }
  ],
  snow: [
    { l: 0.89, c: 0.008, h: 226 },
    { l: 0.81, c: 0.012, h: 215 },
    { l: 0.71, c: 0.016, h: 204 },
    { l: 0.90, c: 0.020, h: 188 }
  ],
  snowNight: [
    { l: 0.34, c: 0.020, h: 248 },
    { l: 0.28, c: 0.022, h: 232 },
    { l: 0.20, c: 0.020, h: 214 },
    { l: 0.62, c: 0.032, h: 206 }
  ],
  freezingRain: [
    { l: 0.58, c: 0.014, h: 224 },
    { l: 0.48, c: 0.018, h: 214 },
    { l: 0.34, c: 0.020, h: 204 },
    { l: 0.72, c: 0.030, h: 190 }
  ],
  dust: [
    { l: 0.74, c: 0.016, h: 84 },
    { l: 0.66, c: 0.022, h: 72 },
    { l: 0.52, c: 0.028, h: 60 },
    { l: 0.74, c: 0.038, h: 50 }
  ],
  sandstorm: [
    { l: 0.56, c: 0.024, h: 78 },
    { l: 0.45, c: 0.032, h: 64 },
    { l: 0.32, c: 0.036, h: 50 },
    { l: 0.60, c: 0.052, h: 42 }
  ]
};

function solarBasePalette(values) {
  const progress = Number.isFinite(values.sunProgress) ? values.sunProgress / 100 : 0.5;
  const day = smoothstep(-4, 14, values.sunAltitude);
  const night = 1 - smoothstep(-8, 4, values.sunAltitude);
  const lowSun = gaussian(values.sunAltitude, 4, 10) * day;
  const sunrise = lowSun * (1 - smoothstep(0.18, 0.50, progress));
  const sunset = lowSun * smoothstep(0.50, 0.82, progress) * 1.08;
  const morning = day * gaussian(progress, 0.20, 0.20) * (1 - lowSun * 0.45);
  const noon = day * gaussian(progress, 0.50, 0.28) * (1 - lowSun * 0.28);
  const afternoon = day * gaussian(progress, 0.78, 0.22) * (1 - lowSun * 0.36);

  return weightedPaletteByWeights([
    { palette: solarPalettes.night, weight: night },
    { palette: solarPalettes.sunrise, weight: sunrise },
    { palette: solarPalettes.morning, weight: morning },
    { palette: solarPalettes.noon, weight: noon + day * 0.12 },
    { palette: solarPalettes.sunset, weight: sunset + afternoon * 0.18 }
  ]);
}

function applyCloudLayer(palette, values) {
  const cloud = values.cloudCover / 100;
  const overcast = smoothstep(74, 100, values.cloudCover);
  const cloudAmount = smoothstep(18, 72, values.cloudCover) * (1 - overcast * 0.58);
  const cloudLight = lerp(0.84, 0.74, overcast);
  const cloudHue = averageHue([palette[0], palette[1]], [0.55, 0.45]);
  const cloudTarget = [
    { l: lerp(palette[0].l, 0.86, 0.36), c: palette[0].c * 0.72, h: lerpHue(palette[0].h, cloudHue, 0.25) },
    { l: lerp(palette[1].l, 0.79, 0.40), c: palette[1].c * 0.68, h: lerpHue(palette[1].h, cloudHue, 0.25) },
    { l: cloudLight, c: 0.008 + cloud * 0.006, h: lerpHue(cloudHue, 220, 0.55) },
    { l: lerp(palette[3].l, 0.88, 0.18), c: palette[3].c * lerp(0.86, 0.62, cloudAmount), h: palette[3].h }
  ];

  return mixPalette(
    mixPalette(palette, cloudTarget, cloudAmount * 0.72),
    weatherPalettes.overcast,
    overcast * 0.92
  );
}

function applyWeatherLayer(palette, values) {
  const day = smoothstep(-4, 12, values.sunAltitude);
  const night = 1 - smoothstep(-6, 8, values.sunAltitude);
  const sunProgress = Number.isFinite(values.sunProgress) ? values.sunProgress / 100 : 0.5;
  const sunset = smoothstep(0.50, 0.82, sunProgress);
  const cloud = values.cloudCover / 100;
  const precip = values.rainAmount / 100;
  const haze = values.hazeAmount / 100;
  const dust = values.dustAmount / 100;
  const cold = 1 - smoothstep(-2, 4, values.temperature);
  const stormSignal = smoothstep(82, 100, values.rainAmount) * smoothstep(82, 100, values.cloudCover);
  const stormDusk = gaussian(values.sunAltitude, 3, 8) * day * sunset;
  const freezeBand = gaussian(values.temperature, 0, 3.2);
  const snowSignal = precip * cold * (1 - freezeBand * 0.42) * (1 - stormSignal * 0.35);
  const snowNightSignal = snowSignal * night * (0.70 + cloud * 0.30);
  const freezingRainSignal = precip * freezeBand * (0.72 + cloud * 0.28) * (1 - snowNightSignal * 0.35) * (1 - stormSignal * 0.45);
  const rainSignal = precip * (1 - cold * 0.72);
  const sandstormSignal = smoothstep(64, 100, values.dustAmount) * (0.62 + cloud * 0.28 + haze * 0.10);
  const dustSignal = dust * (1 - sandstormSignal * 0.55);

  let result = palette;
  result = mixPalette(result, weatherPalettes.mist, haze * (0.56 + cloud * 0.22) * (1 - precip * 0.18) * (1 - dust * 0.55));
  result = mixPalette(result, weatherPalettes.dust, dustSignal * day * (1 - precip * 0.70) * 0.78);
  result = mixPalette(result, weatherPalettes.sandstorm, sandstormSignal * day * (1 - precip * 0.82) * 0.96);
  result = mixPalette(result, weatherPalettes.rain, rainSignal * (0.56 + cloud * 0.30) * (1 - night * 0.32) * (1 - stormSignal * 0.72) * (1 - dust * 0.50));
  result = mixPalette(result, weatherPalettes.nightRain, night * Math.max(rainSignal, cloud * 0.33) * (1 - snowSignal * 0.82) * (1 - freezingRainSignal * 0.45) * (1 - stormSignal * 0.35) * (1 - dust * 0.45));
  result = mixPalette(result, weatherPalettes.snow, snowSignal * (0.72 + cloud * 0.22) * day * (1 - dust * 0.70));
  result = mixPalette(result, weatherPalettes.snowNight, snowNightSignal * (1 - dust * 0.70));
  result = mixPalette(result, weatherPalettes.freezingRain, freezingRainSignal * (1 - dust * 0.60));
  result = mixPalette(result, weatherPalettes.storm, stormSignal * (0.62 + rainSignal * 0.22) * (1 - stormDusk * 0.90) * (1 - cold * 0.80) * (1 - dust * 0.60));
  result = mixPalette(result, weatherPalettes.stormDusk, stormSignal * stormDusk * (0.92 + rainSignal * 0.08) * (1 - cold * 0.80) * (1 - dust * 0.60));
  return result;
}

function generatePalette(values) {
  let palette = applyWeatherLayer(applyCloudLayer(solarBasePalette(values), values), values);

  const warm = smoothstep(16, 36, values.temperature);
  const cold = 1 - smoothstep(-6, 14, values.temperature);
  const sat = values.saturation / 100;
  const bright = values.brightness / 100;
  const cohesion = values.cohesion / 100;
  const contrast = values.accentAmount / 100;
  const hueBias = values.accentFocus;

  const atmosphereHue = averageHue(palette, [0.36, 0.34, 0.30, 0.0]);
  const lightHue = lerpHue(palette[3].h, atmosphereHue, (1 - cohesion) * 0.18);

  palette = palette.map((color, index) => {
    const depth = [-0.035, 0.005, -0.065, 0.045][index] * contrast;
    const chromaLift = [0.95, 1.00, 1.08, 1.12][index];
    const huePull = index < 3 ? (1 - cohesion) * 0.62 : 0;
    const targetHue = index < 3 ? lerpHue(color.h, atmosphereHue, huePull) : lightHue;
    const hueShift = hueBias + warm * -6 + cold * 8 + (index === 3 ? warm * -5 : 0);
    const brightnessShift = bright + depth + (index === 0 ? 0.035 * (cohesion - 0.7) : 0);
    const saturationScale = sat * chromaLift * lerp(0.92, 1.02, cohesion);
    return tuneColor(
      { l: color.l, c: color.c, h: targetHue },
      saturationScale,
      brightnessShift,
      hueShift
    );
  });

  const atmosphereAverage = (palette[0].l + palette[1].l + palette[2].l) / 3;
  palette[3] = {
    l: clamp(lerp(palette[3].l, atmosphereAverage + 0.08, 0.20 * cohesion), 0.14, 0.94),
    c: clamp(palette[3].c, 0, 0.22),
    h: palette[3].h
  };

  return {
    colors: palette.map(oklchToHex),
    oklch: palette
  };
}

function renderSwatches(colors) {
  const root = $("swatches");
  root.innerHTML = "";

  colors.forEach((hex, index) => {
    const row = document.createElement("div");
    row.className = "swatch";
    row.innerHTML = `
      <div class="swatch-chip" style="background:${hex}"></div>
      <div><b>${swatchMeta[index].label}</b><span>${hex}</span></div>
      <input type="color" value="${hex}" aria-label="${swatchMeta[index].label}">
    `;

    row.querySelector("input").addEventListener("input", (event) => {
      if (!state.manualPalette) state.manualPalette = state.palette.slice();
      state.manualPalette[index] = event.target.value.toUpperCase();
      $("lockManual").checked = true;
      state.palette = state.manualPalette.slice();
      renderSwatches(state.palette);
      updateReadout();
    });

    root.appendChild(row);
  });
}

function conditionText(values) {
  if (values.dustAmount > 78) return "沙尘暴，干暖灰黄压低";
  if (values.dustAmount > 35) return "浮尘，暖灰洗淡天空";
  if (values.rainAmount > 86 && values.cloudCover > 88 && values.sunAltitude > -2 && values.sunAltitude < 10 && values.sunProgress > 50) return "黄昏雷雨，赭黄云底";
  if (values.rainAmount > 86 && values.cloudCover > 88 && values.temperature > 4) return "强对流，暗部加深";
  if (values.rainAmount > 45 && values.temperature >= -2 && values.temperature <= 2) return "冻雨，冷湿玻璃感";
  if (values.sunAltitude < 0 && values.rainAmount > 35 && values.temperature <= 2) return "夜雪，暗蓝托冷白";
  if (values.rainAmount > 45 && values.temperature <= 2) return "雪，冷白压低色度";
  if (values.sunAltitude < 0 && values.rainAmount > 45) return "夜雨，局部电青";
  if (values.hazeAmount > 70) return "雾重，颜色洗淡";
  if (values.rainAmount > 55) return "雨天，冷色压低";
  if (values.cloudCover > 78) return "阴天，灰蓝主导";
  if (values.cloudCover > 30) return "多云，蓝天白云";
  if (values.sunAltitude < 10 && values.sunProgress <= 50) return "日出低太阳高度，粉蓝暖光";
  if (values.sunAltitude < 10) return "日落低太阳高度，暖光抬起";
  if (values.sunAltitude > 42) return "高太阳，色温更干净";
  return "晴，三色同族";
}

function updateOutputs(values) {
  const units = {
    sunAltitude: "°",
    sunProgress: "%",
    cloudCover: "%",
    rainAmount: "%",
    hazeAmount: "%",
    dustAmount: "%",
    temperature: "°",
    saturation: "%",
    brightness: "",
    cohesion: "%",
    accentAmount: "%",
    accentFocus: "°",
    motion: "%"
  };

  controls.forEach((id) => {
    document.querySelector(`output[for="${id}"]`).textContent = `${values[id]}${units[id]}`;
  });

  $("mockScene").textContent = presets[$("preset").value]?.name || "自定义";
  $("mockCondition").textContent = conditionText(values);
  $("mockTemp").textContent = `${values.temperature}°`;
  $("mockCloud").textContent = `${values.cloudCover}%`;
  $("mockRain").textContent = `${values.rainAmount}%`;
  $("mockHaze").textContent = `${values.hazeAmount}%`;
  $("mockDust").textContent = `${values.dustAmount}%`;
  $("mockSummary").textContent = values.accentAmount > 55
    ? "层次拉开，色场更有天气重量"
    : "层次收住，适合轻量和雾感场景";
}

function updateReadout() {
  const paletteModel = state.palette.map(hexToOklch);
  const material = computeMaterialColors(paletteModel, state.values);
  applyMaterialPreview(material);

  const payload = {
    weather: {
      sunAltitude: state.values.sunAltitude,
      sunProgress: state.values.sunProgress,
      cloudCover: state.values.cloudCover,
      rainAmount: state.values.rainAmount,
      hazeAmount: state.values.hazeAmount,
      dustAmount: state.values.dustAmount,
      temperature: state.values.temperature
    },
    tuning: {
      saturation: state.values.saturation,
      brightness: state.values.brightness,
      cohesion: state.values.cohesion,
      contrast: state.values.accentAmount,
      hueShift: state.values.accentFocus,
      motion: state.values.motion
    },
    palette: {
      skyA: state.palette[0],
      skyB: state.palette[1],
      massC: state.palette[2],
      lightD: state.palette[3]
    },
    material
  };

  $("paletteJson").textContent = JSON.stringify(payload, null, 2);
}

function updatePalette() {
  state.values = readControls();
  updateOutputs(state.values);

  if ($("lockManual").checked && state.manualPalette) {
    state.palette = state.manualPalette.slice();
  } else {
    const generated = generatePalette(state.values);
    state.palette = generated.colors;
    state.manualPalette = null;
  }

  renderSwatches(state.palette);
  updateReadout();
}

function applyPreset(key) {
  const preset = presets[key];
  controls.forEach((id) => {
    $(id).value = preset[id];
  });
  $("lockManual").checked = false;
  state.manualPalette = null;
  updatePalette();
}

function compileShader(gl, type, source) {
  const shader = gl.createShader(type);
  gl.shaderSource(shader, source);
  gl.compileShader(shader);
  if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
    const message = gl.getShaderInfoLog(shader);
    gl.deleteShader(shader);
    throw new Error(message || "shader compile failed");
  }
  return shader;
}

function createProgram(gl) {
  const vertexShader = compileShader(gl, gl.VERTEX_SHADER, vertexShaderSource);
  const fragmentShader = compileShader(gl, gl.FRAGMENT_SHADER, fragmentShaderSource);
  const program = gl.createProgram();

  gl.attachShader(program, vertexShader);
  gl.attachShader(program, fragmentShader);
  gl.linkProgram(program);

  if (!gl.getProgramParameter(program, gl.LINK_STATUS)) {
    const message = gl.getProgramInfoLog(program);
    gl.deleteProgram(program);
    throw new Error(message || "program link failed");
  }

  gl.deleteShader(vertexShader);
  gl.deleteShader(fragmentShader);
  return program;
}

function setupWebGL() {
  const canvas = $("glCanvas");
  const gl = canvas.getContext("webgl");
  if (!gl) {
    throw new Error("WebGL unavailable");
  }

  const program = createProgram(gl);
  const buffer = gl.createBuffer();
  gl.bindBuffer(gl.ARRAY_BUFFER, buffer);
  gl.bufferData(
    gl.ARRAY_BUFFER,
    new Float32Array([
      -1, -1,
      1, -1,
      -1, 1,
      1, 1
    ]),
    gl.STATIC_DRAW
  );

  const position = gl.getAttribLocation(program, "aPosition");
  const uniforms = {
    resolution: gl.getUniformLocation(program, "uResolution"),
    time: gl.getUniformLocation(program, "uTime"),
    colors: [
      gl.getUniformLocation(program, "uColor1"),
      gl.getUniformLocation(program, "uColor2"),
      gl.getUniformLocation(program, "uColor3"),
      gl.getUniformLocation(program, "uColor4")
    ]
  };

  state.gl = gl;
  state.program = program;
  state.buffer = buffer;
  state.uniforms = uniforms;

  gl.useProgram(program);
  gl.enableVertexAttribArray(position);
  gl.vertexAttribPointer(position, 2, gl.FLOAT, false, 0, 0);
}

function resizeCanvas() {
  const canvas = $("glCanvas");
  const rect = canvas.getBoundingClientRect();
  const dpr = Math.min(window.devicePixelRatio || 1, 2);
  const width = Math.max(1, Math.floor(rect.width * dpr));
  const height = Math.max(1, Math.floor(rect.height * dpr));

  if (canvas.width !== width || canvas.height !== height) {
    canvas.width = width;
    canvas.height = height;
  }

  return { width, height };
}

function renderFrame(now) {
  const gl = state.gl;
  const { width, height } = resizeCanvas();
  gl.viewport(0, 0, width, height);
  gl.clearColor(0, 0, 0, 1);
  gl.clear(gl.COLOR_BUFFER_BIT);

  gl.useProgram(state.program);
  gl.uniform2f(state.uniforms.resolution, width, height);

  const motionScale = state.values.motion / 68;
  gl.uniform1f(state.uniforms.time, ((now - state.startTime) / 1000) * motionScale);

  state.palette.forEach((hex, index) => {
    const rgb = hexToRgb(hex);
    gl.uniform3f(state.uniforms.colors[index], rgb.r, rgb.g, rgb.b);
  });

  gl.drawArrays(gl.TRIANGLE_STRIP, 0, 4);
  requestAnimationFrame(renderFrame);
}

function bindEvents() {
  controls.forEach((id) => {
    $(id).addEventListener("input", () => {
      state.manualPalette = null;
      $("lockManual").checked = false;
      updatePalette();
    });
  });

  $("preset").addEventListener("change", (event) => {
    applyPreset(event.target.value);
  });

  $("lockManual").addEventListener("change", (event) => {
    if (event.target.checked) {
      state.manualPalette = state.palette.slice();
    } else {
      state.manualPalette = null;
      updatePalette();
    }
  });

  $("regenerate").addEventListener("click", () => {
    $("lockManual").checked = false;
    state.manualPalette = null;
    updatePalette();
  });

  $("copyJson").addEventListener("click", async () => {
    try {
      await navigator.clipboard.writeText($("paletteJson").textContent);
      $("copyJson").textContent = "已复制";
    } catch (error) {
      $("copyJson").textContent = "复制受限";
    }

    window.setTimeout(() => {
      $("copyJson").textContent = "复制 JSON";
    }, 900);
  });

  window.addEventListener("resize", () => resizeCanvas());
}

function boot() {
  try {
    setupWebGL();
  } catch (error) {
    $("paletteJson").textContent = `WebGL 初始化失败: ${error.message}`;
    return;
  }

  bindEvents();
  applyPreset("clearMorning");
  requestAnimationFrame(renderFrame);
}

boot();
