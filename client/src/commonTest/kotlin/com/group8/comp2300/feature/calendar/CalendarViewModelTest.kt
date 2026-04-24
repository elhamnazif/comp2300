package com.group8.comp2300.feature.calendar

import com.group8.comp2300.domain.model.medical.*
import com.group8.comp2300.domain.repository.medical.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        kotlinx.coroutines.Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    @Test
    fun `load agenda refreshes appointments for the selected date`() = runTest(dispatcher) {
        val appointmentRepository = MutableAppointmentRepository(emptyList())
        val viewModel = CalendarViewModel(
            syncCoordinator = FakeOfflineSyncCoordinator(),
            calendarRepository = FakeCalendarRepository(),
            appointmentRepository = appointmentRepository,
            medicationRepository = FakeMedicationRepository(),
            medicationLogRepository = FakeMedicationLogRepository(),
            moodRepository = FakeMoodRepository(),
        )

        advanceUntilIdle()
        assertEquals(emptyList(), viewModel.state.value.appointments)

        val appointment = sampleAppointment(
            id = "appointment-1",
            appointmentTime = utcMs(2026, 4, 24, 9, 0),
        )
        appointmentRepository.appointments = listOf(appointment)

        viewModel.loadAgendaForDate("2026-04-24")
        advanceUntilIdle()

        assertEquals(listOf(appointment), viewModel.state.value.appointments)
        assertEquals("2026-04-24", viewModel.state.value.selectedDate)
    }

    @Test
    fun `load agenda refreshes month mood summary for the selected date month`() = runTest(
        dispatcher,
    ) {
        val moodRepository = FakeMoodRepository().apply {
            moodsByDate["2026-05-02"] = listOf(
                sampleMood("day-1", utcMs(2026, 5, 2, 8, 0), MoodType.GOOD),
            )
            moodsByMonth[2026 to 5] = listOf(
                sampleMood("month-1", utcMs(2026, 5, 1, 9, 0), MoodType.GOOD),
                sampleMood("month-2", utcMs(2026, 5, 3, 9, 0), MoodType.GREAT),
                sampleMood("month-3", utcMs(2026, 5, 4, 9, 0), MoodType.GREAT),
            )
        }
        val viewModel = CalendarViewModel(
            syncCoordinator = FakeOfflineSyncCoordinator(),
            calendarRepository = FakeCalendarRepository(),
            appointmentRepository = MutableAppointmentRepository(emptyList()),
            medicationRepository = FakeMedicationRepository(),
            medicationLogRepository = FakeMedicationLogRepository(),
            moodRepository = moodRepository,
        )

        advanceUntilIdle()
        viewModel.loadAgendaForDate("2026-05-02")
        advanceUntilIdle()

        assertEquals("2026-05-02", viewModel.state.value.selectedDate)
        assertEquals(1, viewModel.state.value.dayMoodEntries.size)
        assertEquals(
            mapOf(MoodType.GOOD to 1, MoodType.GREAT to 2),
            viewModel.state.value.monthMoodSummary,
        )
    }
}

private class FakeOfflineSyncCoordinator : OfflineSyncCoordinator {
    override suspend fun syncNow(): SyncStatus = SyncStatus(false, 0, 0, false)

    override suspend fun refreshCaches(): SyncStatus = SyncStatus(false, 0, 0, true)

    override suspend fun listFailedMutations(): List<FailedSyncMutation> = emptyList()

    override suspend fun retryFailedMutation(id: String): SyncStatus = SyncStatus(false, 0, 0, true)

    override suspend fun discardFailedMutation(id: String) = Unit
}

private class FakeCalendarRepository : CalendarDataRepository {
    override suspend fun getCalendarOverview(year: Int, month: Int): List<CalendarOverviewResponse> = emptyList()
}

private class MutableAppointmentRepository(var appointments: List<Appointment>) : AppointmentDataRepository {
    override suspend fun getAppointments(): List<Appointment> = appointments

    override suspend fun getBookingHistory(): List<Appointment> = appointments

    override suspend fun getAppointment(id: String): Appointment? = appointments.firstOrNull { it.id == id }

    override suspend fun bookClinicAppointment(request: ClinicBookingRequest): Appointment = error("unused")

    override suspend fun cancelAppointment(id: String): Appointment = error("unused")

    override suspend fun rescheduleAppointment(id: String, request: ClinicBookingRequest): Appointment = error("unused")
}

private class FakeMedicationRepository : MedicationDataRepository {
    override suspend fun getMedications(): List<Medication> = emptyList()

    override suspend fun saveMedication(request: MedicationCreateRequest, id: String?): Medication = error("unused")

    override suspend fun deleteMedication(id: String) = Unit
}

private class FakeMedicationLogRepository : MedicationLogDataRepository {
    override suspend fun getRoutineAgenda(date: String): List<RoutineDayAgenda> = emptyList()

    override suspend fun getRoutineAgendaRange(
        startDate: String,
        endDate: String,
    ): Map<String, List<RoutineDayAgenda>> = emptyMap()

    override suspend fun getManualMedicationLogs(date: String): List<MedicationLog> = emptyList()

    override suspend fun getManualMedicationLogsRange(
        startDate: String,
        endDate: String,
    ): Map<String, List<MedicationLog>> = emptyMap()

    override suspend fun getMedicationOccurrenceCandidates(
        medicationId: String,
        timestampMs: Long,
    ): List<MedicationOccurrenceCandidate> = emptyList()

    override suspend fun rescheduleRoutineOccurrence(
        request: RoutineOccurrenceOverrideRequest,
    ): RoutineOccurrenceOverride = error("unused")

    override suspend fun logMedication(request: MedicationLogRequest): MedicationLog = error("unused")
}

private class FakeMoodRepository : MoodDataRepository {
    val moodsByDate = mutableMapOf<String, List<Mood>>()
    val moodsByMonth = mutableMapOf<Pair<Int, Int>, List<Mood>>()

    override suspend fun logMood(request: MoodEntryRequest): Mood = error("unused")

    override suspend fun getMoodsForDate(dateString: String): List<Mood> = moodsByDate[dateString].orEmpty()

    override suspend fun getMoodsForMonth(year: Int, month: Int): List<Mood> = moodsByMonth[year to month].orEmpty()
}

private fun sampleAppointment(id: String, appointmentTime: Long) = Appointment(
    id = id,
    userId = "user-1",
    title = "Clinic appointment",
    appointmentTime = appointmentTime,
    appointmentType = "CONSULTATION",
    clinicId = "clinic-1",
    bookingId = "slot-1",
    status = "CONFIRMED",
    notes = null,
    hasReminder = true,
)

private fun utcMs(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long = kotlinx.datetime.LocalDateTime(
    year = year,
    monthNumber = month,
    dayOfMonth = day,
    hour = hour,
    minute = minute,
).toInstant(kotlinx.datetime.TimeZone.UTC).toEpochMilliseconds()

private fun sampleMood(id: String, timestamp: Long, moodType: MoodType) = Mood(
    id = id,
    userId = "user-1",
    timestamp = timestamp,
    moodType = moodType,
    feeling = moodType.displayName,
    journal = null,
)
