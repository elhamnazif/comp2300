package com.group8.comp2300.util

import kotlinx.datetime.LocalDate

/**
 * Converts a [LocalDate] to epoch milliseconds.
 * Returns null if the date is null.
 */
fun LocalDate?.toEpochMilliseconds(): Long? = this?.toEpochDays()?.let { epochDays ->
    epochDays * 24L * 60L * 60L * 1000L
}
