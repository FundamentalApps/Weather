package org.fundamentalos.weather.weather

import org.fundamentalos.weather.weather.provider.fos.FosApiClient
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class ProviderLanguageTest {
    @Test fun fundamentalOsUsesDocumentedServerFallback() {
        assertEquals("zh", FosApiClient.preferredLang(Locale.SIMPLIFIED_CHINESE))
        listOf("de", "en", "fr", "pl", "ru").forEach { language ->
            assertEquals("en", FosApiClient.preferredLang(Locale.forLanguageTag(language)))
        }
    }
}
