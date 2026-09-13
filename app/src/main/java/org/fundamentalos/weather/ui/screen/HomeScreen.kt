package org.fundamentalos.weather.ui.screen

import org.fundamentalos.weather.ui.text.conditionText
import org.fundamentalos.weather.ui.text.windDirectionText
import org.fundamentalos.weather.ui.text.localizedClock
import org.fundamentalos.weather.ui.text.localizedNumber
import androidx.compose.ui.res.stringResource
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import org.fundamentalos.weather.ui.componets.DailyWeatherInfo
import org.fundamentalos.weather.ui.componets.GlassLoadingIndicator
import org.fundamentalos.weather.ui.componets.HourlyWeatherInfo
import org.fundamentalos.weather.weather.domain.AirQuality
import org.fundamentalos.weather.weather.domain.CurrentWeather
import org.fundamentalos.weather.weather.domain.DailyForecast
import org.fundamentalos.weather.weather.domain.MinutelyPrecipitation
import org.fundamentalos.weather.weather.domain.WeatherWarning
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import org.fundamentalos.weather.ui.componets.iosOverscroll
import org.fundamentalos.weather.ui.componets.rememberIosFlingBehavior
import org.fundamentalos.weather.ui.componets.rememberIosOverscrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import org.fundamentalos.weather.R
import org.fundamentalos.weather.settings.AppSettings
import org.fundamentalos.weather.ui.LocalHazeState
import org.fundamentalos.weather.ui.componets.AirQualityCard
import org.fundamentalos.weather.ui.componets.Banner
import org.fundamentalos.weather.ui.componets.BannerPinnedContentOffset
import org.fundamentalos.weather.ui.componets.BannerScrollReserve
import org.fundamentalos.weather.ui.componets.LocalGlassBackdrop
import org.fundamentalos.weather.ui.componets.StatusBarAppearance
import org.fundamentalos.weather.ui.componets.LocalScrollClipTop
import org.fundamentalos.weather.ui.componets.glassBackdropSource
import org.fundamentalos.weather.ui.componets.AppleWeatherCardTitleMaterial
import org.fundamentalos.weather.ui.componets.AppleWeatherCardTitleMaterialLight
import org.fundamentalos.weather.ui.componets.AppleWeatherConditionTextMaterial
import org.fundamentalos.weather.ui.componets.AppleWeatherConditionTextMaterialLight
import org.fundamentalos.weather.ui.componets.rememberGlassBackdropState
import org.fundamentalos.weather.ui.componets.LocationFooter
import org.fundamentalos.weather.ui.componets.countryDisplayName
import org.fundamentalos.weather.ui.componets.BottomBar
import org.fundamentalos.weather.ui.componets.DailyWeatherCard
import org.fundamentalos.weather.ui.componets.DetailGrid
import org.fundamentalos.weather.ui.componets.DetailInfo
import org.fundamentalos.weather.ui.sky.WeatherSkyBackground
import org.fundamentalos.weather.ui.sky.rememberSkyWallTime
import org.fundamentalos.weather.ui.componets.HourlyWeatherCard
import org.fundamentalos.weather.ui.componets.InfoCard
import org.fundamentalos.weather.ui.componets.LocalUseDarkCards
import org.fundamentalos.weather.ui.componets.LocationItem
import org.fundamentalos.weather.ui.componets.MultilayerIcon
import org.fundamentalos.weather.ui.componets.MoonCard
import org.fundamentalos.weather.ui.componets.WindCard
import org.fundamentalos.weather.ui.componets.PrecipitationCard
import org.fundamentalos.weather.ui.componets.QuickInfoCard
import org.fundamentalos.weather.ui.componets.PressureGauge
import org.fundamentalos.weather.ui.componets.SunArc
import org.fundamentalos.weather.ui.componets.parseClockMinutes
import org.fundamentalos.weather.ui.componets.WeatherIcons
import org.fundamentalos.weather.ui.componets.WeatherWarningsSection
import org.fundamentalos.weather.ui.theme.PreviewTheme
import org.fundamentalos.weather.ui.theme.PreviewThemeWithBg
import org.fundamentalos.weather.ui.theme.weatherVisualScheme
import org.fundamentalos.weather.viewmodel.MainViewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    onMapClick: () -> Unit,
    onLocationsClick: () -> Unit,
    vm: MainViewModel = koinViewModel(),
    settings: AppSettings = koinInject(),
) {
    val languageTag = androidx.compose.ui.platform.LocalConfiguration.current.locales.toLanguageTags()
    androidx.compose.runtime.LaunchedEffect(languageTag) { vm.refreshForLanguage(languageTag) }
    val locations = vm.currentLocation.value?.let { listOf(LocationItem(it.name, it.cityId)) }
        ?: listOf(LocationItem(stringResource(vm.locationLabel), ""))

    // The page shows one reading at a time and changes it as a whole: the cards fade down, the
    // reading changes underneath, and they fade back up showing the new one, while the headline
    // rolls to its new figures. The first reading arrives differently: the indicator that stood
    // in for it goes, and the headline and the cards come up, one after another.
    val incoming = homeContent(vm)
    var displayed by remember { mutableStateOf(incoming) }
    var entered by remember { mutableStateOf(false) }
    val contentAlpha = remember { Animatable(1f) }
    val enter = remember { Animatable(if (incoming == null) 0f else 1f) }
    val indicatorAlpha = remember { Animatable(if (incoming == null) 1f else 0f) }
    val latest = rememberUpdatedState(incoming)
    // Readings are taken up one at a time, each change waiting for the one before it to finish:
    // the first readings can come a second apart, as the device's own location follows the
    // network's, and a change cutting the entrance short would leave it half done.
    LaunchedEffect(Unit) {
        snapshotFlow { latest.value }.collect { next ->
            if (next == null) return@collect
            val shown = displayed
            when {
                shown == null -> {
                    indicatorAlpha.animateTo(0f, tween(IndicatorLeaveMillis, easing = FastOutLinearInEasing))
                    entered = true
                    displayed = next
                    // The frame that first composes and draws the cards is a long one; the
                    // entrance starts after it, on the same frame as the headline's roll,
                    // and gives the headline a moment's lead.
                    withFrameNanos { }
                    withFrameNanos { }
                    enter.animateTo(1f, tween(EnterMillis, delayMillis = EnterLeadMillis, easing = LinearEasing))
                }
                shown != next -> {
                    contentAlpha.animateTo(0f, tween(SwapOutMillis, easing = FastOutLinearInEasing))
                    displayed = next
                    contentAlpha.animateTo(1f, tween(SwapInMillis, easing = FastOutSlowInEasing))
                }
            }
        }
    }

    Box {
        val hazeState = remember { HazeState() }
        val scrollState = rememberScrollState()
        val overscrollState = rememberIosOverscrollState()
        // Published to the cards so their headers know where to pin.
        val scrollClipTop = remember { mutableFloatStateOf(0f) }


            val skyWallTime by rememberSkyWallTime()
            // Coloured for the reading on show, not the newest one: the colours change with
            // the cards, at the bottom of their fade, and not a moment before.
            val visualScheme = weatherVisualScheme(
                current = displayed?.weather,
                dailyForecast = displayed?.forecast ?: emptyList(),
                // Before the weather is known, the device's own place puts the sun where it is.
                latitude = (vm.currentLocation.value ?: vm.deviceLocation.value)?.latitude,
                longitude = (vm.currentLocation.value ?: vm.deviceLocation.value)?.longitude,
                now = skyWallTime,
            )
            val baseColorScheme = MaterialTheme.colorScheme
            val isLightCard = !visualScheme.useDarkCards
            val subtleContainer = visualScheme.harmonizeTarget.copy(
                alpha = if (isLightCard) 0.040f else 0.060f
            ).compositeOver(visualScheme.card)
            val strongContainer = visualScheme.harmonizeTarget.copy(
                alpha = if (isLightCard) 0.065f else 0.090f
            ).compositeOver(visualScheme.card)
            val contentColorScheme = baseColorScheme.copy(
                primary = visualScheme.harmonizeTarget,
                onPrimary = if (visualScheme.harmonizeTarget.luminance() < 0.5f) {
                    Color.White
                } else {
                    Color(0xFF111414)
                },
                surface = visualScheme.card,
                surfaceVariant = visualScheme.card,
                surfaceContainer = subtleContainer,
                surfaceContainerLow = subtleContainer,
                surfaceContainerHigh = strongContainer,
                surfaceContainerHighest = strongContainer,
                onSurface = visualScheme.onCard,
                onSurfaceVariant = visualScheme.onCardVariant,
                outlineVariant = visualScheme.onCardVariant.copy(alpha = 0.26f),
            )
            // The sky decides, not the system theme: a bright afternoon needs dark bar icons even
            // while the phone is in dark mode.
            StatusBarAppearance(lightBackground = !visualScheme.useDarkCards)

            Box(Modifier.fillMaxSize()) {
            MaterialTheme(
                colorScheme = contentColorScheme,
                typography = MaterialTheme.typography,
            ) {
                // The root Surface set LocalContentColor from the *system* theme (light text in night mode).
                // Cards pick their own light/dark look from the weather, so default text must follow onCard.
                // The runtime shaders behind the glass material need API 33; below that the
                // backdrop stays null and every glass modifier falls back to plain text.
                val glassBackdrop = if (
                    settings.glassText && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ) {
                    // A bright sky needs the text cut out of the backdrop rather than lifted
                    // off it, or every channel clamps to white.
                    rememberGlassBackdropState(
                        conditionTextMaterial = if (visualScheme.useDarkCards) {
                            AppleWeatherConditionTextMaterial
                        } else {
                            AppleWeatherConditionTextMaterialLight
                        },
                        cardTitleMaterial = if (visualScheme.useDarkCards) {
                            AppleWeatherCardTitleMaterial
                        } else {
                            AppleWeatherCardTitleMaterialLight
                        },
                    )
                } else {
                    null
                }

                CompositionLocalProvider(
                    LocalUseDarkCards provides visualScheme.useDarkCards,
                    LocalContentColor provides contentColorScheme.onSurface,
                    LocalGlassBackdrop provides glassBackdrop,
                ) {
                    Box(
                        modifier = Modifier
                            .hazeSource(hazeState)
                            .fillMaxSize()
                    ) {
                        WeatherSkyBackground(
                            sky = visualScheme.sky,
                            animated = settings.animatedBackground,
                            modifier = if (glassBackdrop != null) Modifier.glassBackdropSource(glassBackdrop) else Modifier,
                        )

                        // The atmosphere renderer owns the final sky color; no legacy tint overlay.

                        // Content slides up and is cut by this rounded window instead of running
                        // under the pinned headline, the way Apple Weather does it.
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .padding(
                                    top = BannerPinnedContentOffset,
                                    start = 16.dp,
                                    end = 16.dp,
                                )
                                .onGloballyPositioned { scrollClipTop.floatValue = it.positionInWindow().y }
                                .clipToBounds()
                        ) {
                        CompositionLocalProvider(LocalScrollClipTop provides scrollClipTop) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                // iOS-style rubber band, before verticalScroll so this sits above it
                                // in the nested scroll chain.
                                .iosOverscroll(overscrollState)
                                .verticalScroll(
                                    state = scrollState,
                                    flingBehavior = rememberIosFlingBehavior(scrollState),
                                )
                                .padding(bottom = 106.dp)
                                .navigationBarsPadding()
                                .graphicsLayer {
                                    val a = contentAlpha.value
                                    alpha = a
                                    // Sinks a little as it fades, and rises back with the new reading.
                                    translationY = (1f - a) * SwapDrop.toPx()
                                }
                        ) {
                            displayed?.let { content ->
                                val weather = content.weather
                                val today = content.today
                                var order = 0
                                // The clip window already accounts for the pinned headline.
                                Spacer(Modifier.height(BannerScrollReserve))

                                Spacer(Modifier.height(28.dp))

                                Entering(enter, order++) {
                                    QuickInfoCard(
                                        feelsLike = weather.feelsLikeCelsius.toString(),
                                        maxTemp = content.daily.firstOrNull()?.tempMax?.toString() ?: "--",
                                        minTemp = content.daily.firstOrNull()?.tempMin?.toString() ?: "--",
                                        windDirection = windDirectionText(weather.windDegree, weather.windDirection),
                                        windScale = weather.windScale,
                                    )
                                }

                                Spacer(Modifier.height(16.dp))

                                if (content.warnings.isNotEmpty()) {
                                    Entering(enter, order++) { WeatherWarningsSection(content.warnings) }
                                    Spacer(Modifier.height(16.dp))
                                }

                                content.minutely?.let {
                                    Entering(enter, order++) { PrecipitationCard(it) }
                                    Spacer(Modifier.height(16.dp))
                                }

                                content.aqi?.let {
                                    Entering(enter, order++) {
                                        AirQualityCard(it.aqi, it.level, it.effect ?: it.category.orEmpty())
                                    }
                                }

                                Spacer(Modifier.height(16.dp))

                                if (content.hourly.isNotEmpty()) {
                                    Entering(enter, order++) { HourlyWeatherCard(Modifier.fillMaxWidth(), content.hourly) }
                                }

                                Spacer(Modifier.height(16.dp))

                                if (content.daily.isNotEmpty()) {
                                    Entering(enter, order++) { DailyWeatherCard(Modifier.fillMaxWidth(), content.daily) }
                                }

                                Spacer(Modifier.height(16.dp))

                                Entering(enter, order++) {
                                    WindCard(
                                        speedKph = weather.windSpeedKph,
                                        gustKph = weather.windGustKph,
                                        direction = windDirectionText(weather.windDegree, weather.windDirection),
                                        degree = weather.windDegree,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }

                                Spacer(Modifier.height(16.dp))

                                today?.let {
                                    Entering(enter, order++) { MoonCard(it.date, Modifier.fillMaxWidth()) }

                                    Spacer(Modifier.height(16.dp))
                                }

                                Entering(enter, order++) {
                                DetailGrid(
                                    items = buildList {
                                        // Whichever comes next leads, with the other underneath and
                                        // the sun's path between them.
                                        val sunrise = parseClockMinutes(today?.sunrise)
                                        val sunset = parseClockMinutes(today?.sunset)
                                        if (sunrise != null && sunset != null && today != null) {
                                            val clock = Clock.System.now()
                                                .toLocalDateTime(TimeZone.currentSystemDefault())
                                            val nowMinutes = clock.hour * 60 + clock.minute
                                            val daytime = nowMinutes in sunrise until sunset
                                            add(
                                                DetailInfo(
                                                    label = if (daytime) stringResource(R.string.sunset) else stringResource(R.string.sunrise),
                                                    icon = R.drawable.ic_sun_horizon_24dp,
                                                    value = localizedClock(if (daytime) today.sunset!! else today.sunrise!!),
                                                    caption = if (daytime) {
                                                        stringResource(R.string.sunrise_at, localizedClock(today.sunrise!!))
                                                    } else {
                                                        stringResource(R.string.sunset_at, localizedClock(today.sunset!!))
                                                    },
                                                    graphic = {
                                                        SunArc(
                                                            sunriseMinutes = sunrise,
                                                            sunsetMinutes = sunset,
                                                            nowMinutes = nowMinutes,
                                                        )
                                                    },
                                                )
                                            )
                                        }
                                        today?.uvIndex?.let {
                                            add(
                                                DetailInfo(
                                                    stringResource(R.string.uv_index), it.toString(), uvIndexLevel(it),
                                                    icon = R.drawable.ic_uv_index_24dp,
                                                )
                                            )
                                        }
                                        add(
                                            DetailInfo(
                                                stringResource(R.string.feels_like), "${weather.feelsLikeCelsius}°",
                                                caption = feelsLikeCaption(
                                                    weather.feelsLikeCelsius, weather.tempCelsius,
                                                ),
                                                icon = R.drawable.ic_feels_like_24dp,
                                            )
                                        )
                                        today?.let {
                                            val mean = (it.tempMinCelsius + it.tempMaxCelsius) / 2
                                            add(
                                                DetailInfo(
                                                    stringResource(R.string.average_temperature), "$mean°", stringResource(R.string.average_temperature_description),
                                                    icon = R.drawable.ic_temperature_24dp,
                                                )
                                            )
                                        }
                                        weather.precipMillimeters?.let {
                                            add(
                                                DetailInfo(
                                                    stringResource(R.string.precipitation), formatMillimeters(it), stringResource(R.string.past_hour),
                                                    icon = R.drawable.ic_precip_amount_24dp,
                                                )
                                            )
                                        }
                                        weather.visibilityKm?.let {
                                            add(
                                                DetailInfo(
                                                    stringResource(R.string.visibility), stringResource(R.string.unit_km, it), visibilityCaption(it),
                                                    icon = R.drawable.ic_visibility_24dp,
                                                )
                                            )
                                        }
                                        weather.humidityPercent?.let {
                                            add(
                                                DetailInfo(
                                                    stringResource(R.string.humidity), "$it%",
                                                    caption = weather.dewPointCelsius?.let { dew ->
                                                        stringResource(R.string.dew_point, dew)
                                                    },
                                                    icon = R.drawable.ic_humidity_24dp,
                                                )
                                            )
                                        }
                                        weather.pressureHpa?.let {
                                            add(
                                                DetailInfo(
                                                    label = stringResource(R.string.pressure),
                                                    // Blank: the dial carries its own reading.
                                                    value = "",
                                                    icon = R.drawable.ic_pressure_24dp,
                                                    graphic = { PressureGauge(it) },
                                                )
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                }

                                Spacer(Modifier.height(16.dp))

                                vm.currentLocation.value?.let { location ->
                                    Entering(enter, order++) {
                                        LocationFooter(
                                            country = countryDisplayName(location.country),
                                            province = location.province,
                                            city = location.city,
                                            place = location.name,
                                            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                                        )
                                    }
                                }
                            }
                        }
                        }
                        // Stands in for the first reading, where the cards will be, and goes as
                        // they come; a failed request leaves nothing turning for nothing.
                        if (displayed == null && vm.weatherStatus.value != MainViewModel.WeatherStatus.Error) {
                            GlassLoadingIndicator(
                                Modifier
                                    .align(Alignment.Center)
                                    .graphicsLayer {
                                        val a = indicatorAlpha.value
                                        alpha = a
                                        scaleX = 0.8f + 0.2f * a
                                        scaleY = 0.8f + 0.2f * a
                                    },
                            )
                        }
                        }
                    }

                    displayed?.let { content ->
                        Banner(
                            temperature = content.weather.tempCelsius.toString(),
                            text = conditionText(content.weather.condition.iconCode, content.weather.condition.text),
                            scrollState = scrollState,
                            modifier = Modifier.align(Alignment.TopCenter).padding(start = 12.dp),
                            overscroll = overscrollState,
                            enter = entered,
                        )
                    }

                    if (vm.locationStatus.value.gpsStatus == MainViewModel.GpsStatus.PermissionDenied) {
                        // TODO: Show message to user
                    }

                    CompositionLocalProvider(LocalHazeState provides hazeState) {
                        BottomBar(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth(),
                            gpsStatus = vm.locationStatus.value.gpsStatus,
                            locations = locations,
                            selected = 0,
                            onSelectionChange = {},
                            onMapClick = onMapClick,
                            onLocationListClick = onLocationsClick,
                        )
                    }
                }
            }
            }

    }
}

/**
 * One reading of the page, taken from the view model in one go so that it changes as a whole.
 * Two readings that say the same are the same reading: a refresh that changes nothing shows
 * nothing changing. The place is not part of it — it is named below the cards from the view
 * model itself, and forgetting it, as handing the page back to the device's location does, is
 * not a new reading.
 */
private data class HomeContent(
    val weather: CurrentWeather,
    val forecast: List<DailyForecast>,
    val daily: List<DailyWeatherInfo>,
    val warnings: List<WeatherWarning>,
    val minutely: MinutelyPrecipitation?,
    val aqi: AirQuality?,
    val hourly: List<HourlyWeatherInfo>,
) {
    val today: DailyForecast? get() = forecast.firstOrNull()
}

private fun homeContent(vm: MainViewModel): HomeContent? {
    val weather = vm.weather.value ?: return null
    return HomeContent(
        weather = weather,
        forecast = vm.dailyForecast.value,
        daily = vm.dailyWeather.value,
        warnings = vm.warnings.value,
        minutely = vm.minutelyPrecipitation.value,
        aqi = vm.aqi.value,
        hourly = vm.hourlyWeather.value,
    )
}

/**
 * Brings a card up in its turn as the first reading arrives: it fades in and rises a little, a
 * beat after the card above it. Once [enter] has run its course this is a plain box.
 */
@Composable
private fun Entering(enter: Animatable<Float, *>, index: Int, content: @Composable () -> Unit) {
    Box(
        Modifier.graphicsLayer {
            val t = ((enter.value * EnterMillis - index * EnterStaggerMillis) / EnterCardMillis).coerceIn(0f, 1f)
            val e = FastOutSlowInEasing.transform(t)
            alpha = e
            translationY = (1f - e) * EnterRise.toPx()
        },
    ) { content() }
}

/** The whole entrance, long enough for a dozen cards to have come up. */
private const val EnterMillis = 1500
/** How long the headline has the page to itself before the first card starts up. */
private const val EnterLeadMillis = 150
/** How long after the card above it a card starts up. */
private const val EnterStaggerMillis = 70
/** How long one card takes to come up. */
private const val EnterCardMillis = 560
private val EnterRise = 28.dp
private const val IndicatorLeaveMillis = 220
private const val SwapOutMillis = 180
private const val SwapInMillis = 380
private val SwapDrop = 8.dp

/** UV index bands as published by the WMO. */
@Composable
private fun uvIndexLevel(index: Int): String = when {
    index <= 2 -> stringResource(R.string.low)
    index <= 5 -> stringResource(R.string.moderate)
    index <= 7 -> stringResource(R.string.high)
    index <= 10 -> stringResource(R.string.very_high)
    else -> stringResource(R.string.extreme)
}

@Composable
private fun feelsLikeCaption(feelsLike: Int, actual: Int): String = when {
    feelsLike - actual >= 2 -> stringResource(R.string.feels_warmer)
    actual - feelsLike >= 2 -> stringResource(R.string.feels_colder)
    else -> stringResource(R.string.feels_similar)
}

@Composable
private fun visibilityCaption(km: Int): String = when {
    km < 1 -> stringResource(R.string.visibility_very_poor)
    km < 5 -> stringResource(R.string.visibility_poor)
    km < 10 -> stringResource(R.string.visibility_fair)
    km < 20 -> stringResource(R.string.visibility_good)
    else -> stringResource(R.string.visibility_clear)
}

@Composable
private fun formatMillimeters(value: Double): String =
    stringResource(R.string.unit_mm, localizedNumber(if (value < 0.05) 0.0 else value, if (value < 0.05) 0 else 1))

@Preview
@Composable
private fun Preview() {
    PreviewThemeWithBg {
        HomeScreen(onMapClick = {}, onLocationsClick = {})
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Preview
@Composable
private fun IconsPreview() {
    PreviewTheme {
        Column {
            InfoCard {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    MultilayerIcon(WeatherIcons.Clear, "")
                    MultilayerIcon(WeatherIcons.PartlyCloudy, "")
                    MultilayerIcon(WeatherIcons.MostlyClearWithIntermittentClouds, "")
                    MultilayerIcon(WeatherIcons.MostlyCloudy, "")
                    MultilayerIcon(WeatherIcons.Overcast, "")
                    MultilayerIcon(WeatherIcons.ClearNight, "")
                    MultilayerIcon(WeatherIcons.PartlyCloudyNight, "")
                    MultilayerIcon(WeatherIcons.MostlyClearWithIntermittentCloudsNight, "")
                    MultilayerIcon(WeatherIcons.MostlyCloudyNight, "")
                    MultilayerIcon(WeatherIcons.Shower, "")
                    MultilayerIcon(WeatherIcons.HeavyShower, "")
                    MultilayerIcon(WeatherIcons.ShowerNight, "")
                    MultilayerIcon(WeatherIcons.HeavyShowerNight, "")
                    MultilayerIcon(WeatherIcons.Thunderstorm, "")
                    MultilayerIcon(WeatherIcons.SevereThunderstorm, "")
                    MultilayerIcon(WeatherIcons.ThunderstormWithHail, "")
                    MultilayerIcon(WeatherIcons.LightRain, "")
                    MultilayerIcon(WeatherIcons.LightToModerateRain, "")
                    MultilayerIcon(WeatherIcons.ModerateRain, "")
                    MultilayerIcon(WeatherIcons.ModerateToHeavyRain, "")
                    MultilayerIcon(WeatherIcons.HeavyRain, "")
                    MultilayerIcon(WeatherIcons.HeavyToTorrentialRain, "")
                    MultilayerIcon(WeatherIcons.TorrentialRain, "")
                    MultilayerIcon(WeatherIcons.TorrentialToSevereTorrentialRain, "")
                    MultilayerIcon(WeatherIcons.SevereTorrentialRain, "")
                    MultilayerIcon(
                        WeatherIcons.SevereTorrentialToExtremelySevereTorrentialRain,
                        ""
                    )
                    MultilayerIcon(WeatherIcons.ExtremelySevereTorrentialRain, "")
                    MultilayerIcon(WeatherIcons.ExtremeRain, "")
                    MultilayerIcon(WeatherIcons.Drizzle, "")
                    MultilayerIcon(WeatherIcons.FreezingRain, "")
                    MultilayerIcon(WeatherIcons.Rain, "")
                    MultilayerIcon(WeatherIcons.LightSnow, "")
                    MultilayerIcon(WeatherIcons.LightToModerateSnow, "")
                    MultilayerIcon(WeatherIcons.ModerateSnow, "")
                    MultilayerIcon(WeatherIcons.ModerateToHeavySnow, "")
                    MultilayerIcon(WeatherIcons.HeavySnow, "")
                    MultilayerIcon(WeatherIcons.HeavyToBlizzardSnow, "")
                    MultilayerIcon(WeatherIcons.Blizzard, "")
                    MultilayerIcon(WeatherIcons.RainAndSnowMix, "")
                    MultilayerIcon(WeatherIcons.RainAndSnow, "")
                    MultilayerIcon(WeatherIcons.ShowerWithSnow, "")
                    MultilayerIcon(WeatherIcons.SnowShower, "")
                    MultilayerIcon(WeatherIcons.ShowerWithSnowNight, "")
                    MultilayerIcon(WeatherIcons.SnowShowerNight, "")
                    MultilayerIcon(WeatherIcons.Snow, "")
                    MultilayerIcon(WeatherIcons.LightFog, "")
                    MultilayerIcon(WeatherIcons.Fog, "")
                    MultilayerIcon(WeatherIcons.HeavyFog, "")
                    MultilayerIcon(WeatherIcons.DenseFog, "")
                    MultilayerIcon(WeatherIcons.SevereDenseFog, "")
                    MultilayerIcon(WeatherIcons.ExtremelyDenseFog, "")
                    MultilayerIcon(WeatherIcons.Haze, "")
                    MultilayerIcon(WeatherIcons.ModerateHaze, "")
                    MultilayerIcon(WeatherIcons.HeavyHaze, "")
                    MultilayerIcon(WeatherIcons.SevereHaze, "")
                    MultilayerIcon(WeatherIcons.FloatingDust, "")
                    MultilayerIcon(WeatherIcons.DustStorm, "")
                    MultilayerIcon(WeatherIcons.Sandstorm, "")
                    MultilayerIcon(WeatherIcons.SevereSandstorm, "")
                    MultilayerIcon(WeatherIcons.Hot, "")
                    MultilayerIcon(WeatherIcons.Cold, "")
                    MultilayerIcon(WeatherIcons.Unknown, "")
                }
            }

            Spacer(Modifier.height(12.dp))

            InfoCard {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        painterResource(R.drawable.warning_typhoon_24dp),
                        "",
                        Modifier,
                        MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        painterResource(R.drawable.warning_tornado_24dp),
                        "",
                        Modifier,
                        MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        painterResource(R.drawable.warning_rainstorm_24dp),
                        "",
                        Modifier,
                        MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        painterResource(R.drawable.warning_snow_storm_24dp),
                        "",
                        Modifier,
                        MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        painterResource(R.drawable.warning_cold_wave_24dp),
                        "",
                        Modifier,
                        MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        painterResource(R.drawable.warning_gale_24dp),
                        "",
                        Modifier,
                        MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        painterResource(R.drawable.warning_heat_wave_24dp),
                        "",
                        Modifier,
                        MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        painterResource(R.drawable.warning_downburst_24dp),
                        "",
                        Modifier,
                        MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        painterResource(R.drawable.warning_avalanche_24dp),
                        "",
                        Modifier,
                        MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        painterResource(R.drawable.warning_lightning_24dp),
                        "",
                        Modifier,
                        MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        painterResource(R.drawable.warning_hail_24dp),
                        "",
                        Modifier,
                        MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        painterResource(R.drawable.warning_frost_24dp),
                        "",
                        Modifier,
                        MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        painterResource(R.drawable.warning_heavy_fog_24dp),
                        "",
                        Modifier,
                        MaterialTheme.colorScheme.onSurface
                    )

                }
            }
        }
    }
}
