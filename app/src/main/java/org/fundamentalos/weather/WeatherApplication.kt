package org.fundamentalos.weather

import android.app.Application
import androidx.compose.ui.text.android.NoFallbackLineSpacingPatch
import org.fundamentalos.weather.di.module
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

class WeatherApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NoFallbackLineSpacingPatch.install()
        startKoin {
            androidContext(this@WeatherApplication)
            modules(module)
        }
    }
}
