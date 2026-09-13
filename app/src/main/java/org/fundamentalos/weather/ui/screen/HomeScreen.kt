package org.fundamentalos.weather.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import org.fundamentalos.weather.settings.AppSettings
import org.fundamentalos.weather.ui.LocalHazeState
import org.fundamentalos.weather.ui.components.Banner
import org.fundamentalos.weather.ui.components.BannerPinnedContentOffset
import org.fundamentalos.weather.ui.components.BottomBar
import org.fundamentalos.weather.ui.components.GlassLoadingIndicator
import org.fundamentalos.weather.ui.components.LocalGlassBackdrop
import org.fundamentalos.weather.ui.components.LocalScrollClipTop
import org.fundamentalos.weather.ui.components.LocalUseDarkCards
import org.fundamentalos.weather.ui.components.LocationItem
import org.fundamentalos.weather.ui.components.StatusBarAppearance
import org.fundamentalos.weather.ui.components.glassBackdropSource
import org.fundamentalos.weather.ui.components.iosOverscroll
import org.fundamentalos.weather.ui.components.rememberIosFlingBehavior
import org.fundamentalos.weather.ui.components.rememberIosOverscrollState
import org.fundamentalos.weather.ui.sky.WeatherSkyBackground
import org.fundamentalos.weather.ui.sky.rememberSkyWallTime
import org.fundamentalos.weather.ui.text.conditionText
import org.fundamentalos.weather.ui.theme.weatherVisualScheme
import org.fundamentalos.weather.viewmodel.MainViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * The home page: the sky, the headline pinned over it, and the cards scrolling under a clipped
 * window, coloured for the reading on show. The reading and how it changes are in
 * [HomeReading.kt], the cards in [HomeCards.kt], the colours in [HomeTheme.kt].
 */
@Composable
fun HomeScreen(
    onMapClick: () -> Unit,
    onLocationsClick: () -> Unit,
    vm: MainViewModel = koinViewModel(),
    settings: AppSettings = koinInject(),
) {
    val languageTag = LocalConfiguration.current.locales.toLanguageTags()
    LaunchedEffect(languageTag) { vm.refreshForLanguage(languageTag) }
    val locations = vm.currentLocation.value?.let { listOf(LocationItem(it.name, it.cityId)) }
        ?: listOf(LocationItem(stringResource(vm.locationLabel), ""))

    // The last place the view model named, kept for the reading: see homeContent.
    val place = remember { doubleArrayOf(Double.NaN, Double.NaN) }
    vm.currentLocation.value?.let { place[0] = it.latitude; place[1] = it.longitude }
    val transition = rememberHomeTransition(homeContent(vm, place))
    val displayed = transition.displayed

    Box {
        val hazeState = remember { HazeState() }
        val scrollState = rememberScrollState()
        val overscrollState = rememberIosOverscrollState()
        // Published to the cards so their headers know where to pin.
        val scrollClipTop = remember { mutableFloatStateOf(0f) }

        // Coloured for the reading on show, not the newest one, sun and all: the colours change
        // with the cards, at the bottom of their fade, and not a moment before. Before the
        // weather is known, the device's own place puts the sun where it is.
        val skyWallTime by rememberSkyWallTime()
        val fallbackPlace = vm.currentLocation.value ?: vm.deviceLocation.value
        fun schemeOf(reading: HomeContent?) = weatherVisualScheme(
            current = reading?.weather,
            dailyForecast = reading?.forecast ?: emptyList(),
            latitude = reading?.latitude ?: fallbackPlace?.latitude,
            longitude = reading?.longitude ?: fallbackPlace?.longitude,
            now = skyWallTime,
        )
        val visualScheme = schemeOf(displayed)
        val skyShown = transition.skyShown
        val sky = if (skyShown === displayed) visualScheme.sky else schemeOf(skyShown).sky
        val contentColorScheme = visualScheme.contentColorScheme(MaterialTheme.colorScheme)
        // The sky decides, not the system theme: a bright afternoon needs dark bar icons even
        // while the phone is in dark mode.
        StatusBarAppearance(lightBackground = !visualScheme.useDarkCards)

        MaterialTheme(colorScheme = contentColorScheme, typography = MaterialTheme.typography) {
            val glassBackdrop = rememberHomeGlassBackdrop(settings.glassText, visualScheme.useDarkCards)
            // The root Surface set LocalContentColor from the *system* theme (light text in night
            // mode). Cards pick their own light/dark look from the weather, so default text must
            // follow onCard.
            CompositionLocalProvider(
                LocalUseDarkCards provides visualScheme.useDarkCards,
                LocalContentColor provides contentColorScheme.onSurface,
                LocalGlassBackdrop provides glassBackdrop,
            ) {
                Box(Modifier.hazeSource(hazeState).fillMaxSize()) {
                    WeatherSkyBackground(
                        sky = sky,
                        animated = settings.animatedBackground,
                        modifier = if (glassBackdrop != null) Modifier.glassBackdropSource(glassBackdrop) else Modifier,
                    )

                    // Content slides up and is cut by this window instead of running under the
                    // pinned headline, the way Apple Weather does it.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(top = BannerPinnedContentOffset, start = 16.dp, end = 16.dp)
                            .onGloballyPositioned { scrollClipTop.floatValue = it.positionInWindow().y }
                            .clipToBounds()
                    ) {
                        CompositionLocalProvider(LocalScrollClipTop provides scrollClipTop) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    // iOS-style rubber band, before verticalScroll so this sits
                                    // above it in the nested scroll chain.
                                    .iosOverscroll(overscrollState)
                                    .verticalScroll(
                                        state = scrollState,
                                        flingBehavior = rememberIosFlingBehavior(scrollState),
                                    )
                                    .padding(bottom = 106.dp)
                                    .navigationBarsPadding()
                                    .graphicsLayer {
                                        val a = transition.contentAlpha.value
                                        alpha = a
                                        // Sinks a little as it fades, and rises back with the new reading.
                                        translationY = (1f - a) * SwapDrop.toPx()
                                    }
                            ) {
                                displayed?.let { HomeCards(it, transition.enter, vm.currentLocation.value) }
                            }
                        }
                        // Stands in for the first reading, where the cards will be, and goes as
                        // they come; a failed request leaves nothing turning for nothing.
                        if (displayed == null && vm.weatherStatus.value != MainViewModel.WeatherStatus.Error) {
                            GlassLoadingIndicator(
                                Modifier
                                    .align(Alignment.Center)
                                    .graphicsLayer {
                                        val a = transition.indicatorAlpha.value
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
                        enter = transition.entered,
                    )
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
