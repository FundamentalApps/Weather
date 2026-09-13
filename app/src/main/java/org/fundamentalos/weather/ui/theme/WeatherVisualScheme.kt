package org.fundamentalos.weather.ui.theme

import androidx.compose.runtime.Immutable
import org.fundamentalos.weather.ui.sky.SkyState
import org.fundamentalos.weather.ui.componets.moonIlluminationFraction
import androidx.compose.ui.graphics.Color
import org.fundamentalos.weather.weather.domain.CurrentWeather
import org.fundamentalos.weather.weather.domain.DailyForecast
import kotlinx.datetime.toKotlinLocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

@Immutable
data class WeatherVisualScheme(
    val backgroundColors: List<Color>,
    val card: Color,
    val onCard: Color,
    val onCardVariant: Color,
    val harmonizeTarget: Color,
    val backgroundScrim: Color,
    val useDarkCards: Boolean,
    val sky: SkyState = SkyState(),
)

private data class WeatherVisualInputs(
    val sunAltitude: Double,
    val sunProgress: Double,
    val cloudCover: Double,
    val rainAmount: Double,
    val hazeAmount: Double,
    val dustAmount: Double,
    val temperature: Double,
    val saturation: Double,
    val brightness: Double,
    val cohesion: Double,
    val accentAmount: Double,
    val accentFocus: Double,
)

internal data class SolarPosition(
    val altitude: Double,
    val progress: Double,
)

private data class Oklch(
    val l: Double,
    val c: Double,
    val h: Double,
)

fun weatherVisualScheme(
    current: CurrentWeather?,
    dailyForecast: List<DailyForecast>,
    latitude: Double? = null,
    longitude: Double? = null,
    now: OffsetDateTime = OffsetDateTime.now(),
): WeatherVisualScheme {
    val inputs = current?.toWeatherVisualInputs(dailyForecast, latitude, longitude, now) ?: WeatherVisualInputs(
        sunAltitude = 12.0,
        sunProgress = 0.18,
        cloudCover = 10.0,
        rainAmount = 0.0,
        hazeAmount = 0.0,
        dustAmount = 0.0,
        temperature = 18.0,
        saturation = 92.0,
        brightness = 0.0,
        cohesion = 92.0,
        accentAmount = 34.0,
        accentFocus = 0.0,
    )
    val palette = generatePalette(inputs)
    val material = computeMaterialColors(palette, inputs)

    return WeatherVisualScheme(
        backgroundColors = palette.map(::oklchToColor),
        card = material.card,
        onCard = material.onCard,
        onCardVariant = material.onCardVariant,
        harmonizeTarget = material.harmonizeTarget,
        backgroundScrim = if (palette.take(3).map { it.l }.average() < 0.50) {
            Color.Black.copy(alpha = 0.04f)
        } else {
            Color.White.copy(alpha = 0.03f)
        },
        useDarkCards = material.useDarkCards,
        sky = SkyState(
            sunAltitude = inputs.sunAltitude.toFloat(),
            sunProgress = inputs.sunProgress.toFloat(),
            cloudCover = (inputs.cloudCover / 100.0).toFloat().coerceIn(0f, 1f),
            precipitation = (inputs.rainAmount / 100.0).toFloat().coerceIn(0f, 1f),
            frozen = if (current?.condition?.iconCode?.let(::isSnow) == true) 1f else 0f,
            haze = (inputs.hazeAmount / 100.0).toFloat().coerceIn(0f, 1f),
            dust = (inputs.dustAmount / 100.0).toFloat().coerceIn(0f, 1f),
            windX = SkyState.wind(current?.windSpeedKph, current?.windDegree).first,
            windY = SkyState.wind(current?.windSpeedKph, current?.windDegree).second,
            cumulus = if (current?.condition?.iconCode in setOf("103", "153", "151", "302", "303", "304")) 1f else 0f,
            storm = if (current?.condition?.iconCode?.let(::isThunderstorm) == true) 1f else 0f,
            moonIllumination = moonIlluminationFraction(now.toLocalDate().toKotlinLocalDate()).toFloat(),
        ),
    )
}

private fun CurrentWeather.toWeatherVisualInputs(
    dailyForecast: List<DailyForecast>,
    latitude: Double?,
    longitude: Double?,
    now: OffsetDateTime,
): WeatherVisualInputs {
    val code = condition.iconCode
    val cloud = visualCloudCover(code, cloudPercent)
    val rain = inferredPrecipitationAmount(code, precipMillimeters)
    val haze = inferredHazeAmount(code, visibilityKm)
    val dust = inferredDustAmount(code)
    val storm = isThunderstorm(code)
    val coldPrecip = isSnow(code) || code == "313" || tempCelsius <= 1 && rain > 24.0
    val sunPosition = estimateSolarPosition(this, dailyForecast, latitude, longitude, now)
    val sunAltitude = sunPosition.altitude
    val night = sunAltitude < 0.0 || isNightIcon(code)

    return WeatherVisualInputs(
        sunAltitude = sunAltitude,
        sunProgress = sunPosition.progress,
        cloudCover = cloud,
        rainAmount = rain,
        hazeAmount = haze,
        dustAmount = dust,
        temperature = if (coldPrecip) min(tempCelsius, 1).toDouble() else tempCelsius.toDouble(),
        saturation = when {
            haze > 60.0 -> 68.0
            dust > 42.0 -> 76.0
            rain > 45.0 || night -> 84.0
            else -> 90.0
        },
        brightness = when {
            night -> -3.0
            storm -> -5.0
            cloud > 80.0 -> -3.0
            haze > 65.0 -> 3.0
            else -> 0.0
        },
        cohesion = when {
            haze > 60.0 || coldPrecip -> 98.0
            storm -> 78.0
            else -> 92.0
        },
        accentAmount = clamp(28.0 + cloud * 0.18 + rain * 0.18 + dust * 0.18 + if (storm) 14.0 else 0.0, 20.0, 58.0),
        accentFocus = when {
            dust > 42.0 -> 8.0
            storm && sunAltitude in -4.0..8.0 && sunPosition.progress > 0.5 -> 10.0
            rain > 28.0 -> -4.0
            tempCelsius <= 0 -> -3.0
            else -> 0.0
        },
    )
}

private data class MaterialColors(
    val card: Color,
    val onCard: Color,
    val onCardVariant: Color,
    val harmonizeTarget: Color,
    val useDarkCards: Boolean,
)

private fun computeMaterialColors(palette: List<Oklch>, values: WeatherVisualInputs): MaterialColors {
    val atmosphere = weightedOklch(palette, listOf(0.42, 0.35, 0.23, 0.0))
    val averageTone = atmosphere.l
    val solarCardDay = smoothstep(-2.0, 8.0, values.sunAltitude)
    val useDarkCards = solarCardDay < 0.45
    val warmLowLightAnchor = palette[3].l < 0.70 &&
        palette[3].c > atmosphere.c * 1.55 &&
        hueDistance(atmosphere.h, palette[3].h) > 70.0
    val materialHue = if (warmLowLightAnchor) lerpHue(atmosphere.h, palette[3].h, 0.82) else atmosphere.h
    val surfaceChroma = clamp(atmosphere.c * 0.14, 0.0025, 0.009)
    val targetChroma = clamp(atmosphere.c * 1.10 + palette[3].c * 0.18, 0.018, 0.052)
    val elevatedTone = if (useDarkCards) 0.280 else 0.905
    val cardIsDark = useDarkCards

    return MaterialColors(
        card = oklchToColor(Oklch(elevatedTone, surfaceChroma * 1.05, materialHue)),
        onCard = oklchToColor(
            Oklch(
                l = if (cardIsDark) 0.93 else 0.16,
                c = clamp(surfaceChroma * 0.45, 0.0015, 0.006),
                h = materialHue,
            )
        ),
        onCardVariant = oklchToColor(
            Oklch(
                l = if (cardIsDark) 0.76 else 0.38,
                c = clamp(surfaceChroma * 0.62, 0.002, 0.008),
                h = materialHue,
            )
        ),
        harmonizeTarget = oklchToColor(
            Oklch(
                l = if (useDarkCards) 0.68 else 0.58,
                c = targetChroma,
                h = materialHue,
            )
        ),
        useDarkCards = useDarkCards,
    )
}

private fun generatePalette(values: WeatherVisualInputs): List<Oklch> {
    var palette = applyWeatherLayer(applyCloudLayer(solarBasePalette(values), values), values)
    val warm = smoothstep(16.0, 36.0, values.temperature)
    val cold = 1.0 - smoothstep(-6.0, 14.0, values.temperature)
    val sat = values.saturation / 100.0
    val bright = values.brightness / 100.0
    val cohesion = values.cohesion / 100.0
    val contrast = values.accentAmount / 100.0
    val atmosphereHue = averageHue(palette, listOf(0.36, 0.34, 0.30, 0.0))
    val lightHue = lerpHue(palette[3].h, atmosphereHue, (1.0 - cohesion) * 0.18)

    palette = palette.mapIndexed { index, color ->
        val depth = listOf(-0.035, 0.005, -0.065, 0.045)[index] * contrast
        val chromaLift = listOf(0.95, 1.00, 1.08, 1.12)[index]
        val huePull = if (index < 3) (1.0 - cohesion) * 0.62 else 0.0
        val targetHue = if (index < 3) lerpHue(color.h, atmosphereHue, huePull) else lightHue
        val hueShift = values.accentFocus + warm * -6.0 + cold * 8.0 + if (index == 3) warm * -5.0 else 0.0
        val brightnessShift = bright + depth + if (index == 0) 0.035 * (cohesion - 0.7) else 0.0
        val saturationScale = sat * chromaLift * lerp(0.92, 1.02, cohesion)
        tuneColor(Oklch(color.l, color.c, targetHue), saturationScale, brightnessShift, hueShift)
    }

    val atmosphereAverage = (palette[0].l + palette[1].l + palette[2].l) / 3.0
    return palette.toMutableList().also {
        it[3] = it[3].copy(
            l = clamp(lerp(it[3].l, atmosphereAverage + 0.08, 0.20 * cohesion), 0.14, 0.94),
            c = clamp(it[3].c, 0.0, 0.22),
        )
    }
}

private fun solarBasePalette(values: WeatherVisualInputs): List<Oklch> {
    val day = smoothstep(-4.0, 14.0, values.sunAltitude)
    val night = 1.0 - smoothstep(-8.0, 4.0, values.sunAltitude)
    val lowSun = gaussian(values.sunAltitude, 4.0, 10.0) * day
    val sunrise = lowSun * (1.0 - smoothstep(0.18, 0.50, values.sunProgress))
    val sunset = lowSun * smoothstep(0.50, 0.82, values.sunProgress) * 1.08
    val morning = day * gaussian(values.sunProgress, 0.20, 0.20) * (1.0 - lowSun * 0.45)
    val noon = day * gaussian(values.sunProgress, 0.50, 0.28) * (1.0 - lowSun * 0.28)
    val afternoon = day * gaussian(values.sunProgress, 0.78, 0.22) * (1.0 - lowSun * 0.36)

    return weightedPalette(
        listOf(
            solarNight to night,
            solarSunrise to sunrise,
            solarMorning to morning,
            solarNoon to noon + day * 0.12,
            solarSunset to sunset + afternoon * 0.18,
        )
    )
}

private fun applyCloudLayer(palette: List<Oklch>, values: WeatherVisualInputs): List<Oklch> {
    val cloud = values.cloudCover / 100.0
    val overcast = smoothstep(74.0, 100.0, values.cloudCover)
    val cloudAmount = smoothstep(18.0, 72.0, values.cloudCover) * (1.0 - overcast * 0.58)
    val cloudLight = lerp(0.84, 0.74, overcast)
    val cloudHue = averageHue(listOf(palette[0], palette[1]), listOf(0.55, 0.45))
    val cloudTarget = listOf(
        Oklch(lerp(palette[0].l, 0.86, 0.36), palette[0].c * 0.72, lerpHue(palette[0].h, cloudHue, 0.25)),
        Oklch(lerp(palette[1].l, 0.79, 0.40), palette[1].c * 0.68, lerpHue(palette[1].h, cloudHue, 0.25)),
        Oklch(cloudLight, 0.008 + cloud * 0.006, lerpHue(cloudHue, 220.0, 0.55)),
        Oklch(lerp(palette[3].l, 0.88, 0.18), palette[3].c * lerp(0.86, 0.62, cloudAmount), palette[3].h),
    )

    return mixPalette(
        mixPalette(palette, cloudTarget, cloudAmount * 0.72),
        overcastPalette,
        overcast * 0.92,
    )
}

private fun applyWeatherLayer(palette: List<Oklch>, values: WeatherVisualInputs): List<Oklch> {
    val day = smoothstep(-4.0, 12.0, values.sunAltitude)
    val night = 1.0 - smoothstep(-6.0, 8.0, values.sunAltitude)
    val sunset = smoothstep(0.50, 0.82, values.sunProgress)
    val cloud = values.cloudCover / 100.0
    val precip = values.rainAmount / 100.0
    val haze = values.hazeAmount / 100.0
    val dust = values.dustAmount / 100.0
    val cold = 1.0 - smoothstep(-2.0, 4.0, values.temperature)
    val stormSignal = smoothstep(82.0, 100.0, values.rainAmount) * smoothstep(82.0, 100.0, values.cloudCover)
    val stormDusk = gaussian(values.sunAltitude, 3.0, 8.0) * day * sunset
    val freezeBand = gaussian(values.temperature, 0.0, 3.2)
    val snowSignal = precip * cold * (1.0 - freezeBand * 0.42) * (1.0 - stormSignal * 0.35)
    val snowNightSignal = snowSignal * night * (0.70 + cloud * 0.30)
    val freezingRainSignal = precip * freezeBand * (0.72 + cloud * 0.28) * (1.0 - snowNightSignal * 0.35) * (1.0 - stormSignal * 0.45)
    val rainSignal = precip * (1.0 - cold * 0.72)
    val sandstormSignal = smoothstep(64.0, 100.0, values.dustAmount) * (0.62 + cloud * 0.28 + haze * 0.10)
    val dustSignal = dust * (1.0 - sandstormSignal * 0.55)

    var result = palette
    result = mixPalette(result, mistPalette, haze * (0.56 + cloud * 0.22) * (1.0 - precip * 0.18) * (1.0 - dust * 0.55))
    result = mixPalette(result, dustPalette, dustSignal * day * (1.0 - precip * 0.70) * 0.78)
    result = mixPalette(result, sandstormPalette, sandstormSignal * day * (1.0 - precip * 0.82) * 0.96)
    result = mixPalette(result, rainPalette, rainSignal * (0.56 + cloud * 0.30) * (1.0 - night * 0.32) * (1.0 - stormSignal * 0.72) * (1.0 - dust * 0.50))
    result = mixPalette(result, nightRainPalette, night * max(rainSignal, cloud * 0.33) * (1.0 - snowSignal * 0.82) * (1.0 - freezingRainSignal * 0.45) * (1.0 - stormSignal * 0.35) * (1.0 - dust * 0.45))
    result = mixPalette(result, snowPalette, snowSignal * (0.72 + cloud * 0.22) * day * (1.0 - dust * 0.70))
    result = mixPalette(result, snowNightPalette, snowNightSignal * (1.0 - dust * 0.70))
    result = mixPalette(result, freezingRainPalette, freezingRainSignal * (1.0 - dust * 0.60))
    result = mixPalette(result, stormPalette, stormSignal * (0.62 + rainSignal * 0.22) * (1.0 - stormDusk * 0.90) * (1.0 - cold * 0.80) * (1.0 - dust * 0.60))
    result = mixPalette(result, stormDuskPalette, stormSignal * stormDusk * (0.92 + rainSignal * 0.08) * (1.0 - cold * 0.80) * (1.0 - dust * 0.60))
    return result
}

private fun mixPalette(a: List<Oklch>, b: List<Oklch>, amount: Double): List<Oklch> {
    val t = clamp(amount, 0.0, 1.0)
    return a.mapIndexed { index, color -> mixOklch(color, b[index], t) }
}

private fun mixOklch(a: Oklch, b: Oklch, amount: Double): Oklch {
    val t = clamp(amount, 0.0, 1.0)
    return Oklch(
        l = lerp(a.l, b.l, t),
        c = lerp(a.c, b.c, t),
        h = lerpHue(a.h, b.h, t),
    )
}

private fun estimateSolarPosition(
    current: CurrentWeather,
    dailyForecast: List<DailyForecast>,
    latitude: Double?,
    longitude: Double?,
    now: OffsetDateTime,
): SolarPosition {
    val today = now.toLocalDate().toKotlinLocalDate()
    val todayForecast = dailyForecast.firstOrNull { it.date == today } ?: dailyForecast.firstOrNull()
    val sunrise = todayForecast?.sunrise?.let(::parseMinutes)
    val sunset = todayForecast?.sunset?.let(::parseMinutes)
    val minutes = now.toLocalTime().hour * 60 + now.toLocalTime().minute
    val progress = daylightProgress(minutes, sunrise, sunset)

    if (latitude != null && longitude != null) {
        return solarPosition(now, latitude, longitude)
    }

    if (sunrise != null && sunset != null && sunset > sunrise) {
        return when {
            minutes in sunrise..sunset -> {
                val daylightProgress = (minutes - sunrise).toDouble() / (sunset - sunrise).toDouble()
                SolarPosition(4.0 + sin(daylightProgress * PI) * 58.0, daylightProgress)
            }
            minutes < sunrise -> SolarPosition(
                lerp(-10.0, 4.0, smoothstep((sunrise - 110).toDouble(), sunrise.toDouble(), minutes.toDouble())),
                0.0,
            )
            else -> SolarPosition(
                lerp(4.0, -10.0, smoothstep(sunset.toDouble(), (sunset + 130).toDouble(), minutes.toDouble())),
                1.0,
            )
        }
    }

    return SolarPosition(fallbackSunAltitude(current, now), fallbackSunProgress(current, now))
}

private fun daylightProgress(minutes: Int, sunrise: Int?, sunset: Int?): Double? {
    if (sunrise == null || sunset == null || sunset <= sunrise) return null
    return when {
        minutes < sunrise -> 0.0
        minutes > sunset -> 1.0
        else -> (minutes - sunrise).toDouble() / (sunset - sunrise).toDouble()
    }
}

private fun fallbackSunAltitude(current: CurrentWeather, now: OffsetDateTime): Double {
    if (current.condition.isDay == false || isNightIcon(current.condition.iconCode)) return -8.0
    val localHour = now.hour
    return when (localHour) {
        in 5..8 -> 12.0
        in 9..15 -> 52.0
        in 16..18 -> 10.0
        else -> -8.0
    }
}

private fun fallbackSunProgress(current: CurrentWeather, now: OffsetDateTime): Double {
    if (current.condition.isDay == false || isNightIcon(current.condition.iconCode)) return 0.5
    return when (now.hour) {
        in 5..8 -> 0.18
        in 9..15 -> 0.50
        in 16..18 -> 0.86
        else -> 0.5
    }
}

internal fun solarPosition(instant: OffsetDateTime, latitude: Double, longitude: Double): SolarPosition {
    // Normalize to UTC so the same instant/location renders identically in any device timezone.
    val time = instant.withOffsetSameInstant(java.time.ZoneOffset.UTC)
    val dayOfYear = time.dayOfYear
    val minutes = time.toLocalTime().hour * 60.0 + time.toLocalTime().minute + time.toLocalTime().second / 60.0
    val hour = minutes / 60.0
    val gamma = 2.0 * PI / 365.0 * (dayOfYear - 1.0 + (hour - 12.0) / 24.0)
    val equationOfTime = 229.18 * (
        0.000075 +
            0.001868 * cos(gamma) -
            0.032077 * sin(gamma) -
            0.014615 * cos(2.0 * gamma) -
            0.040849 * sin(2.0 * gamma)
        )
    val declination =
        0.006918 -
            0.399912 * cos(gamma) +
            0.070257 * sin(gamma) -
            0.006758 * cos(2.0 * gamma) +
            0.000907 * sin(2.0 * gamma) -
            0.002697 * cos(3.0 * gamma) +
            0.00148 * sin(3.0 * gamma)
    val timeZoneHours = time.offset.totalSeconds / 3600.0
    val trueSolarMinutes = ((minutes + equationOfTime + 4.0 * longitude - 60.0 * timeZoneHours) % 1440.0 + 1440.0) % 1440.0
    val hourAngleDegrees = if (trueSolarMinutes / 4.0 < 0.0) {
        trueSolarMinutes / 4.0 + 180.0
    } else {
        trueSolarMinutes / 4.0 - 180.0
    }
    val latitudeRad = latitude.coerceIn(-89.9, 89.9) * PI / 180.0
    val hourAngleRad = hourAngleDegrees * PI / 180.0
    val cosZenith = (
        sin(latitudeRad) * sin(declination) +
            cos(latitudeRad) * cos(declination) * cos(hourAngleRad)
        ).coerceIn(-1.0, 1.0)
    val altitude = 90.0 - kotlin.math.acos(cosZenith) * 180.0 / PI
    val sunriseHourAngle = kotlin.math.acos((-kotlin.math.tan(latitudeRad) * kotlin.math.tan(declination)).coerceIn(-1.0, 1.0)) * 180.0 / PI
    val progress = if (sunriseHourAngle > .001) {
        ((hourAngleDegrees + sunriseHourAngle) / (2.0 * sunriseHourAngle)).coerceIn(0.0, 1.0)
    } else .5
    return SolarPosition(altitude, progress)
}

private fun parseMinutes(value: String): Int? {
    return runCatching {
        val time = LocalTime.parse(value)
        time.hour * 60 + time.minute
    }.getOrNull()
}

private fun visualCloudCover(code: String, observedPercent: Int?): Double {
    val inferred = inferredCloudCover(code)
    val observed = observedPercent?.toDouble()
    val measured = observed ?: inferred

    // Weather text/icon expresses the sky state, while the numeric cloud field is only
    // total covered area. Keep visual sky cover inside the meteorological band for
    // clear/few/cloudy/overcast so "cloudy" does not turn into an overcast gray sky.
    return when (code) {
        "100", "150" -> lerp(6.0, clamp(measured, 0.0, 10.0), 0.45)
        "102", "152" -> lerp(22.0, clamp(measured, 10.0, 30.0), 0.55)
        "103", "153" -> lerp(42.0, clamp(measured, 30.0, 55.0), 0.55)
        "101", "151" -> lerp(58.0, clamp(measured, 30.0, 70.0), 0.35)
        "104" -> lerp(92.0, clamp(measured, 80.0, 100.0), 0.65)
        else -> measured
    }
}

private fun inferredCloudCover(code: String): Double = when (code) {
    "100", "150" -> 8.0
    "101", "151" -> 58.0
    "102", "152", "103", "153" -> 34.0
    "104" -> 92.0
    in rainCodes, in snowCodes -> 88.0
    in hazeCodes -> 70.0
    in dustCodes -> 78.0
    else -> 42.0
}

private fun inferredPrecipitationAmount(code: String, precipMillimeters: Double?): Double {
    val byCode = when (code) {
        "302", "303", "304" -> 92.0
        "300", "301", "350", "351" -> 76.0
        "305", "309" -> 46.0
        "306", "314", "399" -> 62.0
        "307", "315", "316" -> 78.0
        "308", "310", "311", "312", "317", "318" -> 92.0
        "313" -> 72.0
        in snowCodes -> 70.0
        else -> 0.0
    }
    val byAmount = ((precipMillimeters ?: 0.0) * 36.0).coerceAtMost(100.0)
    return max(byCode, byAmount)
}

private fun inferredHazeAmount(code: String, visibilityKm: Int?): Double {
    val byCode = when (code) {
        "500", "501", "509" -> 68.0
        "502", "511" -> 74.0
        "512", "513" -> 90.0
        "510", "514", "515" -> 94.0
        else -> 0.0
    }
    val byVisibility = visibilityKm?.let { clamp((16.0 - it.toDouble()) / 16.0 * 80.0, 0.0, 80.0) } ?: 0.0
    return max(byCode, byVisibility)
}

private fun inferredDustAmount(code: String): Double = when (code) {
    "503", "507" -> 82.0
    "508" -> 100.0
    "504" -> 58.0
    else -> 0.0
}

private fun isThunderstorm(code: String): Boolean = code in setOf("302", "303", "304")
private fun isSnow(code: String): Boolean = code in snowCodes
private fun isNightIcon(code: String): Boolean = code in setOf("150", "151", "152", "153", "350", "351", "456", "457")

private val rainCodes = setOf(
    "300", "301", "302", "303", "304", "305", "306", "307", "308", "309", "310", "311", "312",
    "313", "314", "315", "316", "317", "318", "350", "351", "399",
)
private val snowCodes = setOf("400", "401", "402", "403", "404", "405", "406", "407", "408", "409", "410", "456", "457", "499")
private val hazeCodes = setOf("500", "501", "502", "509", "510", "511", "512", "513", "514", "515")
private val dustCodes = setOf("503", "504", "507", "508")

private fun weightedPalette(entries: List<Pair<List<Oklch>, Double>>): List<Oklch> {
    val totalWeight = entries.sumOf { it.second }.takeIf { it > 0.0 } ?: 1.0
    return List(4) { index ->
        var l = 0.0
        var c = 0.0
        var x = 0.0
        var y = 0.0
        entries.forEach { (palette, weight) ->
            val color = palette[index]
            l += color.l * weight
            c += color.c * weight
            x += cos(color.h * PI / 180.0) * weight
            y += sin(color.h * PI / 180.0) * weight
        }
        Oklch(l / totalWeight, c / totalWeight, (atan2(y, x) * 180.0 / PI + 360.0) % 360.0)
    }
}

private fun weightedOklch(colors: List<Oklch>, weights: List<Double>): Oklch {
    val totalWeight = weights.sum().takeIf { it > 0.0 } ?: 1.0
    val chromaWeights = weights.mapIndexed { index, weight -> weight * max(colors[index].c, 0.006) }
    return Oklch(
        l = colors.mapIndexed { index, color -> color.l * weights[index] }.sum() / totalWeight,
        c = colors.mapIndexed { index, color -> color.c * weights[index] }.sum() / totalWeight,
        h = averageHue(colors, chromaWeights),
    )
}

private fun averageHue(colors: List<Oklch>, weights: List<Double>): Double {
    var x = 0.0
    var y = 0.0
    colors.forEachIndexed { index, color ->
        val weight = weights.getOrElse(index) { 0.0 }
        x += cos(color.h * PI / 180.0) * weight
        y += sin(color.h * PI / 180.0) * weight
    }
    return (atan2(y, x) * 180.0 / PI + 360.0) % 360.0
}

private fun tuneColor(color: Oklch, saturation: Double, brightness: Double, hueShift: Double): Oklch {
    return Oklch(
        l = clamp(color.l + brightness, 0.08, 0.96),
        c = clamp(color.c * saturation, 0.0, 0.24),
        h = (color.h + hueShift + 360.0) % 360.0,
    )
}

private fun oklchToColor(color: Oklch): Color {
    val hr = color.h * PI / 180.0
    val a = cos(hr) * color.c
    val b = sin(hr) * color.c

    val lPrime = color.l + 0.3963377774 * a + 0.2158037573 * b
    val mPrime = color.l - 0.1055613458 * a - 0.0638541728 * b
    val sPrime = color.l - 0.0894841775 * a - 1.2914855480 * b

    val l3 = lPrime * lPrime * lPrime
    val m3 = mPrime * mPrime * mPrime
    val s3 = sPrime * sPrime * sPrime

    return Color(
        red = encodeSrgb(+4.0767416621 * l3 - 3.3077115913 * m3 + 0.2309699292 * s3).toFloat(),
        green = encodeSrgb(-1.2684380046 * l3 + 2.6097574011 * m3 - 0.3413193965 * s3).toFloat(),
        blue = encodeSrgb(-0.0041960863 * l3 - 0.7034186147 * m3 + 1.7076147010 * s3).toFloat(),
        alpha = 1f,
    )
}

private fun encodeSrgb(value: Double): Double {
    val x = clamp(value, 0.0, 1.0)
    return if (x <= 0.0031308) 12.92 * x else 1.055 * x.pow(1.0 / 2.4) - 0.055
}

private fun clamp(value: Double, min: Double, max: Double): Double = min(max, max(min, value))

private fun smoothstep(edge0: Double, edge1: Double, x: Double): Double {
    val t = clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0)
    return t * t * (3.0 - 2.0 * t)
}

private fun gaussian(x: Double, center: Double, width: Double): Double {
    val d = (x - center) / width
    return exp(-(d * d))
}

private fun lerp(a: Double, b: Double, t: Double): Double = a + (b - a) * t

private fun lerpHue(a: Double, b: Double, t: Double): Double {
    val d = ((b - a + 540.0) % 360.0) - 180.0
    return (a + d * t + 360.0) % 360.0
}

private fun hueDistance(a: Double, b: Double): Double = abs(((b - a + 540.0) % 360.0) - 180.0)

private val solarNight = listOf(
    Oklch(0.28, 0.032, 252.0),
    Oklch(0.23, 0.034, 234.0),
    Oklch(0.19, 0.030, 214.0),
    Oklch(0.66, 0.060, 246.0),
)

private val solarSunrise = listOf(
    Oklch(0.82, 0.028, 242.0),
    Oklch(0.74, 0.036, 226.0),
    Oklch(0.61, 0.042, 310.0),
    Oklch(0.82, 0.072, 56.0),
)

private val solarMorning = listOf(
    Oklch(0.89, 0.035, 224.0),
    Oklch(0.82, 0.043, 212.0),
    Oklch(0.70, 0.044, 220.0),
    Oklch(0.85, 0.066, 82.0),
)

private val solarNoon = listOf(
    Oklch(0.88, 0.036, 226.0),
    Oklch(0.80, 0.046, 214.0),
    Oklch(0.68, 0.050, 218.0),
    Oklch(0.89, 0.048, 76.0),
)

private val solarSunset = listOf(
    Oklch(0.76, 0.030, 252.0),
    Oklch(0.68, 0.040, 300.0),
    Oklch(0.55, 0.058, 30.0),
    Oklch(0.76, 0.092, 64.0),
)

private val overcastPalette = listOf(
    Oklch(0.72, 0.007, 224.0),
    Oklch(0.64, 0.010, 214.0),
    Oklch(0.50, 0.012, 204.0),
    Oklch(0.65, 0.020, 194.0),
)

private val rainPalette = listOf(
    Oklch(0.46, 0.024, 236.0),
    Oklch(0.36, 0.028, 221.0),
    Oklch(0.27, 0.032, 206.0),
    Oklch(0.60, 0.054, 194.0),
)

private val nightRainPalette = listOf(
    Oklch(0.24, 0.034, 258.0),
    Oklch(0.20, 0.032, 238.0),
    Oklch(0.16, 0.030, 214.0),
    Oklch(0.62, 0.064, 204.0),
)

private val stormPalette = listOf(
    Oklch(0.48, 0.016, 252.0),
    Oklch(0.40, 0.018, 238.0),
    Oklch(0.29, 0.020, 224.0),
    Oklch(0.66, 0.038, 284.0),
)

private val stormDuskPalette = listOf(
    Oklch(0.54, 0.040, 60.0),
    Oklch(0.43, 0.050, 50.0),
    Oklch(0.28, 0.048, 38.0),
    Oklch(0.66, 0.090, 48.0),
)

private val mistPalette = listOf(
    Oklch(0.82, 0.004, 214.0),
    Oklch(0.75, 0.005, 206.0),
    Oklch(0.66, 0.006, 196.0),
    Oklch(0.72, 0.014, 146.0),
)

private val snowPalette = listOf(
    Oklch(0.89, 0.008, 226.0),
    Oklch(0.81, 0.012, 215.0),
    Oklch(0.71, 0.016, 204.0),
    Oklch(0.90, 0.020, 188.0),
)

private val snowNightPalette = listOf(
    Oklch(0.34, 0.020, 248.0),
    Oklch(0.28, 0.022, 232.0),
    Oklch(0.20, 0.020, 214.0),
    Oklch(0.62, 0.032, 206.0),
)

private val freezingRainPalette = listOf(
    Oklch(0.58, 0.014, 224.0),
    Oklch(0.48, 0.018, 214.0),
    Oklch(0.34, 0.020, 204.0),
    Oklch(0.72, 0.030, 190.0),
)

private val dustPalette = listOf(
    Oklch(0.74, 0.016, 84.0),
    Oklch(0.66, 0.022, 72.0),
    Oklch(0.52, 0.028, 60.0),
    Oklch(0.74, 0.038, 50.0),
)

private val sandstormPalette = listOf(
    Oklch(0.56, 0.024, 78.0),
    Oklch(0.45, 0.032, 64.0),
    Oklch(0.32, 0.036, 50.0),
    Oklch(0.60, 0.052, 42.0),
)
