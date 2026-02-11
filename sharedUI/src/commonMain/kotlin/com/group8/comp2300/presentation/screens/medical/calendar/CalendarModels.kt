package com.group8.comp2300.presentation.screens.medical.calendar

import androidx.compose.runtime.Composable
import comp2300.i18n.generated.resources.*
import comp2300.i18n.generated.resources.Res
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

enum class AdherenceStatus {
    TAKEN,
    MISSED,
    NONE,
    APPOINTMENT
}

data class CalendarDay(
    val dayOfMonth: Int,
    val date: LocalDate,
    val status: AdherenceStatus,
    val isToday: Boolean,
    val isCurrentMonth: Boolean
)

data class Appointment(val id: String, val title: String, val type: String, val date: String, val time: String)

data class Doctor(val name: String)

val sampleAppointments =
    com.group8.comp2300.mock.sampleCalendarAppointments.map {
        Appointment(it.id, it.title, it.type, it.date, it.time)
    }

val sampleDoctors = com.group8.comp2300.mock.sampleCalendarDoctors.map { Doctor(it.name) }

fun generateCalendarDays(year: Int, month: Month): List<CalendarDay> {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val firstOfMonth = LocalDate(year, month, 1)
    val startOffset = firstOfMonth.dayOfWeek.isoDayNumber % 7
    val firstDayOfGrid = firstOfMonth.minus(startOffset, DateTimeUnit.DAY)

    return (0 until 42).map { offset ->
        val date = firstDayOfGrid.plus(offset, DateTimeUnit.DAY)
        val isToday = date == today
        val isCurrentMonth = date.month == month
        val status =
            when {
                date == today -> AdherenceStatus.NONE
                !isCurrentMonth -> AdherenceStatus.NONE
                date.day % 5 == 0 -> AdherenceStatus.TAKEN
                date.day % 7 == 0 -> AdherenceStatus.MISSED
                else -> AdherenceStatus.NONE
            }

        CalendarDay(
            dayOfMonth = date.day,
            date = date,
            status = status,
            isToday = isToday,
            isCurrentMonth = isCurrentMonth
        )
    }
}

enum class SheetView {
    MENU,
    FORM_MED,
    FORM_APPT,
    FORM_MOOD,
    DETAILS_APPT
}

object FormConstants {
    @Composable
    fun commonMeds() = listOf(
        stringResource(Res.string.medication_prep),
        stringResource(Res.string.medication_truvada),
        stringResource(Res.string.medication_descovy),
        stringResource(Res.string.medication_doxypep),
        stringResource(Res.string.medication_multivitamin)
    )

    @Composable
    fun apptTypes() = listOf(
        stringResource(Res.string.appt_type_consultation),
        stringResource(Res.string.appt_type_labwork),
        stringResource(Res.string.appt_type_followup)
    )

    val Dosages = listOf(1, 2, 3)
    val MoodEmojis = listOf("😢", "😕", "😐", "🙂", "🤩")

    @Composable
    fun moodLabels() = listOf(
        stringResource(Res.string.form_mood_very_sad),
        stringResource(Res.string.form_mood_sad),
        stringResource(Res.string.form_mood_neutral),
        stringResource(Res.string.form_mood_happy),
        stringResource(Res.string.form_mood_great)
    )

    @Composable
    fun moodTags() = listOf(
        stringResource(Res.string.form_mood_tag_anxious),
        stringResource(Res.string.form_mood_tag_calm),
        stringResource(Res.string.form_mood_tag_irritable),
        stringResource(Res.string.form_mood_tag_energetic),
        stringResource(Res.string.form_mood_tag_tired),
        stringResource(Res.string.form_mood_tag_stressed),
        stringResource(Res.string.form_mood_tag_focused)
    )
}
