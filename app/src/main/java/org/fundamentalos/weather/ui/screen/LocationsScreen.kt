package org.fundamentalos.weather.ui.screen

import org.fundamentalos.weather.R
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kyant.shapes.RoundedRectangle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import org.fundamentalos.weather.ui.components.CollapsingLargeTitle
import org.fundamentalos.weather.ui.components.CollapsingTitle
import org.fundamentalos.weather.ui.components.rememberCollapsingTitle
import org.fundamentalos.weather.ui.components.AppBarIconAlignmentPad
import org.fundamentalos.weather.ui.components.BlurOnlyHazeStyle
import org.fundamentalos.weather.ui.components.GlassTopAppBar
import org.fundamentalos.weather.ui.components.StatusBarAppearance
import org.fundamentalos.weather.ui.components.topEdgeBlur
import org.fundamentalos.weather.ui.components.iosOverscroll
import org.fundamentalos.weather.ui.components.rememberIosFlingBehavior
import org.fundamentalos.weather.ui.components.rememberIosOverscrollState
import org.fundamentalos.weather.location.SavedPlace
import org.fundamentalos.weather.location.SavedPlaces
import org.fundamentalos.weather.viewmodel.MainViewModel
import org.fundamentalos.weather.weather.provider.fos.FosApiClient
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

private val FieldHeight = 72.dp
private val FieldInset = 16.dp
private val FieldPadding = 24.dp
private val FieldCorner = 24.dp

/** Where the field rests once a search starts: this far above the keyboard, or the gesture bar. */
private val SearchFieldBottomGap = 24.dp

private val RowHeight = 72.dp
private val RowTextInset = 40.dp
private val RowPartGap = 24.dp
private val DividerInset = 24.dp

/** Where the search state's back arrow lines up with the bar's own. */
private val AppBarIconTopGap = 8.dp

/** The gap the design leaves between the title and the field. */
private val TitleToFieldGap = 45.dp

/** Results run to the top of the screen and fade out under this scrim. */
private val ResultsTopInset = 84.dp
private val ScrimHeight = 140.dp

private const val SearchDebounceMillis = 250L

private val TravelSpec = tween<Float>(320, easing = FastOutSlowInEasing)

/**
 * The places the app knows: the device's own at the top, then the saved ones, then a way to add
 * more. Opening one shows its weather and closes the page.
 *
 * One field serves both states: it rests under the title while the places are listed, and travels
 * down above the keyboard once a search starts, with the results filling the screen behind it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationsScreen(
    onDone: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    savedPlaces: SavedPlaces = koinInject(),
    api: FosApiClient = koinInject(),
    vm: MainViewModel = koinViewModel(),
) {
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SavedPlace>>(emptyList()) }
    val travel = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    // Read in the layout phase, so the field travels without recomposing anything. NaN until the
    // layout has said where the field rests; the resting place goes negative once the list scrolls.
    val restY = remember { mutableFloatStateOf(Float.NaN) }
    val searchY = remember { mutableFloatStateOf(Float.NaN) }

    LaunchedEffect(searching) {
        travel.animateTo(if (searching) 1f else 0f, TravelSpec)
        if (searching) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }

    LaunchedEffect(query, searching) {
        if (!searching || query.isBlank()) {
            results = emptyList()
            return@LaunchedEffect
        }
        delay(SearchDebounceMillis)
        results = runCatching { api.search(query.trim(), limit = 20).map(SavedPlace::of) }
            .getOrDefault(emptyList())
    }

    fun leaveSearch() {
        searching = false
        query = ""
        keyboard?.hide()
    }

    // While searching, back leaves the search rather than the page; the gesture drives the same
    // travel a tap on the arrow does, so releasing it hands over mid-slide.
    PredictiveBackHandler(enabled = searching) { events ->
        try {
            events.collect { travel.snapTo(1f - it.progress) }
            leaveSearch()
        } catch (_: CancellationException) {
            // The back handler's coroutine is already cancelled here, so the recovery animation
            // must run in the composition scope — a suspend on this job throws before it can start.
            scope.launch { travel.animateTo(1f, TravelSpec) }
        }
    }

    val here = vm.deviceLocation.value?.let {
        SavedPlace(it.name, it.city, it.province, it.country, it.latitude, it.longitude)
    }

    // What the bar blurs: the places behind it as they scroll up.
    val listHaze = remember { HazeState() }
    val collapsingTitle = rememberCollapsingTitle()

    StatusBarAppearance(lightBackground = !isSystemInDarkTheme())
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainer) {
        Box(Modifier.fillMaxSize()) {
            SavedPlacesList(
                places = savedPlaces.places,
                onOpen = { place ->
                    vm.selectPlace(place.latitude, place.longitude)
                    onDone()
                },
                onRemove = savedPlaces::remove,
                onAdd = { searching = true },
                onFieldSlotPlaced = { restY.floatValue = it },
                collapsingTitle = collapsingTitle,
                modifier = Modifier
                    .hazeSource(listHaze)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .graphicsLayer { alpha = 1f - travel.value },
            )

            if (travel.value > 0f) {
                SearchResultsLayer(
                    results = results,
                    savedPlaces = savedPlaces,
                    here = here,
                    onBack = ::leaveSearch,
                    onFieldSlotPlaced = { searchY.floatValue = it },
                    modifier = Modifier.graphicsLayer { alpha = travel.value },
                )
            }

            SearchField(
                query = query,
                onQueryChange = { query = it },
                searching = searching,
                deviceLocation = here?.name ?: stringResource(vm.locationLabel),
                onUseLocation = {
                    vm.useDeviceLocation()
                    onDone()
                },
                focusRequester = focusRequester,
                // A source of its own, above the list: without this the bar's frosted copy of the
                // list simply covers the field as it scrolls under.
                // Both ends are measured rather than computed: the window resizes for the
                // keyboard on some devices and reports it as an inset on others.
                modifier = Modifier
                    .hazeSource(listHaze, zIndex = 1f)
                    .offset {
                        val rest = restY.floatValue
                        val search = searchY.floatValue
                        val y = when {
                            rest.isNaN() -> search
                            search.isNaN() -> rest
                            else -> lerp(rest, search, travel.value)
                        }
                        IntOffset(0, if (y.isNaN()) 0 else y.roundToInt())
                    }
                    // Nothing to show until one of the two ends has been placed.
                    .graphicsLayer {
                        alpha = if (restY.floatValue.isNaN() && searchY.floatValue.isNaN()) 0f else 1f
                    },
            )

        }

        GlassTopAppBar(
            hazeState = listHaze,
            title = { Text(stringResource(R.string.add_location)) },
            onBack = onDone,
            actions = {
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.padding(end = AppBarIconAlignmentPad),
                ) {
                    Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings))
                }
            },
            collapsingTitle = collapsingTitle,
            modifier = Modifier.graphicsLayer { alpha = 1f - travel.value },
        )
    }
}

@Composable
private fun SavedPlacesList(
    places: List<SavedPlace>,
    onOpen: (SavedPlace) -> Unit,
    onRemove: (SavedPlace) -> Unit,
    onAdd: () -> Unit,
    onFieldSlotPlaced: (Float) -> Unit,
    collapsingTitle: CollapsingTitle,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberLazyListState()
    val overscrollState = rememberIosOverscrollState()
    LazyColumn(
        state = scrollState,
        flingBehavior = rememberIosFlingBehavior(scrollState),
        modifier = modifier
            .fillMaxSize()
            .iosOverscroll(overscrollState),
    ) {
        item {
            Column {
                CollapsingLargeTitle(
                    title = stringResource(R.string.add_location),
                    collapsingTitle = collapsingTitle,
                    icon = Icons.Outlined.LocationOn,
                )
                // Reserve the slot for the search field drawn above this list.
                Spacer(
                    Modifier.padding(top = TitleToFieldGap).fillMaxWidth().height(FieldHeight)
                        .onGloballyPositioned { onFieldSlotPlaced(it.positionInRoot().y) },
                )
            }
        }
        items(places) { place ->
            PlaceRow(place = place, onClick = { onOpen(place) }) {
                RowAction(Icons.Default.Close, stringResource(R.string.remove)) { onRemove(place) }
            }
        }
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().height(RowHeight)
                        .clickable(onClick = onAdd).padding(start = RowTextInset),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.add),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.height(24.dp).navigationBarsPadding())
            }
        }
    }
}

@Composable
private fun SearchResultsLayer(
    results: List<SavedPlace>,
    savedPlaces: SavedPlaces,
    here: SavedPlace?,
    onBack: () -> Unit,
    onFieldSlotPlaced: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hazeState = remember { HazeState() }
    val hazeStyle = BlurOnlyHazeStyle()
    Box(modifier.fillMaxSize()) {
        val listState = rememberLazyListState()
        val overscrollState = rememberIosOverscrollState()
        LazyColumn(
            state = listState,
            flingBehavior = rememberIosFlingBehavior(listState),
            modifier = Modifier
                .fillMaxSize()
                .iosOverscroll(overscrollState)
                .hazeSource(hazeState)
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentPadding = PaddingValues(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() +
                    ResultsTopInset,
                bottom = 160.dp,
            ),
        ) {
            items(results, key = { "${it.latitude},${it.longitude},${it.name}" }) { place ->
                PlaceRow(place = place, onClick = { savedPlaces.add(place) }) {
                    if (here != null && here.isAround(place)) {
                        Icon(
                            Icons.Default.NearMe,
                            contentDescription = stringResource(R.string.current_location),
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.width(16.dp))
                    }
                    if (savedPlaces.contains(place)) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = stringResource(R.string.added),
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    } else {
                        RowAction(Icons.Default.Add, stringResource(R.string.add)) { savedPlaces.add(place) }
                    }
                }
            }
        }
        // The list runs to the top of the screen and dissolves under this rather than being cut:
        // the blur ramps up towards the top edge and the tint takes over where it is strongest.
        Box(
            Modifier
                .fillMaxWidth()
                .height(ScrimHeight)
                .topEdgeBlur(hazeState, hazeStyle),
        )
        // The field travels here from the saved-places layout; this is where it comes to rest.
        Spacer(
            Modifier
                .align(Alignment.BottomStart)
                .imePadding()
                .navigationBarsPadding()
                .padding(bottom = SearchFieldBottomGap)
                .fillMaxWidth()
                .height(FieldHeight)
                .onGloballyPositioned { onFieldSlotPlaced(it.positionInRoot().y) },
        )
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 4.dp + AppBarIconAlignmentPad, top = AppBarIconTopGap),
        ) {
            Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = stringResource(R.string.back))
        }
    }
}

/** A place, as the mock lists it: the place itself, then the chain above it, then one action. */
@Composable
private fun PlaceRow(
    place: SavedPlace,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(RowHeight)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(start = RowTextInset, end = RowTextInset),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // A city is often its own district, and the server says so twice; show it once.
                listOfNotNull(place.city, place.province)
                    .filterNot { it == place.name }
                    .distinct()
                    .forEach { part ->
                        Spacer(Modifier.width(RowPartGap))
                        Text(
                            text = part,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
            }
            Spacer(Modifier.width(RowPartGap))
            trailing()
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = DividerInset),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@Composable
private fun RowAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    Icon(
        imageVector = icon,
        contentDescription = description,
        modifier = Modifier
            .size(24.dp)
            .clickable(
                interactionSource = null,
                indication = ripple(bounded = false, radius = 24.dp),
                onClick = onClick,
            ),
        tint = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    searching: Boolean,
    deviceLocation: String,
    onUseLocation: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = FieldInset)
            .height(FieldHeight)
            .clip(RoundedRectangle(FieldCorner))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(enabled = !searching, onClick = onUseLocation)
            .padding(horizontal = FieldPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                enabled = searching,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
            if (query.isEmpty()) {
                Text(
                    text = if (searching) stringResource(R.string.search_places) else deviceLocation,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (searching) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        if (searching) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        } else {
            RowAction(Icons.Default.NearMe, stringResource(R.string.use_current_location), onUseLocation)
        }
    }
}
