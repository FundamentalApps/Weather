package org.fundamentalos.weather.ipc

import android.app.PendingIntent
import android.graphics.drawable.Icon
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * The weather values that cross the process boundary to a bound client (ASI Alt), which turns them
 * into a smartspace target. The field order, names and types are the frozen IPC contract: changing
 * them breaks every consumer, so treat this as an API.
 *
 * [conditionIcon] is the app's own weather glyph, addressed by resource so a consumer in another
 * process loads it from this app's package; the lock screen shows it directly. [tapIntent] deep
 * links back into the app.
 */
@Parcelize
data class WeatherSnapshot(
    val temperature: Double,
    val useCelsius: Boolean,
    val wmoCode: Int,
    val isDay: Boolean,
    val description: String,
    val conditionIcon: Icon?,
    val locationName: String?,
    val observationTimeMillis: Long,
    val validUntilMillis: Long,
    val tapIntent: PendingIntent?,
) : Parcelable
