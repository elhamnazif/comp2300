package com.group8.comp2300.feature.home

import com.group8.comp2300.domain.model.medical.Appointment
import com.group8.comp2300.domain.model.medical.MedicationLogStatus
import com.group8.comp2300.domain.model.medical.RoutineDayAgenda
import com.group8.comp2300.domain.model.medical.RoutineMedicationAgenda
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

internal data class HomeUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val greetingPeriod: GreetingPeriod = GreetingPeriod.MORNING,
    val activeMedicationCount: Int = 0,
    val todaySummary: TodaySummary = TodaySummary(),
    val inboxItems: List<HomeInboxItem> = emptyList(),
)

internal enum class GreetingPeriod {
    MORNING,
    AFTERNOON,
    EVENING,
}

internal data class TodaySummary(
    val nextAppointment: HomeAppointmentSummary? = null,
    val medicationsDueCount: Int = 0,
    val takenMedicationCount: Int = 0,
    val totalMedicationCount: Int = 0,
    val adherenceEligibleMedicationCount: Int = 0,
    val firstMedicationTimeMs: Long? = null,
) {
    val adherenceProgress: Float
        get() = if (adherenceEligibleMedicationCount == 0) {
            0f
        } else {
            takenMedicationCount.toFloat() / adherenceEligibleMedicationCount
        }

    val adherenceState: TodayAdherenceState
        get() = when {
            totalMedicationCount == 0 -> TodayAdherenceState.NO_DOSES
            adherenceEligibleMedicationCount == 0 -> TodayAdherenceState.NOT_STARTED
            else -> TodayAdherenceState.IN_PROGRESS
        }
}

internal enum class TodayAdherenceState {
    NO_DOSES,
    NOT_STARTED,
    IN_PROGRESS,
}

internal data class HomeAppointmentSummary(val appointmentId: String, val title: String, val appointmentTime: Long)

internal enum class HomeInboxGroup {
    ATTENTION,
    TODAY,
}

internal sealed interface HomeInboxAction {
    data object OpenCalendar : HomeInboxAction

    data class OpenBookingHistory(val appointmentId: String) : HomeInboxAction

    data object OpenNotificationSettings : HomeInboxAction
}

internal sealed interface HomeInboxItem {
    val id: String
    val group: HomeInboxGroup
    val timestampMs: Long
    val action: HomeInboxAction

    data class NotificationAlert(override val timestampMs: Long) : HomeInboxItem {
        override val id: String = "alert:notifications_disabled"
        override val group: HomeInboxGroup = HomeInboxGroup.ATTENTION
        override val action: HomeInboxAction = HomeInboxAction.OpenNotificationSettings
    }

    data class MedicationAttention(
        val routineId: String,
        val routineName: String,
        val dueCount: Int,
        override val timestampMs: Long,
    ) : HomeInboxItem {
        override val id: String = "medication:$routineId:$timestampMs"
        override val group: HomeInboxGroup = HomeInboxGroup.ATTENTION
        override val action: HomeInboxAction = HomeInboxAction.OpenCalendar
    }

    data class AppointmentUpdate(val appointmentId: String, val title: String, override val timestampMs: Long) :
        HomeInboxItem {
        override val id: String = "appointment:$appointmentId"
        override val group: HomeInboxGroup = HomeInboxGroup.TODAY
        override val action: HomeInboxAction = HomeInboxAction.OpenBookingHistory(appointmentId)
    }
}

internal fun buildGreetingPeriod(nowMs: Long, timeZone: TimeZone): GreetingPeriod {
    val hour = kotlin.time.Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(timeZone).hour
    return when {
        hour < 12 -> GreetingPeriod.MORNING
        hour < 17 -> GreetingPeriod.AFTERNOON
        else -> GreetingPeriod.EVENING
    }
}

internal fun buildTodaySummary(
    appointments: List<Appointment>,
    agenda: List<RoutineDayAgenda>,
    nowMs: Long,
): TodaySummary {
    val nextAppointment = appointments
        .filter { it.appointmentTime >= nowMs }
        .minByOrNull(Appointment::appointmentTime)
        ?.let {
            HomeAppointmentSummary(
                appointmentId = it.id,
                title = it.title,
                appointmentTime = it.appointmentTime,
            )
        }

    val medications = agenda.flatMap(RoutineDayAgenda::medications)
    val adherenceEligibleMedicationCount = agenda.sumOf { routine ->
        routine.medications.count { medication ->
            medication.loggedTimeMs != null || routine.occurrenceTimeMs <= nowMs
        }
    }
    return TodaySummary(
        nextAppointment = nextAppointment,
        medicationsDueCount = agenda.sumOf { routine ->
            routine.medications.count { medication ->
                needsMedicationAttention(
                    medication = medication,
                    occurrenceTimeMs = routine.occurrenceTimeMs,
                    nowMs = nowMs,
                )
            }
        },
        takenMedicationCount = medications.count { it.status == MedicationLogStatus.TAKEN },
        totalMedicationCount = medications.size,
        adherenceEligibleMedicationCount = adherenceEligibleMedicationCount,
        firstMedicationTimeMs = agenda.minOfOrNull(RoutineDayAgenda::occurrenceTimeMs),
    )
}

internal fun buildHomeInboxItems(
    appointments: List<Appointment>,
    agenda: List<RoutineDayAgenda>,
    notificationsEnabled: Boolean,
    nowMs: Long,
    timeZone: TimeZone,
): List<HomeInboxItem> {
    val today = epochMillisToLocalDate(nowMs, timeZone)
    val items = buildList {
        if (!notificationsEnabled && agenda.any { it.hasReminder && it.reminderOffsetsMins.isNotEmpty() }) {
            add(HomeInboxItem.NotificationAlert(timestampMs = nowMs))
        }

        agenda.forEach { routine ->
            val dueCount = routine.medications.count { medication ->
                needsMedicationAttention(
                    medication = medication,
                    occurrenceTimeMs = routine.occurrenceTimeMs,
                    nowMs = nowMs,
                )
            }
            if (dueCount > 0) {
                add(
                    HomeInboxItem.MedicationAttention(
                        routineId = routine.routineId,
                        routineName = routine.routineName,
                        dueCount = dueCount,
                        timestampMs = routine.occurrenceTimeMs,
                    ),
                )
            }
        }

        appointments
            .filter { appointment ->
                appointment.appointmentTime >= nowMs &&
                    epochMillisToLocalDate(appointment.appointmentTime, timeZone) == today
            }
            .forEach { appointment ->
                add(
                    HomeInboxItem.AppointmentUpdate(
                        appointmentId = appointment.id,
                        title = appointment.title,
                        timestampMs = appointment.appointmentTime,
                    ),
                )
            }
    }

    return items.sortedWith(
        compareBy(
            ::homeInboxGroupRank,
            ::homeInboxKindRank,
            HomeInboxItem::timestampMs,
        ),
    )
}

private fun needsMedicationAttention(
    medication: RoutineMedicationAgenda,
    occurrenceTimeMs: Long,
    nowMs: Long,
): Boolean = when (medication.status) {
    MedicationLogStatus.PENDING -> occurrenceTimeMs <= nowMs

    MedicationLogStatus.MISSED,
    MedicationLogStatus.SNOOZED,
    -> true

    MedicationLogStatus.TAKEN,
    MedicationLogStatus.SKIPPED,
    -> false
}

private fun homeInboxGroupRank(item: HomeInboxItem): Int = when (item.group) {
    HomeInboxGroup.ATTENTION -> 0
    HomeInboxGroup.TODAY -> 1
}

private fun homeInboxKindRank(item: HomeInboxItem): Int = when (item) {
    is HomeInboxItem.NotificationAlert -> 0
    is HomeInboxItem.MedicationAttention -> 1
    is HomeInboxItem.AppointmentUpdate -> 2
}

private fun epochMillisToLocalDate(timestampMs: Long, timeZone: TimeZone): kotlinx.datetime.LocalDate =
    kotlin.time.Instant.fromEpochMilliseconds(timestampMs).toLocalDateTime(timeZone).date
