package org.fundamentalos.weather.ipc

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.fundamentalos.weather.weather.provider.WeatherService
import org.koin.core.context.GlobalContext
import java.util.concurrent.TimeUnit

/**
 * Periodic refresh behind the IPC provider: resolve the device location, fetch one weather snapshot
 * through the app's existing [WeatherService], persist the scalars and push them to any live bound
 * client. Dependencies come from Koin's global context so the default WorkerFactory can build the
 * worker with no custom configuration.
 */
class WeatherRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val koin = GlobalContext.getOrNull() ?: return Result.retry()
        val weatherService = koin.get<WeatherService>()
        val resolver = koin.get<WeatherLocationResolver>()
        val cache = koin.get<WeatherProviderCache>()

        val fix = resolver.resolve() ?: run {
            Log.w(TAG, "refresh: no location fix")
            return Result.retry()
        }

        return try {
            val domain = weatherService.getWeather(fix.latitude, fix.longitude)
            val cached = WeatherSnapshotFactory.toCached(domain)
            cache.save(cached)
            WeatherUpdateBus.publish(cached)
            Log.d(TAG, "refresh: ok, ${cached.description} ${cached.temperature}")
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "refresh: failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "WeatherRefreshWorker"
        private const val PERIODIC_NAME = "weather_ipc_refresh"
        private const val IMMEDIATE_NAME = "weather_ipc_refresh_now"
        private const val INTERVAL_MINUTES = 30L

        /** Enqueue the periodic refresh (kept if already scheduled) plus one immediate run. */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodic = PeriodicWorkRequestBuilder<WeatherRefreshWorker>(
                INTERVAL_MINUTES, TimeUnit.MINUTES,
            ).setConstraints(constraints).build()

            val immediate = OneTimeWorkRequestBuilder<WeatherRefreshWorker>()
                .setConstraints(constraints)
                .build()

            val workManager = WorkManager.getInstance(context.applicationContext)
            workManager.enqueueUniquePeriodicWork(
                PERIODIC_NAME, ExistingPeriodicWorkPolicy.KEEP, periodic,
            )
            workManager.enqueueUniqueWork(
                IMMEDIATE_NAME, ExistingWorkPolicy.KEEP, immediate,
            )
        }
    }
}
