package com.group8.comp2300.presentation.util

import androidx.compose.runtime.Composable
import comp2300.i18n.generated.resources.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

object DateFormatter {

    /**
     * Formats a timestamp to "Month Day, Year" (e.g., "Jan 21, 2024").
     * Requires Composable context for string resources.
     */
    @Composable
    fun formatMonthDayYear(timestamp: Long): String {
        val instant = Instant.fromEpochMilliseconds(timestamp)
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val monthRes = getMonthResource(localDateTime.month.number)
        return "${stringResource(monthRes)} ${localDateTime.day}, ${localDateTime.year}"
    }

    /**
     * Formats a LocalDate to "Month Year" (e.g., "January 2024").
     * Requires Composable context for string resources.
     */
    @Composable
    fun formatMonthYear(date: LocalDate): String {
        val monthRes = getMonthResource(date.month.number)
        return "${stringResource(monthRes)} ${date.year}"
    }

    /**
     * Formats a LocalDate to "DD/MM/YYYY" (e.g., "21/01/2024").
     */
    fun formatDayMonthYear(date: LocalDate): String {
        val day = date.day.toString().padStart(2, '0')
        val month = date.month.number.toString().padStart(2, '0')
        return "$day/$month/${date.year}"
    }

    /**
     * Formats a timestamp to "DD/MM/YYYY" (e.g., "21/01/2024").
     */
    fun formatDayMonthYear(timestamp: Long): String {
        val instant = Instant.fromEpochMilliseconds(timestamp)
        val date = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
        return formatDayMonthYear(date)
    }

    /**
     * Formats a time to "H:MM AM/PM" (e.g., "09:00 AM").
     */
    fun formatTime(hour: Int, minute: Int): String {
        val amPm = if (hour < 12) "AM" else "PM"
        val hour12 = if (hour == 0 || hour == 12) 12 else hour % 12
        return "$hour12:${minute.toString().padStart(2, '0')} $amPm"
    }

    private fun getMonthResource(monthNumber: Int): StringResource = when (monthNumber) {
        1 -> Res.string.month_jan
        2 -> Res.string.month_feb
        3 -> Res.string.month_mar
        4 -> Res.string.month_apr
        5 -> Res.string.month_may
        6 -> Res.string.month_jun
        7 -> Res.string.month_jul
        8 -> Res.string.month_aug
        9 -> Res.string.month_sep
        10 -> Res.string.month_oct
        11 -> Res.string.month_nov
        12 -> Res.string.month_dec
        else -> Res.string.month_jan
    }
}
