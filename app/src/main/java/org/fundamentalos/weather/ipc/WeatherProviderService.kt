package org.fundamentalos.weather.ipc

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteCallbackList
import org.koin.core.context.GlobalContext

/**
 * The bound, exported entry point other same-signature system apps (FundamentalIntelligence) reach
 * to read the current weather and subscribe to updates. Guarded by a signature-level permission in
 * the manifest, so only apps signed with the same platform key can bind.
 *
 * getCurrent() answers from the persisted cache (so it works on a cold bind) as an android.os.Bundle;
 * live updates are fanned out to registered callbacks as the same Bundle whenever the refresh worker
 * publishes a new value.
 */
class WeatherProviderService : Service() {

    private val callbacks = RemoteCallbackList<IWeatherCallback>()

    private val cache: WeatherProviderCache by lazy {
        GlobalContext.getOrNull()?.get<WeatherProviderCache>() ?: WeatherProviderCache(this)
    }

    private val busListener = WeatherUpdateBus.Listener { value -> broadcast(value) }

    override fun onCreate() {
        super.onCreate()
        WeatherUpdateBus.subscribe(busListener)
    }

    override fun onDestroy() {
        WeatherUpdateBus.unsubscribe(busListener)
        callbacks.kill()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private val binder = object : IWeatherProvider.Stub() {
        override fun getCurrent(): Bundle? =
            cache.load()?.let { WeatherSnapshotFactory.toBundle(this@WeatherProviderService, it) }

        override fun registerCallback(cb: IWeatherCallback?) {
            if (cb == null) return
            callbacks.register(cb)
            // Push the current value once on registration, if we have one.
            cache.load()?.let { cached ->
                runCatching {
                    cb.onWeatherChanged(
                        WeatherSnapshotFactory.toBundle(this@WeatherProviderService, cached),
                    )
                }
            }
        }

        override fun unregisterCallback(cb: IWeatherCallback?) {
            if (cb == null) return
            callbacks.unregister(cb)
        }
    }

    private fun broadcast(value: CachedWeather) {
        val snapshot = WeatherSnapshotFactory.toBundle(this, value)
        val count = callbacks.beginBroadcast()
        try {
            for (i in 0 until count) {
                runCatching { callbacks.getBroadcastItem(i).onWeatherChanged(snapshot) }
            }
        } finally {
            callbacks.finishBroadcast()
        }
    }
}
