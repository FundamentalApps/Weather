package org.fundamentalos.weather.weather.provider

internal fun String?.toIntOrNullSafe(): Int? = this?.toIntOrNull()

internal fun String?.toDoubleOrNullSafe(): Double? = this?.toDoubleOrNull()

internal fun Int?.orZero(): Int = this ?: 0
