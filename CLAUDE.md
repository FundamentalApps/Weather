# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Is

Weather is a native Android weather app (`org.fundamentalos.weather`) built with Kotlin and Jetpack Compose. Weather data comes from the **FundamentalOS** backend (a hosted API at `https://api.fundamentalos.org`), which aggregates upstream sources — forecast and air quality from Open-Meteo, place names from GeoNames, IP geolocation from DB-IP Lite (see `DataSourcesScreen` for the full credit list). This repo is the Android client only; the backend is a separate project.

## Build & Run

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Unit tests
./gradlew test

# Instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest
```

Version code is derived from `git rev-list --count --first-parent HEAD`; version name from git tags matching `v[0-9]*`.

`local.properties` (not committed) may set `app.fosApiBaseUrl` to point at a different FundamentalOS API base URL; it defaults to `https://api.fundamentalos.org`.

## Architecture

**Pattern**: Single-Activity MVVM with Jetpack Compose. `WeatherApplication` bootstraps Koin; `MainActivity` hosts `WeatherApp`, which owns the back stack and drives the screens.

**Dependency Injection**: Koin — module in `app/src/main/java/org/fundamentalos/weather/di/Module.kt`.

**Data flow**:
```
WeatherApp / HomeScreen (Compose)
  └── MainViewModel (StateFlow + coroutines)
        └── WeatherService ──► ProviderRegistry ──► FundamentalOsWeatherProvider ──► FosApiClient
```

**Weather providers** (`weather/provider/`): the app is built around a small provider abstraction — `WeatherProvider` (the interface), `ProviderRegistry` (holds the registered providers, deduped by id, and names the default), and `WeatherService` (what the ViewModel calls). Today **`FundamentalOsWeatherProvider` is the only registered provider**; the abstraction is kept so another source could be added without touching the UI. Domain models live in `weather/domain/`; the provider maps its wire models (`fos/FosModels`) into them.

**FundamentalOS API** (`FosApiClient`): a Ktor client with `kotlinx.serialization`. Endpoints: `snapshot` (lat/lon → current conditions, forecast, hourly, and air quality in one call), `reverse` (lat/lon → place), `search` (query → places), `mapLayers` (weather-map overlays), `locateByIp` (server-side IP geolocation).

**Weather map** (`ui/screen/MapScreen.kt`, `ui/map/`): osmdroid for the camera only — its raster tile overlay is disabled — limited to one copy of the world. A field layer such as temperature is not tiled: `WeatherFieldStore` fetches it in whole square chunks of the world (level 0 is one image of the world, level 3 is 8 × 8), turns the source's categorical bands into a smooth colour field (`TemperatureField`), keeps the result in memory for the life of the app and on disk in `cacheDir/weather-field/`, and `WeatherFieldOverlay` stretches the chunk under the map at any zoom — so a pan or a zoom is a redraw, never a request. Above the field, `MapInkOverlay` draws the map itself from OpenStreetMap's vector tiles (`vector.openstreetmap.org`, Shortbread schema; decoded by the dependency-free `MvtDecoder`, kept by `InkTileStore`): water, motorways and trunk roads (nothing finer), borders, and only the names of districts, cities, regions and countries, in the app language via the tiles' `name_*` keys — in two grains: a region grain below zoom 8 (coasts, borders, motorways from zoom 6, countries, regions, cities of a million) and a city grain from 8 (trunk roads and every district). Each tile's lines are rasterised once, off the main thread, into two ALPHA_8 coverage masks (water, lines) at one and a half times the tile size, coloured for the theme at draw time, and names into small cached bitmaps — drawing the stroked paths every frame cost 200 ms a frame; the coast is a hairline, since stroking a hundred thousand corners was most of the cost of setting a tile. Tiles are only asked for, or set, once the zoom holds still, so a pinch does not set every level it passes through. The two coarser levels over the screen as it would be at their zoom, and the world, are baked ahead (fetched and decoded) and the nearest of them set as well, so a zoom out of one level is ready; while a tile is not set, the set tiles below it (sharp, shrunk) and the nearest set tile above it (stretched) stand in. `LocationBubbleOverlay` marks the home screen's place with the temperature now and today's range.

**Home page** (`ui/screen/Home*.kt`): `HomeScreen` is the layout only — upright, the headline pinned over the scrolling cards and shrinking with them; sideways (Apple Weather's layout) the headline and the quick-info chips stand still at the left, the other cards scroll at the right, and the place chip sits beside the map button. `HomeReading.kt` holds the reading on show — `HomeContent`, the view model's state taken in one go so the page changes as a whole — and `HomeTransition`, which animates a change of reading (indicator out, headline and cards in; or cards fade down, reading swaps, cards fade back up, the sky a step behind); `HomeCards.kt` the cards in order and the detail tiles; `HomeTheme.kt` the colour scheme and glass backdrop the sky gives the content. The view model publishes domain models only; `ui/components/ForecastMapping.kt` turns forecasts into the cards' rows and picks their icons.

**Warnings & icons**: severity colors and weather-icon codes use a shared code format the backend returns — `WarningSeverity.fromSeverityColor`, `WarningIcons`, `WarningTheme`, and the icon tables in `ForecastMapping.kt` — so labels do not depend on any single upstream source or on server language.

**Location acquisition** (`MainViewModel`): tries system sources in priority order (GPS, network, passive, last-known) and falls back to the server's IP geolocation (`FosApiClient.locateByIp`) when they come up empty. The chosen location is passed as lat/lon to the provider.

**Persistence**: lightweight state only, via `SharedPreferences` (`AppSettings`, `SavedPlaces`). There is no local database.

## Key Libraries

| Purpose | Library |
|---|---|
| UI | Jetpack Compose + Material3 |
| HTTP | Ktor Client 3.x (Android engine) |
| Serialization | `kotlinx.serialization` |
| DI | Koin 4.x |
| Date/Time | `kotlinx-datetime` |
| Blur/glass effects | Haze |
| Permissions | Accompanist Permissions |

All versions are managed in `gradle/libs.versions.toml`.

## Tools

`tools/palette-lab/` is a standalone WebGL page ("Weather Palette Lab") for designing the sky/weather color fields — pick a weather archetype, tune four colors, preview the shader — before folding the result into the app's sky and theme.
