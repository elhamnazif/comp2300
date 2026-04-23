package com.group8.comp2300.feature.calendar

import androidx.compose.runtime.Composable
import comp2300.i18n.generated.resources.*
import kotlinx.datetime.*
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

enum class AdherenceStatus {
    TAKEN,
    MISSED,
    NONE,
    APPOINTMENT,
}

data class CalendarDay(
    val day: Int,
    val date: LocalDate,
    val status: AdherenceStatus,
    val isToday: Boolean,
    val isCurrentMonth: Boolean,
)

fun generateCalendarDays(year: Int, month: Month, overviewMap: Map<String, String> = emptyMap()): List<CalendarDay> {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val firstOfMonth = LocalDate(year, month, 1)
    val startOffset = firstOfMonth.dayOfWeek.isoDayNumber % 7
    val firstDayOfGrid = firstOfMonth.minus(startOffset, DateTimeUnit.DAY)

    return (0 until 42).map { offset ->
        val date = firstDayOfGrid.plus(offset, DateTimeUnit.DAY)
        val isToday = date == today
        val isCurrentMonth = date.month == month

        // Use real data from the calendar overview API
        val dateKey = "${date.year}-${
            date.month.number.toString().padStart(2, '0')
        }-${date.day.toString().padStart(2, '0')}"
        val overviewStatus = overviewMap[dateKey]

        val status = when {
            !isCurrentMonth -> AdherenceStatus.NONE
            overviewStatus == "TAKEN" -> AdherenceStatus.TAKEN
            overviewStatus == "MISSED" -> AdherenceStatus.MISSED
            overviewStatus == "APPOINTMENT" -> AdherenceStatus.APPOINTMENT
            else -> AdherenceStatus.NONE
        }

        CalendarDay(
            day = date.day,
            date = date,
            status = status,
            isToday = isToday,
            isCurrentMonth = isCurrentMonth,
        )
    }
}

object FormConstants {
    val MoodEmojis = listOf("😢", "😕", "😐", "🙂", "🤩")

    @Composable
    fun moodLabels() = listOf(
        stringResource(Res.string.form_mood_very_sad),
        stringResource(Res.string.form_mood_sad),
        stringResource(Res.string.form_mood_neutral),
        stringResource(Res.string.form_mood_happy),
        stringResource(Res.string.form_mood_great),
    )
}
