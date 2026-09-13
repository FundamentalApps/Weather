package org.fundamentalos.weather.weather.provider

import org.fundamentalos.weather.weather.domain.WeatherSnapshot
import org.fundamentalos.weather.weather.settings.ProviderSettingsRepository

class WeatherService(
    private val registry: ProviderRegistry,
    private val settings: ProviderSettingsRepository,
) {
    val providers: List<WeatherProvider>
        get() = registry.providers

    val selectedProviderId: String
        get() = settings.selectedProviderId

    fun selectProvider(providerId: String) {
        require(registry.provider(providerId) != null) { "Unknown weather provider: $providerId" }
        settings.selectedProviderId = providerId
    }

    suspend fun getWeather(latitude: Double, longitude: Double): WeatherSnapshot {
        val selected = registry.provider(settings.selectedProviderId) ?: registry.defaultProvider()
        val candidates = buildList {
            add(selected)
            if (settings.fallbackEnabled) {
                registry.providers
                    .filter { it.id != selected.id && it.capabilities.containsAll(CoreCapabilities) }
                    .forEach { add(it) }
            }
        }

        var lastError: Throwable? = null
        for (provider in candidates) {
            try {
                return provider.getWeather(latitude, longitude)
            } catch (e: Throwable) {
                lastError = e
            }
        }

        throw WeatherProviderException("All weather providers failed", lastError)
    }

    companion object {
        private val CoreCapabilities = setOf(
            WeatherCapability.CurrentWeather,
            WeatherCapability.DailyForecast,
            WeatherCapability.HourlyForecast,
        )
    }
}
