package org.fundamentalos.weather.ipc

import java.util.concurrent.CopyOnWriteArrayList

/**
 * In-process fan-out from the refresh worker to a live [WeatherProviderService]. Both run in the
 * app's single process, so a plain listener list is enough here; cross-process delivery to bound
 * clients is the service's job (via its RemoteCallbackList), and survival across process death is
 * the cache's. This only wakes a service that is already alive and bound.
 */
internal object WeatherUpdateBus {
    fun interface Listener {
        fun onWeather(value: CachedWeather)
    }

    private val listeners = CopyOnWriteArrayList<Listener>()

    fun subscribe(listener: Listener) {
        listeners.addIfAbsent(listener)
    }

    fun unsubscribe(listener: Listener) {
        listeners.remove(listener)
    }

    fun publish(value: CachedWeather) {
        for (listener in listeners) {
            runCatching { listener.onWeather(value) }
        }
    }
}
