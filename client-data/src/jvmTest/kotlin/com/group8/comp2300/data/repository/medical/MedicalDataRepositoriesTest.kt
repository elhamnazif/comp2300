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
import com.group8.comp2300.data.offline.MedicalOfflineMutations
import com.group8.comp2300.data.offline.MutationHandlerRegistry
import com.group8.comp2300.data.offline.OfflineDataRefresher
import com.group8.comp2300.data.offline.OfflineMutationHandler
import com.group8.comp2300.data.offline.QueuedWriteDispatcher
import com.group8.comp2300.data.offline.SyncCoordinatorImpl
import com.group8.comp2300.data.remote.ApiService
import com.group8.comp2300.data.repository.newDatabase
import com.group8.comp2300.data.repository.sampleMedication
import com.group8.comp2300.domain.model.medical.Appointment
import com.group8.comp2300.domain.model.medical.AppointmentSlot
import com.group8.comp2300.domain.model.medical.Clinic
import com.group8.comp2300.domain.model.medical.ClinicBookingRequest
import com.group8.comp2300.domain.model.medical.MedicationCreateRequest
import com.group8.comp2300.domain.model.medical.MedicationLog
import com.group8.comp2300.domain.model.medical.MedicationLogRequest
import com.group8.comp2300.domain.model.medical.MedicationLogStatus
import com.group8.comp2300.domain.model.medical.Routine
import com.group8.comp2300.domain.model.medical.RoutineCreateRequest
import com.group8.comp2300.domain.model.medical.RoutineOccurrenceOverrideRequest
import com.group8.comp2300.domain.model.medical.RoutineRepeatType
import com.group8.comp2300.domain.model.medical.RoutineStatus
import com.group8.comp2300.domain.model.session.AuthSession
import com.group8.comp2300.domain.model.user.User
import com.group8.comp2300.domain.repository.AuthRepository
import com.group8.comp2300.domain.repository.medical.FailedSyncMutation
import com.group8.comp2300.domain.repository.medical.SyncCoordinator
import com.group8.comp2300.domain.repository.medical.SyncStatus
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
                doseAmount = "1",
                doseUnit = "TABLET",
                stockAmount = "30",
                stockUnit = "TABLET",
            ),
        )

        assertEquals(1, MedicationLocalDataSource(db).getAll().size)
        assertEquals(1, outbox.getAll().size)
        assertEquals(OutboxState.PENDING, outbox.getAll().single().state)
    }

    @Test
    fun medicationSaveKeepsZeroStockAmount() = runTest {
        val db = newDatabase()
        val repository = MedicationDataRepositoryImpl(
            authRepository = FakeSessionAuthRepository(AuthSession.SignedOut),
            medicationLocal = MedicationLocalDataSource(db),
            queuedWriteDispatcher = dispatcher(db, OutboxDataSource(db)),
        )

        val savedMedication = repository.saveMedication(
            MedicationCreateRequest(
                name = "Vitamin D",
                doseAmount = "1",
                doseUnit = "TABLET",
                stockAmount = "0",
                stockUnit = "TABLET",
            ),
        )

        assertEquals("0", savedMedication.stockAmount)
        assertEquals("0", MedicationLocalDataSource(db).getAll().single().stockAmount)
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
    fun signedInMedicationSaveKeepsOptimisticRowWhenSyncWriteFailsTransiently() = runTest {
        val db = newDatabase()
        val outbox = OutboxDataSource(db)
        val sessionDataSource = SessionDataSource(db)
        TokenManagerImpl(sessionDataSource).saveTokens(
            userId = "user-1",
            accessToken = "access",
            refreshToken = "refresh",
            expiresAt = Long.MAX_VALUE,
        )
        val dataRefresher = object : OfflineDataRefresher {
            var refreshCalls = 0

            override suspend fun refreshAuthenticatedData() {
                refreshCalls += 1
                MedicationLocalDataSource(db).replaceAll(emptyList())
            }
        }
        val mutationHandlers = MutationHandlerRegistry(
            listOf(
                object : OfflineMutationHandler {
                    override val type: String = MedicalOfflineMutations.medicationUpsert.type

                    override suspend fun apply(item: com.group8.comp2300.data.local.OutboxItem): Unit =
                        throw Exception("offline")
                },
            ),
        )
        val queuedWriteDispatcher = QueuedWriteDispatcher(
            tokenManager = TokenManagerImpl(sessionDataSource),
            outbox = outbox,
            syncCoordinator = SyncCoordinatorImpl(
                tokenManager = TokenManagerImpl(sessionDataSource),
                outbox = outbox,
                mutationHandlers = mutationHandlers,
                dataRefresher = dataRefresher,
            ),
        )
        val repository = MedicationDataRepositoryImpl(
            authRepository = FakeSessionAuthRepository(
                AuthSession.SignedIn(
                    User(
                        id = "user-1",
                        firstName = "Test",
                        lastName = "User",
                        email = "user@example.com",
                    ),
                ),
            ),
            medicationLocal = MedicationLocalDataSource(db),
            queuedWriteDispatcher = queuedWriteDispatcher,
        )

        val savedMedication = repository.saveMedication(
            MedicationCreateRequest(
                name = "Vitamin D",
                doseAmount = "1",
                doseUnit = "TABLET",
                stockAmount = "1000",
                stockUnit = "OTHER",
                customStockUnit = "IU",
            ),
        )

        assertEquals(savedMedication.id, MedicationLocalDataSource(db).getAll().single().id)
        assertEquals(1, outbox.getPending().size)
        assertEquals(0, dataRefresher.refreshCalls)
    }

    @Test
    fun bookingAppointmentPersistsServerResultLocally() = runTest {
        val db = newDatabase()
        val repository = AppointmentDataRepositoryImpl(
            appointmentLocal = AppointmentLocalDataSource(db),
            apiService = BookingApiStub(),
        )

        repository.bookClinicAppointment(
            ClinicBookingRequest(
                clinicId = "clinic-1",
                slotId = "slot-1",
                appointmentType = "CONSULTATION",
                reason = "Fever",
                hasReminder = true,
            ),
        )

        assertEquals(1, AppointmentLocalDataSource(db).getAll().size)
        assertEquals("clinic-1", AppointmentLocalDataSource(db).getAll().single().clinicId)
        assertEquals("slot-1", AppointmentLocalDataSource(db).getAll().single().bookingId)
    }

    @Test
    fun cancellingAppointmentUpdatesLocalRecord() = runTest {
        val db = newDatabase()
        val apiService = BookingApiStub()
        val repository = AppointmentDataRepositoryImpl(
            appointmentLocal = AppointmentLocalDataSource(db),
            apiService = apiService,
        )

        repository.bookClinicAppointment(
            ClinicBookingRequest(
                clinicId = "clinic-1",
                slotId = "slot-1",
                appointmentType = "STI_TESTING",
                reason = "Screening",
                hasReminder = true,
            ),
        )

        repository.cancelAppointment("appointment-1")

        val stored = AppointmentLocalDataSource(db).getById("appointment-1")
        assertEquals("CANCELLED", stored?.status)
        assertEquals(null, stored?.bookingId)
    }

    @Test
    fun reschedulingAppointmentUpdatesLocalRecord() = runTest {
        val db = newDatabase()
        val apiService = BookingApiStub()
        val repository = AppointmentDataRepositoryImpl(
            appointmentLocal = AppointmentLocalDataSource(db),
            apiService = apiService,
        )

        repository.bookClinicAppointment(
            ClinicBookingRequest(
                clinicId = "clinic-1",
                slotId = "slot-1",
                appointmentType = "STI_TESTING",
                reason = "Screening",
                hasReminder = true,
            ),
        )

        repository.rescheduleAppointment(
            id = "appointment-1",
            request = ClinicBookingRequest(
                clinicId = "clinic-1",
                slotId = "slot-2",
                appointmentType = "FOLLOW_UP",
                reason = "Bring results",
                hasReminder = false,
            ),
        )

        val stored = AppointmentLocalDataSource(db).getById("appointment-1")
        assertEquals("slot-2", stored?.bookingId)
        assertEquals("FOLLOW_UP", stored?.appointmentType)
        assertEquals("Bring results", stored?.notes)
        assertEquals(false, stored?.hasReminder)
    }

    @Test
    fun appointmentRepositoryFiltersCancelledBookingsFromCalendar() = runTest {
        val db = newDatabase()
        val local = AppointmentLocalDataSource(db)
        local.insert(
            sampleAppointment(
                id = "appointment-confirmed",
                bookingId = "slot-1",
                status = "CONFIRMED",
            ),
        )
        local.insert(
            sampleAppointment(
                id = "appointment-cancelled",
                bookingId = null,
                status = "CANCELLED",
            ),
        )
        val repository = AppointmentDataRepositoryImpl(
            appointmentLocal = local,
            apiService = BookingApiStub(),
        )

        assertEquals(listOf("appointment-confirmed"), repository.getAppointments().map(Appointment::id))
        assertEquals(
            listOf("appointment-confirmed", "appointment-cancelled"),
            repository.getBookingHistory().map(Appointment::id),
        )
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
    fun routineAgendaRangeIncludesEmptyDaysAndMovedOccurrenceOnTargetDate() = runTest {
        val db = newDatabase()
        MedicationLocalDataSource(db).insert(sampleMedication())
        RoutineLocalDataSource(db).insert(
            Routine(
                id = "routine-1",
                userId = "",
                name = "Morning meds",
                timesOfDayMs = listOf(9 * 60 * 60 * 1000L),
                repeatType = RoutineRepeatType.DAILY,
                startDate = "2026-03-17",
                endDate = "2026-03-18",
                hasReminder = true,
                reminderOffsetsMins = listOf(0),
                status = RoutineStatus.ACTIVE,
                medicationIds = listOf("med-1"),
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

        val agendaRange = repository.getRoutineAgendaRange("2026-03-17", "2026-03-19")

        assertEquals(
            listOf("2026-03-17", "2026-03-18", "2026-03-19"),
            agendaRange.keys.toList(),
        )
        assertTrue(agendaRange.getValue("2026-03-17").isEmpty())
        assertTrue(agendaRange.getValue("2026-03-19").isEmpty())
        val movedDayAgenda = agendaRange.getValue("2026-03-18")
        assertEquals(2, movedDayAgenda.size)
        val rescheduledAgenda = movedDayAgenda.first { it.isRescheduled }
        assertEquals(movedTimestamp, rescheduledAgenda.occurrenceTimeMs)
        assertEquals(originalTimestamp, rescheduledAgenda.originalOccurrenceTimeMs)
    }

    @Test
    fun manualMedicationLogRangeBucketsLogsByDate() = runTest {
        val db = newDatabase()
        MedicationLocalDataSource(db).insert(sampleMedication())
        val logLocal = MedicationLogLocalDataSource(db)
        val repository = MedicationLogDataRepositoryImpl(
            medicationLocal = MedicationLocalDataSource(db),
            routineLocal = RoutineLocalDataSource(db),
            routineOccurrenceOverrideLocal = RoutineOccurrenceOverrideLocalDataSource(db),
            medicationLogLocal = logLocal,
            queuedWriteDispatcher = dispatcher(db, OutboxDataSource(db)),
            routineNotificationScheduler = RecordingRoutineNotificationScheduler(),
        )
        val firstLogTime = LocalDateTime(2026, Month.MARCH, 17, 8, 15, 0, 0)
            .toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()
        val secondLogTime = LocalDateTime(2026, Month.MARCH, 18, 22, 45, 0, 0)
            .toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()

        logLocal.insert(
            MedicationLog(
                id = "manual-1",
                medicationId = "med-1",
                medicationTime = firstLogTime,
                status = MedicationLogStatus.TAKEN,
                routineId = null,
                occurrenceTimeMs = null,
                medicationName = "PrEP",
            ),
        )
        logLocal.insert(
            MedicationLog(
                id = "manual-2",
                medicationId = "med-1",
                medicationTime = secondLogTime,
                status = MedicationLogStatus.SKIPPED,
                routineId = null,
                occurrenceTimeMs = null,
                medicationName = "PrEP",
            ),
        )

        val logsByDate = repository.getManualMedicationLogsRange("2026-03-17", "2026-03-19")

        assertEquals(
            listOf("2026-03-17", "2026-03-18", "2026-03-19"),
            logsByDate.keys.toList(),
        )
        assertEquals(listOf("manual-1"), logsByDate.getValue("2026-03-17").map { it.id })
        assertEquals(listOf("manual-2"), logsByDate.getValue("2026-03-18").map { it.id })
        assertTrue(logsByDate.getValue("2026-03-19").isEmpty())
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

private class BookingApiStub : ApiService {
    private val appointments = linkedMapOf<String, Appointment>()

    override suspend fun getHealth(): Map<String, String> = emptyMap()
    override suspend fun sendChatbotMessage(request: com.group8.comp2300.domain.model.chatbot.ChatbotRequest) =
        error("unused")
    override suspend fun getProducts() = emptyList<com.group8.comp2300.data.remote.dto.ProductDto>()
    override suspend fun getProduct(id: String) = error("unused")
    override suspend fun login(request: com.group8.comp2300.data.remote.dto.LoginRequest) = error("unused")
    override suspend fun refreshToken(request: com.group8.comp2300.data.remote.dto.RefreshTokenRequest) =
        error("unused")
    override suspend fun logout() = Unit
    override suspend fun getProfile() = error("unused")
    override suspend fun activateAccount(token: String) = error("unused")
    override suspend fun forgotPassword(email: String) = error("unused")
    override suspend fun resetPassword(token: String, newPassword: String) = error("unused")
    override suspend fun preregister(request: com.group8.comp2300.data.remote.dto.PreregisterRequest) = error("unused")
    override suspend fun completeProfile(request: com.group8.comp2300.data.remote.dto.CompleteProfileRequest) =
        error("unused")
    override suspend fun resendVerificationEmail(email: String) = error("unused")
    override suspend fun getClinics(): List<Clinic> = emptyList()
    override suspend fun getClinic(id: String): Clinic = error("unused")
    override suspend fun getClinicAvailability(clinicId: String): List<AppointmentSlot> = emptyList()
    override suspend fun getAppointments(): List<Appointment> = appointments.values.toList()

    override suspend fun bookClinicAppointment(request: ClinicBookingRequest): Appointment = sampleAppointment(
        id = "appointment-1",
        clinicId = request.clinicId,
        bookingId = request.slotId,
        appointmentType = request.appointmentType,
        notes = request.reason,
        hasReminder = request.hasReminder,
    ).also { appointments[it.id] = it }

    override suspend fun cancelAppointment(id: String): Appointment = appointments.getValue(id)
        .copy(status = "CANCELLED", bookingId = null)
        .also { appointments[id] = it }

    override suspend fun rescheduleAppointment(id: String, request: ClinicBookingRequest): Appointment =
        appointments.getValue(id)
            .copy(
                appointmentType = request.appointmentType,
                clinicId = request.clinicId,
                bookingId = request.slotId,
                notes = request.reason,
                hasReminder = request.hasReminder,
            )
            .also { appointments[id] = it }

    override suspend fun logMedication(request: MedicationLogRequest) = error("unused")
    override suspend fun getMedicationLogHistory() = emptyList<com.group8.comp2300.domain.model.medical.MedicationLog>()
    override suspend fun getMedicationAgenda(date: String) =
        emptyList<com.group8.comp2300.domain.model.medical.MedicationLog>()
    override suspend fun getRoutineAgenda(date: String) = error("unused")
    override suspend fun logMood(request: com.group8.comp2300.domain.model.medical.MoodEntryRequest) = error("unused")
    override suspend fun getMoodHistory() = emptyList<com.group8.comp2300.domain.model.medical.Mood>()
    override suspend fun getUserMedications() = emptyList<com.group8.comp2300.domain.model.medical.Medication>()
    override suspend fun upsertMedication(id: String, request: MedicationCreateRequest) = error("unused")

    override suspend fun deleteMedication(id: String) = Unit
    override suspend fun getUserRoutines() = emptyList<Routine>()
    override suspend fun upsertRoutine(id: String, request: RoutineCreateRequest) = error("unused")

    override suspend fun deleteRoutine(id: String) = Unit
    override suspend fun getRoutineOccurrenceOverrides() =
        emptyList<com.group8.comp2300.domain.model.medical.RoutineOccurrenceOverride>()

    override suspend fun upsertRoutineOccurrenceOverride(request: RoutineOccurrenceOverrideRequest) = error("unused")

    override suspend fun getMedicalRecords(sort: String) =
        emptyList<com.group8.comp2300.domain.model.medical.MedicalRecordResponse>()
    override suspend fun uploadMedicalRecord(
        fileBytes: ByteArray,
        fileName: String,
        category: com.group8.comp2300.domain.model.medical.MedicalRecordCategory,
    ) = Unit

    override suspend fun downloadMedicalRecord(id: String): ByteArray = ByteArray(0)
    override suspend fun deleteMedicalRecord(id: String) = Unit
    override suspend fun getEducationCategories() = emptyList<com.group8.comp2300.data.remote.dto.CategoryDto>()
    override suspend fun getEducationArticles() = emptyList<com.group8.comp2300.data.remote.dto.ArticleSummaryDto>()
    override suspend fun getEducationArticle(id: String) = error("unused")
    override suspend fun getEducationQuiz(id: String) = error("unused")
    override suspend fun submitEducationQuiz(
        quizId: String,
        request: com.group8.comp2300.data.remote.dto.QuizSubmissionRequestDto,
    ) = error("unused")
    override suspend fun getEducationQuizStats() = error("unused")
    override suspend fun getEducationEarnedBadges() = emptyList<com.group8.comp2300.data.remote.dto.EarnedBadgeDto>()
}

private fun sampleAppointment(
    id: String,
    clinicId: String = "clinic-1",
    bookingId: String? = "slot-1",
    appointmentType: String = "CONSULTATION",
    notes: String? = "Fever",
    hasReminder: Boolean = true,
    status: String = "CONFIRMED",
): Appointment = Appointment(
    id = id,
    userId = "user-1",
    title = "Appointment at Clinic",
    appointmentTime = 123456789L,
    appointmentType = appointmentType,
    clinicId = clinicId,
    bookingId = bookingId,
    status = status,
    notes = notes,
    hasReminder = hasReminder,
    paymentStatus = "PENDING",
)

class TestSyncCoordinator : SyncCoordinator {
    var flushCalls: Int = 0
    var refreshCalls: Int = 0

    override suspend fun flushOutbox(): SyncStatus {
        flushCalls += 1
        return SyncStatus(
            hasAuthenticatedSession = true,
            pendingCount = 0,
            failedCount = 0,
            refreshed = false,
        )
    }

    override suspend fun refreshAuthenticatedData(): SyncStatus {
        refreshCalls += 1
        return SyncStatus(
            hasAuthenticatedSession = true,
            pendingCount = 0,
            failedCount = 0,
            refreshed = true,
        )
    }

    override suspend fun getFailedMutations(): List<FailedSyncMutation> = emptyList()

    override suspend fun retryFailedMutation(id: String): SyncStatus = SyncStatus(
        hasAuthenticatedSession = true,
        pendingCount = 0,
        failedCount = 0,
        refreshed = false,
    )

    override suspend fun discardMutation(id: String) = Unit
}

private class FakeSessionAuthRepository(initialSession: AuthSession) : AuthRepository {
    private val _session = MutableStateFlow(initialSession)
    override val session: StateFlow<AuthSession> = _session

    override suspend fun login(email: String, password: String) = error("unused")

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
