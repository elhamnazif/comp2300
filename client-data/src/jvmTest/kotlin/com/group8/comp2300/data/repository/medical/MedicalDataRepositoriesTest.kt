package com.group8.comp2300.data.repository.medical

import com.group8.comp2300.data.auth.TokenManagerImpl
import com.group8.comp2300.data.local.AppointmentLocalDataSource
import com.group8.comp2300.data.local.MedicationLocalDataSource
import com.group8.comp2300.data.local.MedicationLogLocalDataSource
import com.group8.comp2300.data.local.OutboxDataSource
import com.group8.comp2300.data.local.OutboxState
import com.group8.comp2300.data.local.RoutineLocalDataSource
import com.group8.comp2300.data.local.RoutineOccurrenceOverrideLocalDataSource
import com.group8.comp2300.data.local.SessionDataSource
import com.group8.comp2300.data.notifications.RoutineNotificationScheduler
import com.group8.comp2300.data.offline.QueuedWriteDispatcher
import com.group8.comp2300.data.repository.newDatabase
import com.group8.comp2300.data.repository.sampleMedication
import com.group8.comp2300.domain.model.medical.AppointmentRequest
import com.group8.comp2300.domain.model.medical.MedicationCreateRequest
import com.group8.comp2300.domain.model.medical.MedicationLogRequest
import com.group8.comp2300.domain.model.medical.MedicationLogStatus
import com.group8.comp2300.domain.model.medical.Routine
import com.group8.comp2300.domain.model.medical.RoutineCreateRequest
import com.group8.comp2300.domain.model.medical.RoutineOccurrenceOverrideRequest
import com.group8.comp2300.domain.model.medical.RoutineRepeatType
import com.group8.comp2300.domain.model.medical.RoutineStatus
import com.group8.comp2300.domain.model.session.AuthSession
import com.group8.comp2300.domain.repository.AuthRepository
import com.group8.comp2300.domain.repository.medical.SyncCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MedicalDataRepositoriesTest {
    @Test
    fun guestMedicationSaveQueuesPendingWrite() = runTest {
        val db = newDatabase()
        val outbox = OutboxDataSource(db)
        val repository = MedicationDataRepositoryImpl(
            authRepository = FakeSessionAuthRepository(AuthSession.SignedOut),
            medicationLocal = MedicationLocalDataSource(db),
            queuedWriteDispatcher = dispatcher(db, outbox),
        )

        repository.saveMedication(
            MedicationCreateRequest(
                name = "Vitamin D",
                dosage = "1 tablet",
                quantity = "1000 IU",
                frequency = "DAILY",
            ),
        )

        assertEquals(1, MedicationLocalDataSource(db).getAll().size)
        assertEquals(1, outbox.getAll().size)
        assertEquals(OutboxState.PENDING, outbox.getAll().single().state)
    }

    @Test
    fun guestRoutineSaveQueuesPendingWriteAndSyncsNotifications() = runTest {
        val db = newDatabase()
        val outbox = OutboxDataSource(db)
        val notificationScheduler = RecordingRoutineNotificationScheduler()
        MedicationLocalDataSource(db).insert(sampleMedication())
        val repository = RoutineDataRepositoryImpl(
            authRepository = FakeSessionAuthRepository(AuthSession.SignedOut),
            routineLocal = RoutineLocalDataSource(db),
            queuedWriteDispatcher = dispatcher(db, outbox),
            routineNotificationScheduler = notificationScheduler,
        )

        repository.saveRoutine(
            RoutineCreateRequest(
                name = "Morning meds",
                timesOfDayMs = listOf(9 * 60 * 60 * 1000L),
                repeatType = RoutineRepeatType.DAILY.name,
                startDate = "2026-03-17",
                reminderOffsetsMins = listOf(0),
                medicationIds = listOf("med-1"),
            ),
        )

        assertEquals(1, RoutineLocalDataSource(db).getAll().size)
        assertEquals("ROUTINE_UPSERT", outbox.getAll().single().entityType)
        assertEquals(listOf("Morning meds"), notificationScheduler.syncedRoutineNames)
    }

    @Test
    fun guestAppointmentSaveQueuesPendingWrite() = runTest {
        val db = newDatabase()
        val outbox = OutboxDataSource(db)
        val repository = AppointmentDataRepositoryImpl(
            authRepository = FakeSessionAuthRepository(AuthSession.SignedOut),
            appointmentLocal = AppointmentLocalDataSource(db),
            queuedWriteDispatcher = dispatcher(db, outbox),
        )

        repository.scheduleAppointment(
            AppointmentRequest(
                title = "Appointment with Dr Tan",
                appointmentTime = 123456789L,
                appointmentType = "CONSULTATION",
                doctorName = "Dr Tan",
            ),
        )

        assertEquals(1, AppointmentLocalDataSource(db).getAll().size)
        assertEquals("APPOINTMENT", outbox.getAll().single().entityType)
        assertEquals(OutboxState.PENDING, outbox.getAll().single().state)
    }

    @Test
    fun medicationLogRepositoryBuildsRoutineLogsAndQueuesWrite() = runTest {
        val db = newDatabase()
        MedicationLocalDataSource(db).insert(sampleMedication())
        RoutineLocalDataSource(db).insert(
            routine(
                id = "routine-1",
                timesOfDayMs = listOf(9 * 60 * 60 * 1000L),
            ),
        )
        val outbox = OutboxDataSource(db)

        val repository = MedicationLogDataRepositoryImpl(
            medicationLocal = MedicationLocalDataSource(db),
            routineLocal = RoutineLocalDataSource(db),
            routineOccurrenceOverrideLocal = RoutineOccurrenceOverrideLocalDataSource(db),
            medicationLogLocal = MedicationLogLocalDataSource(db),
            queuedWriteDispatcher = dispatcher(db, outbox),
            routineNotificationScheduler = RecordingRoutineNotificationScheduler(),
        )

        repository.logMedication(
            MedicationLogRequest(
                medicationId = "med-1",
                status = MedicationLogStatus.TAKEN.name,
                timestampMs = 123L,
                routineId = "routine-1",
                occurrenceTimeMs = 1000L,
            ),
        )

        assertEquals(1, MedicationLogLocalDataSource(db).getAll().size)
        assertEquals("MEDICATION_LOG", outbox.getAll().single().entityType)
    }

    @Test
    fun occurrenceCandidatesIncludeNearbyScheduledDose() = runTest {
        val db = newDatabase()
        MedicationLocalDataSource(db).insert(sampleMedication())
        RoutineLocalDataSource(db).insert(
            routine(
                id = "routine-1",
                timesOfDayMs = listOf(9 * 60 * 60 * 1000L, 21 * 60 * 60 * 1000L),
            ),
        )
        val repository = MedicationLogDataRepositoryImpl(
            medicationLocal = MedicationLocalDataSource(db),
            routineLocal = RoutineLocalDataSource(db),
            routineOccurrenceOverrideLocal = RoutineOccurrenceOverrideLocalDataSource(db),
            medicationLogLocal = MedicationLogLocalDataSource(db),
            queuedWriteDispatcher = dispatcher(db, OutboxDataSource(db)),
            routineNotificationScheduler = RecordingRoutineNotificationScheduler(),
        )
        val agenda = repository.getRoutineAgenda("2026-03-17")
        val timestampMs = LocalDateTime(2026, Month.MARCH, 17, 10, 0, 0, 0)
            .toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()

        val candidates = repository.getMedicationOccurrenceCandidates("med-1", timestampMs)

        assertEquals(2, agenda.size)
        assertTrue(candidates.isNotEmpty())
        assertTrue(candidates.all { it.routineId == "routine-1" })
    }

    @Test
    fun reschedulingOccurrenceMovesAgendaQueuesWriteAndResyncsNotifications() = runTest {
        val db = newDatabase()
        MedicationLocalDataSource(db).insert(sampleMedication())
        RoutineLocalDataSource(db).insert(
            routine(
                id = "routine-1",
                timesOfDayMs = listOf(9 * 60 * 60 * 1000L),
            ),
        )
        val outbox = OutboxDataSource(db)
        val notificationScheduler = RecordingRoutineNotificationScheduler()
        val repository = MedicationLogDataRepositoryImpl(
            medicationLocal = MedicationLocalDataSource(db),
            routineLocal = RoutineLocalDataSource(db),
            routineOccurrenceOverrideLocal = RoutineOccurrenceOverrideLocalDataSource(db),
            medicationLogLocal = MedicationLogLocalDataSource(db),
            queuedWriteDispatcher = dispatcher(db, outbox),
            routineNotificationScheduler = notificationScheduler,
        )
        val originalTimestamp = LocalDateTime(2026, Month.MARCH, 17, 9, 0, 0, 0)
            .toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()
        val movedTimestamp = LocalDateTime(2026, Month.MARCH, 18, 11, 30, 0, 0)
            .toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()

        repository.rescheduleRoutineOccurrence(
            RoutineOccurrenceOverrideRequest(
                routineId = "routine-1",
                originalOccurrenceTimeMs = originalTimestamp,
                rescheduledOccurrenceTimeMs = movedTimestamp,
            ),
        )

        val originalDayAgenda = repository.getRoutineAgenda("2026-03-17")
        val movedDayAgenda = repository.getRoutineAgenda("2026-03-18")

        assertTrue(originalDayAgenda.isEmpty())
        assertEquals(2, movedDayAgenda.size)
        val rescheduledAgenda = movedDayAgenda.first { it.isRescheduled }
        assertEquals(movedTimestamp, rescheduledAgenda.occurrenceTimeMs)
        assertEquals(originalTimestamp, rescheduledAgenda.originalOccurrenceTimeMs)
        assertEquals("ROUTINE_OCCURRENCE_OVERRIDE_UPSERT", outbox.getAll().single().entityType)
        assertEquals(listOf("Morning meds"), notificationScheduler.syncedRoutineNames)
    }
}

class TestSyncCoordinator : SyncCoordinator {
    var flushCalls: Int = 0
    var refreshCalls: Int = 0

    override suspend fun flushOutbox() {
        flushCalls += 1
    }

    override suspend fun refreshAuthenticatedData() {
        refreshCalls += 1
    }
}

private class FakeSessionAuthRepository(initialSession: AuthSession) : AuthRepository {
    private val _session = MutableStateFlow(initialSession)
    override val session: StateFlow<AuthSession> = _session

    override suspend fun login(email: String, password: String) = error("unused")

    override suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        gender: com.group8.comp2300.domain.model.user.Gender,
        sexualOrientation: com.group8.comp2300.domain.model.user.SexualOrientation,
        dateOfBirth: kotlinx.datetime.LocalDate?,
    ) = error("unused")

    override suspend fun preregister(email: String, password: String) = error("unused")

    override suspend fun completeProfile(
        firstName: String,
        lastName: String,
        gender: com.group8.comp2300.domain.model.user.Gender,
        sexualOrientation: com.group8.comp2300.domain.model.user.SexualOrientation,
        dateOfBirth: kotlinx.datetime.LocalDate?,
    ) = error("unused")

    override suspend fun activateAccount(token: String) = error("unused")
    override suspend fun forgotPassword(email: String) = error("unused")
    override suspend fun resendVerificationEmail(email: String) = error("unused")
    override suspend fun resetPassword(token: String, newPassword: String) = error("unused")
    override suspend fun logout() = Unit
}

private class RecordingRoutineNotificationScheduler : RoutineNotificationScheduler {
    val syncedRoutineNames = mutableListOf<String>()

    override suspend fun syncRoutine(routine: Routine, previousRoutine: Routine?) {
        syncedRoutineNames += routine.name
    }

    override suspend fun removeRoutine(routine: Routine) = Unit

    override suspend fun syncAllRoutines() = Unit
}

private fun dispatcher(db: com.group8.comp2300.data.database.AppDatabase, outbox: OutboxDataSource) =
    QueuedWriteDispatcher(
        TokenManagerImpl(SessionDataSource(db)),
        outbox,
        TestSyncCoordinator(),
    )

private fun routine(id: String, timesOfDayMs: List<Long>) = Routine(
    id = id,
    userId = "",
    name = "Morning meds",
    timesOfDayMs = timesOfDayMs,
    repeatType = RoutineRepeatType.DAILY,
    startDate = "2026-03-17",
    hasReminder = true,
    reminderOffsetsMins = listOf(0),
    status = RoutineStatus.ACTIVE,
    medicationIds = listOf("med-1"),
)
