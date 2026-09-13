package org.fundamentalos.weather.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import dev.chrisbanes.haze.HazeState
import org.fundamentalos.weather.ui.components.LocationProvider
import org.fundamentalos.weather.ui.screen.AboutScreen
import org.fundamentalos.weather.ui.screen.DataSourcesScreen
import org.fundamentalos.weather.ui.screen.HomeScreen
import org.fundamentalos.weather.ui.screen.LocationsScreen
import org.fundamentalos.weather.ui.screen.MapScreen
import org.fundamentalos.weather.ui.screen.SettingsScreen

val LocalHazeState = compositionLocalOf { HazeState() }

/** Where the app can be. Everything except [WeatherDestination.Home] is pushed on top of it. */
sealed interface WeatherDestination {
    data object Home : WeatherDestination
    data object Locations : WeatherDestination
    data object Map : WeatherDestination
    data object Settings : WeatherDestination
    data object DataSources : WeatherDestination
    data object About : WeatherDestination
}

private val WeatherDestinations = listOf(
    WeatherDestination.Home,
    WeatherDestination.Locations,
    WeatherDestination.Map,
    WeatherDestination.Settings,
    WeatherDestination.DataSources,
    WeatherDestination.About,
)

/** The destinations carry no data, so their position in [WeatherDestinations] is the whole state. */
private val WeatherBackStackSaver = listSaver<SnapshotStateList<WeatherDestination>, Int>(
    save = { stack -> stack.map(WeatherDestinations::indexOf) },
    restore = { saved ->
        saved.mapNotNull(WeatherDestinations::getOrNull)
            .ifEmpty { listOf(WeatherDestination.Home) }
            .toMutableStateList()
    },
)

@Composable
fun WeatherApp() {
    // Saved, or a theme change or a rotation would drop the user back on the home screen.
    val backStack = rememberSaveable(saver = WeatherBackStackSaver) {
        mutableStateListOf<WeatherDestination>(WeatherDestination.Home)
    }
    fun push(destination: WeatherDestination) {
        if (backStack.lastOrNull() != destination) backStack.add(destination)
    }
    fun pop() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    // Pushed destinations are overlays on the home screen rather than replacements for it, so
    // coming back never rebuilds it. The motion they slide by is shared with the screen below.
    val motion = remember { PushMotion() }
    // Whatever the restored back stack already names is already open; only a push animates.
    remember(motion) {
        for (depth in 1..backStack.lastIndex) motion.settle(depth)
    }
    val topDepth = backStack.lastIndex
    SideEffect { motion.topDepth = topDepth }
    // The animated background stops while a pushed screen hides it.
    // Freeze the sky as soon as a push begins, including while home is still
    // visible in parallax. It resumes once the destination has been removed.
    val homeCovered = remember(motion) { { motion.topDepth > 0 } }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Box(Modifier.fillMaxSize()) {
            CompositionLocalProvider(LocalScreenCovered provides homeCovered) {
                Box(Modifier.fillMaxSize().pushedUnder(motion, depth = 0)) {
                    HomeScreen(
                        onMapClick = { push(WeatherDestination.Map) },
                        onLocationsClick = { push(WeatherDestination.Locations) },
                    )
                }
            }
            backStack.drop(1).forEachIndexed { index, destination ->
                val depth = index + 1
                key(destination, depth) {
                    PushStackEntry(depth, motion, destination == WeatherDestination.Map,
                        onRemoved = { if (backStack.lastIndex == depth) pop() }) { close ->
                        when (destination) {
                            WeatherDestination.Locations -> LocationsScreen(onDone = close,
                                onSettingsClick = { push(WeatherDestination.Settings) })
                            WeatherDestination.Map -> MapScreen(onBackClick = close)
                            WeatherDestination.Settings -> SettingsScreen(onBackClick = close,
                                onDataSourcesClick = { push(WeatherDestination.DataSources) },
                                onAboutClick = { push(WeatherDestination.About) })
                            WeatherDestination.DataSources -> DataSourcesScreen(onBackClick = close)
                            WeatherDestination.About -> AboutScreen(onBackClick = close)
                            WeatherDestination.Home -> Unit
                        }
                    }
                }
            }
        }
    }

    LocationProvider()
}
