# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Is

Caelum is a native Android weather app built with Kotlin and Jetpack Compose. It fetches weather data from a companion Rust backend (`../caelum-server`) which proxies [QWeather](https://dev.qweather.com/) API calls. The backend handles JWT auth, caching (Redis), and database persistence (MySQL).

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

API base URLs are loaded from `local.properties` (not committed). The file needs entries for dev/prod API endpoints before the app builds.

## Architecture

**Pattern**: Single-Activity MVVM with Jetpack Compose UI.

**Dependency Injection**: Koin — modules defined in `app/src/main/java/ink/duo3/caelum/di/Module.kt`. The `CaelumApplication` bootstraps Koin.

**Data flow**:
```
HomeScreen (Compose)
  └── MainViewModel (StateFlow + coroutines)
        └── WeatherModule (API layer, Ktor client)
              └── caelum-server HTTP endpoints (/weather/*)
```

**Location acquisition** (`MainViewModel`): Tries 5 sources in priority order — GPS, network, passive, last-known, and a fallback. Location is passed as lat/lon to the server.

**API client** (`CaelumApiClient` + `WeatherModule`): Ktor-based HTTP client with `kotlinx.serialization`. All responses are wrapped in `WebResp<T>`. The single `/weather/all` endpoint fetches current, forecast, hourly, and AQI in one call.

**Room database**: Used for caching weather data locally. Schema migrations live in `app/schemas/`. KSP generates the DAOs.

## Key Libraries

| Purpose | Library |
|---|---|
| UI | Jetpack Compose + Material3 |
| HTTP | Ktor Client 3.x (Android engine) |
| Serialization | `kotlinx.serialization` |
| DI | Koin 4.x |
| DB | Room 2.7 (KSP) |
| Date/Time | `kotlinx-datetime` |
| Blur/glass effects | Haze |
| Permissions | Accompanist Permissions |

All versions are managed in `gradle/libs.versions.toml`.

## caelum-server (../caelum-server)

The backend is a Rust/Axum REST API. Relevant commands:

```bash
cargo build --release
cargo run --release    # reads application.yaml for config
cargo test
```

Config is YAML (`application.yaml`): server bind address, MySQL, Redis, and QWeather credentials including an Ed25519 PEM private key for JWT. Docker deployment uses a multi-stage Cargo-Chef build.

**Endpoints** (all under `/weather/`):
- `getCityByLocation` — reverse geocoding (lat/lon → city)
- `now` — current conditions by city ID
- `10d` — 10-day forecast
- `24h` — hourly forecast
- `aqiNow` — air quality by coordinates
- `all` — unified endpoint combining the above

The server generates short-lived EdDSA JWTs (1-hour expiry, auto-refreshed) to authenticate with QWeather's API.
