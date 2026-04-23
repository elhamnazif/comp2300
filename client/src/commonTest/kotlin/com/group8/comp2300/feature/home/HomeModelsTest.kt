package com.group8.comp2300.feature.home

import com.group8.comp2300.domain.model.medical.Appointment
import com.group8.comp2300.domain.model.medical.MedicationLogStatus
import com.group8.comp2300.domain.model.medical.RoutineDayAgenda
import com.group8.comp2300.domain.model.medical.RoutineMedicationAgenda
import kotlinx.datetime.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class HomeModelsTest {
    private val timeZone = TimeZone.UTC
    private val nowMs = timestamp("2026-04-24T09:00")

    @Test
    fun `today summary picks next appointment and computes adherence`() {
        val summary = buildTodaySummary(
            appointments = listOf(
                appointment(id = "past", title = "Past", time = "2026-04-24T08:00"),
                appointment(id = "next", title = "Check-up", time = "2026-04-24T10:30"),
                appointment(id = "later", title = "Later", time = "2026-04-24T15:30"),
            ),
            agenda = listOf(
                routineAgenda(
                    "Morning Prep",
                    "2026-04-24T07:00",
                    MedicationLogStatus.TAKEN,
                    MedicationLogStatus.PENDING,
                ),
            ),
            nowMs = nowMs,
        )

        assertEquals("next", summary.nextAppointment?.appointmentId)
        assertEquals(1, summary.medicationsDueCount)
        assertEquals(1, summary.takenMedicationCount)
        assertEquals(2, summary.totalMedicationCount)
        assertEquals(0.5f, summary.adherenceProgress)
    }

    @Test
    fun `inbox includes app alert when notifications are disabled`() {
        val items = buildHomeInboxItems(
            appointments = emptyList(),
            agenda = listOf(
                routineAgenda(
                    "Evening dose",
                    "2026-04-24T20:00",
                    MedicationLogStatus.PENDING,
                    hasReminder = true,
                ),
            ),
            notificationsEnabled = false,
            nowMs = nowMs,
            timeZone = timeZone,
        )

        assertIs<HomeInboxItem.NotificationAlert>(items.first())
    }

    @Test
    fun `inbox routes appointments to highlighted booking history and orders medication before today updates`() {
        val items = buildHomeInboxItems(
            appointments = listOf(
                appointment(id = "booking-1", title = "Clinic follow-up", time = "2026-04-24T14:00"),
            ),
            agenda = listOf(
                routineAgenda(
                    "Morning dose",
                    "2026-04-24T09:30",
                    MedicationLogStatus.MISSED,
                ),
            ),
            notificationsEnabled = true,
            nowMs = nowMs,
            timeZone = timeZone,
        )

        assertIs<HomeInboxItem.MedicationAttention>(items.first())
        val appointmentItem = items.last()
        assertIs<HomeInboxItem.AppointmentUpdate>(appointmentItem)
        assertEquals(
            HomeInboxAction.OpenBookingHistory("booking-1"),
            appointmentItem.action,
        )
        assertTrue(items.none { it is HomeInboxItem.NotificationAlert })
    }

    @Test
    fun `future pending doses are not counted as attention yet`() {
        val summary = buildTodaySummary(
            appointments = emptyList(),
            agenda = listOf(
                routineAgenda(
                    "Evening dose",
                    "2026-04-24T20:00",
                    MedicationLogStatus.PENDING,
                ),
            ),
            nowMs = nowMs,
        )
        val items = buildHomeInboxItems(
            appointments = emptyList(),
            agenda = listOf(
                routineAgenda(
                    "Evening dose",
                    "2026-04-24T20:00",
                    MedicationLogStatus.PENDING,
                ),
            ),
            notificationsEnabled = true,
            nowMs = nowMs,
            timeZone = timeZone,
        )

        assertEquals(0, summary.medicationsDueCount)
        assertTrue(items.none { it is HomeInboxItem.MedicationAttention })
    }

    private fun appointment(id: String, title: String, time: String): Appointment = Appointment(
        id = id,
        userId = "user-1",
        title = title,
        appointmentTime = timestamp(time),
        appointmentType = "CHECKUP",
        clinicId = "clinic-1",
        bookingId = "booking-$id",
        status = "CONFIRMED",
        notes = null,
        hasReminder = true,
    )

    private fun routineAgenda(
        name: String,
        time: String,
        vararg statuses: MedicationLogStatus,
        hasReminder: Boolean = false,
    ): RoutineDayAgenda = RoutineDayAgenda(
        routineId = name.lowercase().replace(" ", "-"),
        routineName = name,
        occurrenceTimeMs = timestamp(time),
        hasReminder = hasReminder,
        reminderOffsetsMins = if (hasReminder) listOf(30) else emptyList(),
        medications = statuses.mapIndexed { index, status ->
            RoutineMedicationAgenda(
                medicationId = "med-$index",
                medicationName = "Medication ${index + 1}",
                dosage = "1 tab",
                status = status,
            )
        },
    )

    private fun timestamp(value: String): Long {
        val date = LocalDate.parse(value.substringBefore('T'))
        val time = LocalTime.parse(value.substringAfter('T'))
        return LocalDateTime(date, time).toInstant(timeZone).toEpochMilliseconds()
    }
}
