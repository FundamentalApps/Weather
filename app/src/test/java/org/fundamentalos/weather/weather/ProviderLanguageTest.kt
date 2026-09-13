package org.fundamentalos.weather.weather

import org.fundamentalos.weather.weather.provider.fos.FosApiClient
import org.fundamentalos.weather.weather.provider.qweather.QWeatherApiClient
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class ProviderLanguageTest {
    @Test fun qweatherReceivesSupportedAppLanguage() {
        listOf("de", "en", "fr", "pl", "ru", "zh").forEach { language ->
            assertEquals(language, QWeatherApiClient.preferredLanguage(Locale.forLanguageTag(language)))
        }
        assertEquals("zh-hant", QWeatherApiClient.preferredLanguage(Locale.forLanguageTag("zh-TW")))
        assertEquals("en", QWeatherApiClient.preferredLanguage(Locale.forLanguageTag("xx")))
    }

    @Test fun fundamentalOsUsesDocumentedServerFallback() {
        assertEquals("zh", FosApiClient.preferredLang(Locale.SIMPLIFIED_CHINESE))
        listOf("de", "en", "fr", "pl", "ru").forEach { language ->
            assertEquals("en", FosApiClient.preferredLang(Locale.forLanguageTag(language)))
        }
    }
}
