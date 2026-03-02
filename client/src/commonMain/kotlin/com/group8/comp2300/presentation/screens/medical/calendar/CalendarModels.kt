package com.group8.comp2300.presentation.screens.medical.calendar

import androidx.compose.runtime.Composable
import comp2300.i18n.generated.resources.*
import comp2300.i18n.generated.resources.Res
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
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

data class Doctor(val name: String)

val sampleDoctors = com.group8.comp2300.mock.sampleCalendarDoctors.map { Doctor(it.name) }

fun generateCalendarDays(
    year: Int,
    month: Month,
    overviewMap: Map<String, String> = emptyMap(),
): List<CalendarDay> {
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

enum class SheetView {
    MENU,
    FORM_MED,
    FORM_APPT,
    FORM_MOOD,
    DETAILS_APPT,
}

object FormConstants {
    @Composable
    fun commonMeds() = listOf(
        stringResource(Res.string.medication_prep),
        stringResource(Res.string.medication_truvada),
        stringResource(Res.string.medication_descovy),
        stringResource(Res.string.medication_doxypep),
        stringResource(Res.string.medication_multivitamin),
    )

    @Composable
    fun apptTypes() = listOf(
        stringResource(Res.string.appt_type_consultation),
        stringResource(Res.string.appt_type_labwork),
        stringResource(Res.string.appt_type_followup),
    )

    val Dosages = listOf(1, 2, 3)
    val MoodEmojis = listOf("😢", "😕", "😐", "🙂", "🤩")

    @Composable
    fun moodLabels() = listOf(
        stringResource(Res.string.form_mood_very_sad),
        stringResource(Res.string.form_mood_sad),
        stringResource(Res.string.form_mood_neutral),
        stringResource(Res.string.form_mood_happy),
        stringResource(Res.string.form_mood_great),
    )

    @Composable
    fun moodTags() = listOf(
        stringResource(Res.string.form_mood_tag_anxious),
        stringResource(Res.string.form_mood_tag_calm),
        stringResource(Res.string.form_mood_tag_irritable),
        stringResource(Res.string.form_mood_tag_energetic),
        stringResource(Res.string.form_mood_tag_tired),
        stringResource(Res.string.form_mood_tag_stressed),
        stringResource(Res.string.form_mood_tag_focused),
    )
}
