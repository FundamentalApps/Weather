package org.fundamentalos.weather.ui.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import org.fundamentalos.weather.ui.components.DailyWeatherInfo
import org.fundamentalos.weather.ui.components.HourlyWeatherInfo
import org.fundamentalos.weather.ui.components.toDailyWeatherInfo
import org.fundamentalos.weather.ui.components.toHourlyWeatherInfo
import org.fundamentalos.weather.viewmodel.MainViewModel
import org.fundamentalos.weather.weather.domain.AirQuality
import org.fundamentalos.weather.weather.domain.CurrentWeather
import org.fundamentalos.weather.weather.domain.DailyForecast
import org.fundamentalos.weather.weather.domain.MinutelyPrecipitation
import org.fundamentalos.weather.weather.domain.WeatherWarning

/**
 * One reading of the page, taken from the view model in one go so that it changes as a whole.
 * Two readings that say the same are the same reading: a refresh that changes nothing shows
 * nothing changing. The place is not part of it — it is named below the cards from the view
 * model itself, and forgetting it, as handing the page back to the device's location does, is
 * not a new reading.
 */
internal data class HomeContent(
    val weather: CurrentWeather,
    val forecast: List<DailyForecast>,
    val daily: List<DailyWeatherInfo>,
    val warnings: List<WeatherWarning>,
    val minutely: MinutelyPrecipitation?,
    val aqi: AirQuality?,
    val hourly: List<HourlyWeatherInfo>,
    /** Where the reading is from, which puts the sun in its sky. */
    val latitude: Double?,
    val longitude: Double?,
) {
    val today: DailyForecast? get() = forecast.firstOrNull()
}

/** The hourly card is a one-day strip; the source sends two days of hours. */
private const val HourlyHours = 24

/**
 * The reading the view model holds now. [place] is the last place it named, kept by the caller:
 * handing the page back to the device's location forgets the place before the new reading
 * comes, and the reading on show keeps its own sun until then.
 */
@Composable
internal fun homeContent(vm: MainViewModel, place: DoubleArray): HomeContent? {
    val weather = vm.weather.value ?: return null
    val forecast = vm.dailyForecast.value
    val hours = vm.hourlyForecast.value
    // The cards' own shapes of the forecast, made once per forecast rather than per frame.
    val daily = remember(forecast) { forecast.map { it.toDailyWeatherInfo() } }
    val hourly = remember(hours) { hours.take(HourlyHours).map { it.toHourlyWeatherInfo() } }
    return HomeContent(
        weather = weather,
        latitude = place[0].takeIf { !it.isNaN() },
        longitude = place[1].takeIf { !it.isNaN() },
        forecast = forecast,
        daily = daily,
        warnings = vm.warnings.value,
        minutely = vm.minutelyPrecipitation.value,
        aqi = vm.aqi.value,
        hourly = hourly,
    )
}

/**
 * The reading on show, and how it changes. The page shows one reading at a time and changes it
 * as a whole: the cards fade down, the reading changes underneath, and they fade back up
 * showing the new one, while the headline rolls to its new figures. The first reading arrives
 * differently: the indicator that stood in for it goes, and the headline and the cards come
 * up, one after another.
 */
internal class HomeTransition(incoming: HomeContent?) {
    /** The reading the cards and the headline show. */
    var displayed by mutableStateOf(incoming)
        private set

    /**
     * The reading the sky shows. It is given the new one as the cards come back up with it,
     * after the long frame that composes them, so its cross-fade sets off with them and is
     * not frozen by it.
     */
    var skyShown by mutableStateOf(incoming)
        private set

    /** Whether the first reading arrived while the page was open, and so rolled in. */
    var entered by mutableStateOf(false)
        private set

    /** The cards' opacity: down and back up around a change of reading. */
    val contentAlpha = Animatable(1f)

    /** How far the entrance has run; each card takes its turn from it. */
    val enter = Animatable(if (incoming == null) 0f else 1f)

    /** The loading indicator's opacity, gone once the first reading is in. */
    val indicatorAlpha = Animatable(if (incoming == null) 1f else 0f)

    /** Takes up [next], animating the change; suspends until it is done. */
    suspend fun take(next: HomeContent) {
        val shown = displayed
        when {
            shown == null -> {
                indicatorAlpha.animateTo(0f, tween(IndicatorLeaveMillis, easing = FastOutLinearInEasing))
                entered = true
                displayed = next
                // The frame that first composes and draws the cards is a long one; the
                // entrance starts after it, on the same frame as the headline's roll and
                // the sky's change, and gives the headline a moment's lead.
                withFrameNanos { }
                withFrameNanos { }
                skyShown = next
                enter.animateTo(1f, tween(EnterMillis, delayMillis = EnterLeadMillis, easing = LinearEasing))
            }
            shown != next -> {
                contentAlpha.animateTo(0f, tween(SwapOutMillis, easing = FastOutLinearInEasing))
                displayed = next
                // The frame that composes the new reading's cards is a long one; the fade
                // back up and the sky's change start after it, whole, rather than losing
                // their first moments to it.
                withFrameNanos { }
                withFrameNanos { }
                skyShown = next
                contentAlpha.animateTo(1f, tween(SwapInMillis, easing = FastOutSlowInEasing))
            }
        }
    }
}

/**
 * The page's transition, fed every reading as it comes. Readings are taken up one at a time,
 * each change waiting for the one before it to finish: the first readings can come a second
 * apart, as the device's own location follows the network's, and a change cutting the entrance
 * short would leave it half done.
 */
@Composable
internal fun rememberHomeTransition(incoming: HomeContent?): HomeTransition {
    val transition = remember { HomeTransition(incoming) }
    val latest = rememberUpdatedState(incoming)
    LaunchedEffect(Unit) {
        snapshotFlow { latest.value }.collect { next -> if (next != null) transition.take(next) }
    }
    return transition
}

/**
 * Brings a card up in its turn as the first reading arrives: it fades in and rises a little, a
 * beat after the card above it. Once [enter] has run its course this is a plain box.
 */
@Composable
internal fun Entering(enter: Animatable<Float, AnimationVector1D>, index: Int, content: @Composable () -> Unit) {
    Box(
        Modifier.graphicsLayer {
            val t = ((enter.value * EnterMillis - index * EnterStaggerMillis) / EnterCardMillis).coerceIn(0f, 1f)
            val e = FastOutSlowInEasing.transform(t)
            alpha = e
            translationY = (1f - e) * EnterRise.toPx()
        },
    ) { content() }
}

/** How far the cards sink as they fade, and rise back with the new reading. */
internal val SwapDrop = 8.dp

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
