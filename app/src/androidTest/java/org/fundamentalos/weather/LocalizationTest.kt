package org.fundamentalos.weather

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.fundamentalos.weather.ui.text.conditionText
import org.fundamentalos.weather.ui.text.localizedDays
import org.fundamentalos.weather.ui.text.localizedForecastDate
import org.fundamentalos.weather.ui.text.localizedNumber
import org.fundamentalos.weather.ui.text.windDirectionText
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class LocalizationTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun contextFor(language: String) = context.createConfigurationContext(
        Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) }
    )

    @Test
    fun appNameAndPluralRules() {
        val names = mapOf("en" to "Weather", "de" to "Wetter", "fr" to "Météo",
            "pl" to "Pogoda", "ru" to "Погода", "zh" to "天气")
        names.forEach { (language, name) ->
            val localized = contextFor(language)
            assertEquals(name, localized.getString(R.string.app_name))
            // Exercise integer and string placeholders in the Android resource formatter.
            assertEquals(false, localized.getString(R.string.dew_point, 12).contains("%1"))
            assertEquals(false, localized.getString(R.string.sunrise_at, "06:30").contains("%1"))
        }
        val russian = contextFor("ru").resources
        assertEquals("1 день", russian.getQuantityString(R.plurals.duration_days, 1, "1"))
        assertEquals("2 дня", russian.getQuantityString(R.plurals.duration_days, 2, "2"))
        assertEquals("5 дней", russian.getQuantityString(R.plurals.duration_days, 5, "5"))
        assertEquals("21 день", russian.getQuantityString(R.plurals.duration_days, 21, "21"))
        val polish = contextFor("pl").resources
        assertEquals("1 dzień", polish.getQuantityString(R.plurals.duration_days, 1, "1"))
        assertEquals("2 dni", polish.getQuantityString(R.plurals.duration_days, 2, "2"))
        assertEquals("5 dni", polish.getQuantityString(R.plurals.duration_days, 5, "5"))
    }

    @Test
    fun displayTextReactsToAppLocaleWithoutRefetchingWeather() {
        val language = mutableStateOf("en")
        compose.setContent {
            val localized = contextFor(language.value)
            CompositionLocalProvider(LocalContext provides localized,
                LocalConfiguration provides localized.resources.configuration) {
                Column {
                    Text(conditionText("305", "server text"))
                    Text(localizedForecastDate(LocalDate.now().toString()))
                    Text(localizedDays(2))
                    Text(windDirectionText(90))
                    Text(localizedNumber(1.5, 1))
                }
            }
        }
        listOf("en", "de", "fr", "pl", "ru", "zh").forEach { tag ->
            compose.runOnIdle { language.value = tag }
            val localized = contextFor(tag)
            compose.onNodeWithText(localized.getString(R.string.condition_light_rain)).assertExists()
            compose.onNodeWithText(localized.getString(R.string.today)).assertExists()
            compose.onNodeWithText(localized.getString(R.string.east)).assertExists()
        }
        compose.runOnIdle { language.value = "de" }
        compose.onNodeWithText("1,5").assertExists()
    }
}
