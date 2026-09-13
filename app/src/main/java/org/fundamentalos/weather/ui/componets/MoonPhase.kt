package org.fundamentalos.weather.ui.componets

import org.fundamentalos.weather.R
import kotlinx.datetime.LocalDate
import kotlin.math.PI
import kotlin.math.cos

/** Mean length of a lunation, in days. */
private const val SYNODIC_MONTH = 29.530588853

/**
 * A known new moon, as days since the Unix epoch: 2000-01-06 18:14 UTC, which is
 * JD 2451550.26, and JD 2440587.5 is the epoch itself.
 */
private const val REFERENCE_NEW_MOON_EPOCH_DAY = 10962.76

/**
 * Age of the moon in days for [date], counted from the previous new moon.
 *
 * Mean-lunation approximation: good to roughly half a day, which never changes the reported phase
 * name by more than the boundary hours. The weather sources do not carry moon data, so this is
 * computed locally rather than fetched.
 */
fun moonAgeDays(date: LocalDate): Double {
    val days = date.toEpochDays().toDouble() - REFERENCE_NEW_MOON_EPOCH_DAY
    val age = days % SYNODIC_MONTH
    return if (age < 0) age + SYNODIC_MONTH else age
}

/** Lit fraction of the moon's disc on [date], 0 at new moon and 1 at full. */
fun moonIlluminationFraction(date: LocalDate): Double =
    (1.0 - cos(2.0 * PI * moonAgeDays(date) / SYNODIC_MONTH)) / 2.0

/** True while the lit side is growing, which puts the lit limb on the right in the northern sky. */
fun moonIsWaxing(date: LocalDate): Boolean = moonAgeDays(date) < SYNODIC_MONTH / 2.0

/** Days from [date] to the next full moon. */
fun daysToNextFullMoon(date: LocalDate): Double {
    val full = SYNODIC_MONTH / 2.0
    val age = moonAgeDays(date)
    return if (age <= full) full - age else SYNODIC_MONTH - age + full
}

/** Resource ID for the phase; astronomy calculations remain language-independent. */
fun moonPhaseName(date: LocalDate): Int {
    val age = moonAgeDays(date)
    val eighth = SYNODIC_MONTH / 8.0
    return when (((age + eighth / 2.0) / eighth).toInt() % 8) {
        0 -> R.string.moon_new
        1 -> R.string.moon_waxing_crescent
        2 -> R.string.moon_first_quarter
        3 -> R.string.moon_waxing_gibbous
        4 -> R.string.moon_full
        5 -> R.string.moon_waning_gibbous
        6 -> R.string.moon_last_quarter
        else -> R.string.moon_waning_crescent
    }
}
