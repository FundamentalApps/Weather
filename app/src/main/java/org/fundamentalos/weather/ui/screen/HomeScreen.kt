package org.fundamentalos.weather.ui.screen

import android.content.res.Configuration
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import org.fundamentalos.weather.settings.AppSettings
import org.fundamentalos.weather.ui.LocalHazeState
import org.fundamentalos.weather.ui.components.Banner
import org.fundamentalos.weather.ui.components.BannerPinnedContentOffset
import org.fundamentalos.weather.ui.components.BottomBar
import org.fundamentalos.weather.ui.components.GlassLoadingIndicator
import org.fundamentalos.weather.ui.components.IosOverscrollState
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

/** How much of the width the sideways headline and chips take; the cards have the rest. */
private const val LandscapeHeadlineShare = 0.42f

/** Room the sideways cards keep above their first card, and below their last for the buttons. */
private val LandscapeCardsHeadroom = 16.dp
private val LandscapeCardsFootroom = 96.dp

/**
 * The home page: the sky, the headline over it, and the cards scrolling under a clipped window,
 * coloured for the reading on show. Upright, the headline is pinned over the cards and shrinks
 * as they scroll; sideways, the way Apple Weather lays it out, the headline and the quick-info
 * chips stand still at the left and the other cards scroll at the right. The reading and how it
 * changes are in [HomeReading.kt], the cards in [HomeCards.kt], the colours in [HomeTheme.kt].
 */
@Composable
fun HomeScreen(
    onMapClick: () -> Unit,
    onLocationsClick: () -> Unit,
    vm: MainViewModel = koinViewModel(),
    settings: AppSettings = koinInject(),
) {
    val configuration = LocalConfiguration.current
    val languageTag = configuration.locales.toLanguageTags()
    LaunchedEffect(languageTag) { vm.refreshForLanguage(languageTag) }
    val landscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
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
        val darkTheme = isSystemInDarkTheme()
        fun schemeOf(reading: HomeContent?) = weatherVisualScheme(
            current = reading?.weather,
            dailyForecast = reading?.forecast ?: emptyList(),
            latitude = reading?.latitude ?: fallbackPlace?.latitude,
            longitude = reading?.longitude ?: fallbackPlace?.longitude,
            now = skyWallTime,
            darkTheme = darkTheme,
        )
        // Remembered by their inputs: a recomposition that changes none of them must hand the
        // theme the same colour scheme, or every card under it recomposes for nothing.
        val visualScheme = remember(displayed, skyWallTime, darkTheme, fallbackPlace) { schemeOf(displayed) }
        val skyShown = transition.skyShown
        val sky = remember(skyShown, visualScheme, fallbackPlace) {
            if (skyShown === displayed) visualScheme.sky else schemeOf(skyShown).sky
        }
        val baseColorScheme = MaterialTheme.colorScheme
        val contentColorScheme = remember(visualScheme, baseColorScheme) { visualScheme.contentColorScheme(baseColorScheme) }
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

                    // The cards fade down and back up around a change of reading, and sink a
                    // little as they go; the chips pinned sideways go with them.
                    val fading = Modifier.graphicsLayer {
                        val a = transition.contentAlpha.value
                        alpha = a
                        translationY = (1f - a) * SwapDrop.toPx()
                    }
                    val cardsColumn: @Composable (headroom: Dp, footroom: Dp, quickInfo: Boolean) -> Unit =
                        { headroom, footroom, quickInfo ->
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
                                        .padding(bottom = footroom)
                                        .navigationBarsPadding()
                                        .then(fading)
                                ) {
                                    displayed?.let { HomeCards(it, transition.enter, vm.currentLocation.value, headroom, quickInfo) }
                                }
                            }
                        }

                    if (landscape) {
                        Row(
                            Modifier
                                .fillMaxSize()
                                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top))
                        ) {
                            // The headline and the chips, pinned: nothing of them scrolls. Centred
                            // in the room above the buttons, not the whole height, or they sit on
                            // them.
                            Column(
                                Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(LandscapeHeadlineShare)
                                    .padding(start = 16.dp, end = 12.dp, bottom = LandscapeCardsFootroom),
                                verticalArrangement = Arrangement.Center,
                            ) {
                                displayed?.let { content ->
                                    Headline(content, rememberScrollState(), overscroll = null, enter = transition.entered, pinned = true)
                                    Spacer(Modifier.height(24.dp))
                                    Box(fading) { Entering(transition.enter, 0) { QuickInfoChips(content) } }
                                }
                            }
                            // The cards, cut at the pane's top edge the way the upright window cuts
                            // them. The edge sits exactly on the first card's top: with no room
                            // above it to travel, the first scroll pins its title at once, which is
                            // the state the upright page reaches when its headline has collapsed.
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .padding(top = LandscapeCardsHeadroom, end = 16.dp)
                                    .onGloballyPositioned { scrollClipTop.floatValue = it.positionInWindow().y }
                                    .clipToBounds()
                            ) {
                                cardsColumn(0.dp, LandscapeCardsFootroom, false)
                            }
                        }
                    } else {
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
                            cardsColumn(PortraitCardsHeadroom, 106.dp, true)
                        }
                    }
                    // Stands in for the first reading, in the middle of the screen itself rather
                    // than of the cards' window, and goes as they come; a failed request leaves
                    // nothing turning for nothing.
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

                if (!landscape) displayed?.let { content ->
                    Headline(
                        content, scrollState, overscrollState, enter = transition.entered, pinned = false,
                        modifier = Modifier.align(Alignment.TopCenter).padding(start = 12.dp),
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
                        // Sideways the middle of the bar is under the cards; the place keeps to the map button.
                        placeBesideMap = landscape,
                    )
                }
            }
        }
    }
}

/** The reading's headline: the temperature and the condition, as [Banner] sets them. */
@Composable
private fun Headline(
    content: HomeContent,
    scrollState: ScrollState,
    overscroll: IosOverscrollState?,
    enter: Boolean,
    pinned: Boolean,
    modifier: Modifier = Modifier,
) {
    Banner(
        temperature = content.weather.tempCelsius.toString(),
        text = conditionText(content.weather.condition.iconCode, content.weather.condition.text),
        scrollState = scrollState,
        modifier = modifier,
        overscroll = overscroll,
        enter = enter,
        pinned = pinned,
    )
}
