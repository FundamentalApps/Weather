package org.fundamentalos.weather.ui.components

import org.fundamentalos.weather.weather.domain.DailyForecast
import org.fundamentalos.weather.weather.domain.HourlyForecast

/**
 * The forecast's shared numeric weather codes, drawn: each maps to a layered icon, by day or by
 * night. This is the UI's reading of the codes; the view model publishes the forecast itself.
 */
fun weatherIconFor(code: String, isDay: Boolean = true): MultilayerIcon =
    (if (isDay) DayIcons else NightIcons)[code]?.invoke()
        ?: DayIcons[code]?.invoke()
        ?: NightIcons[code]?.invoke()
        ?: WeatherIcons.Unknown

/** A day of the forecast as the ten-day card shows it. */
fun DailyForecast.toDailyWeatherInfo(): DailyWeatherInfo = DailyWeatherInfo(
    date = date.toString(),
    icon = weatherIconFor(dayCondition.iconCode, dayCondition.isDay ?: true),
    description = dayCondition.text,
    conditionCode = dayCondition.iconCode,
    probability = precipitationProbabilityPercent?.let { "$it%" },
    tempMin = tempMinCelsius,
    tempMax = tempMaxCelsius,
)

/** An hour of the forecast as the hourly strip shows it. */
fun HourlyForecast.toHourlyWeatherInfo(): HourlyWeatherInfo = HourlyWeatherInfo(
    time = time.toString(),
    icon = weatherIconFor(condition.iconCode, condition.isDay ?: true),
    description = condition.text,
    conditionCode = condition.iconCode,
    temp = tempCelsius,
)

/** Icon-code map: the shared numeric weather codes each map to a layered icon. */
private val DayIcons = mapOf<String, () -> MultilayerIcon>(
    "100" to { WeatherIcons.Clear },
    "101" to { WeatherIcons.MostlyCloudy },
    "102" to { WeatherIcons.MostlyClearWithIntermittentClouds },
    "103" to { WeatherIcons.PartlyCloudy },
    "104" to { WeatherIcons.Overcast },
    "300" to { WeatherIcons.Shower },
    "301" to { WeatherIcons.HeavyShower },
    "302" to { WeatherIcons.Thunderstorm },
    "303" to { WeatherIcons.SevereThunderstorm },
    "304" to { WeatherIcons.ThunderstormWithHail },
    "305" to { WeatherIcons.LightRain },
    "306" to { WeatherIcons.ModerateRain },
    "307" to { WeatherIcons.HeavyRain },
    "308" to { WeatherIcons.ExtremeRain },
    "309" to { WeatherIcons.Drizzle },
    "310" to { WeatherIcons.TorrentialRain },
    "311" to { WeatherIcons.SevereTorrentialRain },
    "312" to { WeatherIcons.ExtremelySevereTorrentialRain },
    "313" to { WeatherIcons.FreezingRain },
    "314" to { WeatherIcons.LightToModerateRain },
    "315" to { WeatherIcons.ModerateToHeavyRain },
    "316" to { WeatherIcons.HeavyToTorrentialRain },
    "317" to { WeatherIcons.TorrentialToSevereTorrentialRain },
    "318" to { WeatherIcons.SevereTorrentialToExtremelySevereTorrentialRain },
    "399" to { WeatherIcons.Rain },
    "400" to { WeatherIcons.LightSnow },
    "401" to { WeatherIcons.ModerateSnow },
    "402" to { WeatherIcons.HeavySnow },
    "403" to { WeatherIcons.Blizzard },
    "404" to { WeatherIcons.RainAndSnowMix },
    "405" to { WeatherIcons.RainAndSnow },
    "406" to { WeatherIcons.ShowerWithSnow },
    "407" to { WeatherIcons.SnowShower },
    "408" to { WeatherIcons.LightToModerateSnow },
    "409" to { WeatherIcons.ModerateToHeavySnow },
    "410" to { WeatherIcons.HeavyToBlizzardSnow },
    "499" to { WeatherIcons.Snow },
    "500" to { WeatherIcons.LightFog },
    "501" to { WeatherIcons.Fog },
    "502" to { WeatherIcons.Haze },
    "503" to { WeatherIcons.DustStorm },
    "504" to { WeatherIcons.FloatingDust },
    "507" to { WeatherIcons.Sandstorm },
    "508" to { WeatherIcons.SevereSandstorm },
    "509" to { WeatherIcons.DenseFog },
    "510" to { WeatherIcons.SevereDenseFog },
    "511" to { WeatherIcons.ModerateHaze },
    "512" to { WeatherIcons.HeavyHaze },
    "513" to { WeatherIcons.SevereHaze },
    "514" to { WeatherIcons.HeavyFog },
    "515" to { WeatherIcons.ExtremelyDenseFog },
    "900" to { WeatherIcons.Hot },
    "901" to { WeatherIcons.Cold },
    "999" to { WeatherIcons.Unknown },
)

private val NightIcons = mapOf<String, () -> MultilayerIcon>(
    "150" to { WeatherIcons.ClearNight },
    "151" to { WeatherIcons.MostlyCloudyNight },
    "152" to { WeatherIcons.MostlyClearWithIntermittentCloudsNight },
    "153" to { WeatherIcons.PartlyCloudyNight },
    "104" to { WeatherIcons.Overcast },
    "350" to { WeatherIcons.ShowerNight },
    "351" to { WeatherIcons.HeavyShowerNight },
    "302" to { WeatherIcons.Thunderstorm },
    "303" to { WeatherIcons.SevereThunderstorm },
    "304" to { WeatherIcons.ThunderstormWithHail },
    "305" to { WeatherIcons.LightRain },
    "306" to { WeatherIcons.ModerateRain },
    "307" to { WeatherIcons.HeavyRain },
    "308" to { WeatherIcons.ExtremeRain },
    "309" to { WeatherIcons.Drizzle },
    "310" to { WeatherIcons.TorrentialRain },
    "311" to { WeatherIcons.SevereTorrentialRain },
    "312" to { WeatherIcons.ExtremelySevereTorrentialRain },
    "313" to { WeatherIcons.FreezingRain },
    "314" to { WeatherIcons.LightToModerateRain },
    "315" to { WeatherIcons.ModerateToHeavyRain },
    "316" to { WeatherIcons.HeavyToTorrentialRain },
    "317" to { WeatherIcons.TorrentialToSevereTorrentialRain },
    "318" to { WeatherIcons.SevereTorrentialToExtremelySevereTorrentialRain },
    "400" to { WeatherIcons.LightSnow },
    "401" to { WeatherIcons.ModerateSnow },
    "402" to { WeatherIcons.HeavySnow },
    "403" to { WeatherIcons.Blizzard },
    "404" to { WeatherIcons.RainAndSnowMix },
    "405" to { WeatherIcons.RainAndSnow },
    "456" to { WeatherIcons.ShowerWithSnowNight },
    "457" to { WeatherIcons.SnowShowerNight },
    "408" to { WeatherIcons.LightToModerateSnow },
    "409" to { WeatherIcons.ModerateToHeavySnow },
    "410" to { WeatherIcons.HeavyToBlizzardSnow },
    "499" to { WeatherIcons.Snow },
    "500" to { WeatherIcons.LightFog },
    "501" to { WeatherIcons.Fog },
    "502" to { WeatherIcons.Haze },
    "503" to { WeatherIcons.DustStorm },
    "504" to { WeatherIcons.FloatingDust },
    "507" to { WeatherIcons.Sandstorm },
    "508" to { WeatherIcons.SevereSandstorm },
    "509" to { WeatherIcons.DenseFog },
    "510" to { WeatherIcons.SevereDenseFog },
    "511" to { WeatherIcons.ModerateHaze },
    "512" to { WeatherIcons.HeavyHaze },
    "513" to { WeatherIcons.SevereHaze },
    "514" to { WeatherIcons.HeavyFog },
    "515" to { WeatherIcons.ExtremelyDenseFog },
    "900" to { WeatherIcons.Hot },
    "901" to { WeatherIcons.Cold },
    "999" to { WeatherIcons.Unknown }
)
