package org.fundamentalos.weather.weather.provider

class ProviderRegistry(
    providers: List<WeatherProvider>,
) {
    val providers: List<WeatherProvider> = providers.distinctBy { it.id }

    fun defaultProvider(): WeatherProvider = provider(FundamentalOsProviderId)
        ?: providers.firstOrNull()
        ?: error("No weather providers registered")

    fun provider(id: String): WeatherProvider? = providers.firstOrNull { it.id == id }

    fun providersWith(capability: WeatherCapability): List<WeatherProvider> {
        return providers.filter { capability in it.capabilities }
    }

    companion object {
        const val FundamentalOsProviderId = "fundamentalos"
    }
}
