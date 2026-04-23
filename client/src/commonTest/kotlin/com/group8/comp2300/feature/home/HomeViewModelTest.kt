package com.group8.comp2300.feature.home

import com.group8.comp2300.data.notifications.RoutineNotificationService
import com.group8.comp2300.domain.model.medical.Appointment
import com.group8.comp2300.domain.model.medical.ClinicBookingRequest
import com.group8.comp2300.domain.model.medical.Medication
import com.group8.comp2300.domain.model.medical.MedicationCreateRequest
import com.group8.comp2300.domain.model.medical.MedicationLog
import com.group8.comp2300.domain.model.medical.MedicationLogRequest
import com.group8.comp2300.domain.model.medical.MedicationOccurrenceCandidate
import com.group8.comp2300.domain.model.medical.MedicationStatus
import com.group8.comp2300.domain.model.medical.MedicationUnit
import com.group8.comp2300.domain.model.medical.RoutineDayAgenda
import com.group8.comp2300.domain.model.medical.RoutineOccurrenceOverride
import com.group8.comp2300.domain.model.medical.RoutineOccurrenceOverrideRequest
import com.group8.comp2300.domain.repository.medical.AppointmentDataRepository
import com.group8.comp2300.domain.repository.medical.FailedSyncMutation
import com.group8.comp2300.domain.repository.medical.MedicationDataRepository
import com.group8.comp2300.domain.repository.medical.MedicationLogDataRepository
import com.group8.comp2300.domain.repository.medical.OfflineSyncCoordinator
import com.group8.comp2300.domain.repository.medical.SyncStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
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
    fun `load success keeps empty fallback state stable`() = runTest(dispatcher) {
        val viewModel = HomeViewModel(
            syncCoordinator = FakeOfflineSyncCoordinator(),
            appointmentRepository = FakeAppointmentRepository(),
            medicationRepository = FakeMedicationRepository(),
            medicationLogRepository = FakeMedicationLogRepository(),
            notificationService = FakeNotificationService(),
        )

        advanceUntilIdle()

        assertEquals(false, viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.errorMessage)
        assertEquals(0, viewModel.state.value.activeMedicationCount)
        assertTrue(viewModel.state.value.inboxItems.isEmpty())
        assertEquals(0, viewModel.state.value.todaySummary.totalMedicationCount)
    }

    @Test
    fun `load failure exposes underlying error message`() = runTest(dispatcher) {
        val viewModel = HomeViewModel(
            syncCoordinator = FailingOfflineSyncCoordinator("Cannot reach server"),
            appointmentRepository = FakeAppointmentRepository(),
            medicationRepository = FakeMedicationRepository(),
            medicationLogRepository = FakeMedicationLogRepository(),
            notificationService = FakeNotificationService(),
        )

        advanceUntilIdle()

        assertEquals(false, viewModel.state.value.isLoading)
        assertEquals("Cannot reach server", viewModel.state.value.errorMessage)
    }

    @Test
    fun `loading stays true while refresh is in flight`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val viewModel = HomeViewModel(
            syncCoordinator = WaitingOfflineSyncCoordinator(gate),
            appointmentRepository = FakeAppointmentRepository(),
            medicationRepository = FakeMedicationRepository(
                medications = listOf(
                    Medication(
                        id = "med-1",
                        userId = "user-1",
                        name = "PrEP",
                        doseAmount = "1",
                        doseUnit = MedicationUnit.TABLET,
                        stockAmount = "30",
                        stockUnit = MedicationUnit.TABLET,
                        status = MedicationStatus.ACTIVE,
                    ),
                ),
            ),
            medicationLogRepository = FakeMedicationLogRepository(),
            notificationService = FakeNotificationService(),
        )

        runCurrent()
        assertEquals(true, viewModel.state.value.isLoading)

        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(false, viewModel.state.value.isLoading)
        assertEquals(1, viewModel.state.value.activeMedicationCount)
    }
}

private class FakeOfflineSyncCoordinator : OfflineSyncCoordinator {
    override suspend fun syncNow(): SyncStatus = SyncStatus(false, 0, 0, false)

    override suspend fun refreshCaches(): SyncStatus = SyncStatus(false, 0, 0, true)

    override suspend fun listFailedMutations(): List<FailedSyncMutation> = emptyList()

    override suspend fun retryFailedMutation(id: String): SyncStatus = SyncStatus(false, 0, 0, true)

    override suspend fun discardFailedMutation(id: String) = Unit
}

private class FailingOfflineSyncCoordinator(private val message: String) : OfflineSyncCoordinator {
    override suspend fun syncNow(): SyncStatus = SyncStatus(false, 0, 0, false)

    override suspend fun refreshCaches(): SyncStatus = throw IllegalStateException(message)

    override suspend fun listFailedMutations(): List<FailedSyncMutation> = emptyList()

    override suspend fun retryFailedMutation(id: String): SyncStatus = SyncStatus(false, 0, 0, true)

    override suspend fun discardFailedMutation(id: String) = Unit
}

private class WaitingOfflineSyncCoordinator(private val gate: CompletableDeferred<Unit>) : OfflineSyncCoordinator {
    override suspend fun syncNow(): SyncStatus = SyncStatus(false, 0, 0, false)

    override suspend fun refreshCaches(): SyncStatus {
        gate.await()
        return SyncStatus(false, 0, 0, true)
    }

    override suspend fun listFailedMutations(): List<FailedSyncMutation> = emptyList()

    override suspend fun retryFailedMutation(id: String): SyncStatus = SyncStatus(false, 0, 0, true)

    override suspend fun discardFailedMutation(id: String) = Unit
}

private class FakeAppointmentRepository(private val appointments: List<Appointment> = emptyList()) :
    AppointmentDataRepository {
    override suspend fun getAppointments(): List<Appointment> = appointments

    override suspend fun getBookingHistory(): List<Appointment> = appointments

    override suspend fun getAppointment(id: String): Appointment? = appointments.firstOrNull { it.id == id }

    override suspend fun bookClinicAppointment(request: ClinicBookingRequest): Appointment = error("unused")

    override suspend fun cancelAppointment(id: String): Appointment = error("unused")

    override suspend fun rescheduleAppointment(id: String, request: ClinicBookingRequest): Appointment = error("unused")
}

private class FakeMedicationRepository(private val medications: List<Medication> = emptyList()) :
    MedicationDataRepository {
    override suspend fun getMedications(): List<Medication> = medications

    override suspend fun saveMedication(request: MedicationCreateRequest, id: String?): Medication = error("unused")

    override suspend fun deleteMedication(id: String) = Unit
}

private class FakeMedicationLogRepository(private val agenda: List<RoutineDayAgenda> = emptyList()) :
    MedicationLogDataRepository {
    override suspend fun getRoutineAgenda(date: String): List<RoutineDayAgenda> = agenda

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

private class FakeNotificationService(private val enabled: Boolean = true) : RoutineNotificationService {
    override suspend fun schedule(notification: com.group8.comp2300.data.notifications.ScheduledRoutineNotification) =
        Unit

    override suspend fun cancel(notificationId: String) = Unit

    override suspend fun notificationsEnabled(): Boolean = enabled
}
