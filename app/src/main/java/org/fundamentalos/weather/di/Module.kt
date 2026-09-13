package org.fundamentalos.weather.di

import org.fundamentalos.weather.BuildConfig
import org.fundamentalos.weather.location.SavedPlaces
import org.fundamentalos.weather.settings.AppSettings
import org.fundamentalos.weather.viewmodel.MainViewModel
import org.fundamentalos.weather.weather.provider.ProviderRegistry
import org.fundamentalos.weather.weather.provider.WeatherProvider
import org.fundamentalos.weather.weather.provider.WeatherService
import org.fundamentalos.weather.weather.provider.fos.FosApiClient
import org.fundamentalos.weather.weather.provider.fos.FundamentalOsWeatherProvider
import org.fundamentalos.weather.weather.settings.ProviderSettingsRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val module = module {
    single { AppSettings(androidContext()) }
    single { SavedPlaces(androidContext()) }
    single {
        val context = androidContext()
        FosApiClient(BuildConfig.FOS_API_BASE_URL) { context.resources.configuration.locales[0] }
    }
    // FundamentalOS is the only weather provider.
    single { FundamentalOsWeatherProvider(get()) } bind WeatherProvider::class

    single { ProviderRegistry(getAll<WeatherProvider>().distinctBy { it.id }) }
    single { ProviderSettingsRepository(androidContext()) }
    single { WeatherService(get(), get()) }

    viewModelOf(::MainViewModel)
}
