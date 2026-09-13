package org.fundamentalos.weather.weather.settings

import android.content.Context
import org.fundamentalos.weather.weather.provider.ProviderRegistry

class ProviderSettingsRepository(context: Context) {
    private val preferences = context.getSharedPreferences("weather_provider_settings", Context.MODE_PRIVATE)

    var selectedProviderId: String
        get() = preferences.getString(KEY_SELECTED_PROVIDER, ProviderRegistry.FundamentalOsProviderId)
            ?: ProviderRegistry.FundamentalOsProviderId
        set(value) {
            preferences.edit().putString(KEY_SELECTED_PROVIDER, value).apply()
        }

    var fallbackEnabled: Boolean
        get() = preferences.getBoolean(KEY_FALLBACK_ENABLED, true)
        set(value) {
            preferences.edit().putBoolean(KEY_FALLBACK_ENABLED, value).apply()
        }

    companion object {
        private const val KEY_SELECTED_PROVIDER = "selected_provider"
        private const val KEY_FALLBACK_ENABLED = "fallback_enabled"
    }
}
