package org.fundamentalos.weather

import android.app.Application
import androidx.compose.ui.text.android.NoFallbackLineSpacingPatch
import org.fundamentalos.weather.di.module
import org.fundamentalos.weather.ipc.WeatherRefreshWorker
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
        // The cross-process weather feed only has a consumer in the FundamentalOS (inline) build,
        // so only that build runs the periodic background refresh.
        if (BuildConfig.INLINE) {
            WeatherRefreshWorker.schedule(this)
        }
    }
}
