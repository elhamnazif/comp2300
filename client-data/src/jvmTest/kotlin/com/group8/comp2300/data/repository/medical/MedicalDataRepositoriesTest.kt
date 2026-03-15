package com.group8.comp2300.data.repository.medical

import com.group8.comp2300.data.auth.TokenManagerImpl
import com.group8.comp2300.data.local.MedicationLocalDataSource
import com.group8.comp2300.data.local.MedicationLogLocalDataSource
import com.group8.comp2300.data.local.MoodLocalDataSource
import com.group8.comp2300.data.local.OutboxDataSource
import com.group8.comp2300.data.local.OutboxState
import com.group8.comp2300.data.local.SessionDataSource
import com.group8.comp2300.data.offline.QueuedWriteDispatcher
import com.group8.comp2300.data.offline.SyncCoordinatorImpl
import com.group8.comp2300.data.repository.FakeApiService
import com.group8.comp2300.data.repository.newDatabase
import com.group8.comp2300.data.repository.sampleMedication
import com.group8.comp2300.domain.model.medical.MedicationCreateRequest
import com.group8.comp2300.domain.model.medical.MedicationLog
import com.group8.comp2300.domain.model.medical.MedicationLogRequest
import com.group8.comp2300.domain.model.medical.MedicationLogStatus
import com.group8.comp2300.domain.model.medical.Mood
import com.group8.comp2300.domain.model.medical.MoodType
import com.group8.comp2300.domain.model.session.AuthSession
import com.group8.comp2300.domain.repository.AuthRepository
import com.group8.comp2300.domain.repository.medical.SyncCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock

class MedicalDataRepositoriesTest {
    @Test
    fun guestMedicationSaveQueuesPendingWrite() = runTest {
        val db = newDatabase()
        val outbox = OutboxDataSource(db)
        val tokenManager = TokenManagerImpl(SessionDataSource(db))
        val syncCoordinator = TestSyncCoordinator()
        val repository = MedicationDataRepositoryImpl(
            authRepository = FakeSessionAuthRepository(AuthSession.SignedOut),
            medicationLocal = MedicationLocalDataSource(db),
            queuedWriteDispatcher = QueuedWriteDispatcher(tokenManager, outbox, syncCoordinator),
        )

        repository.saveMedication(
            MedicationCreateRequest(
                name = "Vitamin D",
                dosage = "1 tablet",
                quantity = "1000 IU",
                frequency = "DAILY",
                startDate = "2026-03-01",
                endDate = "2026-12-31",
            ),
        )

        assertEquals(1, MedicationLocalDataSource(db).getAll().size)
        assertEquals(1, outbox.getAll().size)
        assertEquals(OutboxState.PENDING, outbox.getAll().single().state)
        assertEquals(0, syncCoordinator.flushCalls)
    }

    @Test
    fun syncCoordinatorFlushesOutboxAndRefreshesCurrentCache() = runTest {
        val db = newDatabase()
        val sessionDataSource = SessionDataSource(db)
        val tokenManager = TokenManagerImpl(sessionDataSource)
        tokenManager.saveTokens(
            userId = "user-1",
            accessToken = "access",
            refreshToken = "refresh",
            expiresAt = Clock.System.now().toEpochMilliseconds() + 60_000,
        )

        val medicationLocal = MedicationLocalDataSource(db)
        val logLocal = MedicationLogLocalDataSource(db)
        val moodLocal = MoodLocalDataSource(db)
        val outbox = OutboxDataSource(db)
        val remoteMedication = sampleMedication(id = "server-med", userId = "user-1")
        val remoteLog = MedicationLog("server-log", "server-med", 10L, MedicationLogStatus.TAKEN, "Vitamin D")
        val remoteMood = Mood("server-mood", "user-1", 20L, MoodType.GOOD, "calm", null)
        val apiService = FakeApiService(
            medications = mutableListOf(remoteMedication),
            medicationLogs = mutableListOf(remoteLog),
            moods = mutableListOf(remoteMood),
        )

        val localMedication = sampleMedication(id = "local-med")
        medicationLocal.insert(localMedication)
        outbox.enqueue(
            entityType = "MEDICATION_UPSERT",
            payload = """{"name":"Vitamin D","dosage":"1 tablet","quantity":"1000 IU","frequency":"DAILY","startDate":"2026-03-01","endDate":"2026-12-31","hasReminder":true,"status":"ACTIVE"}""",
            localId = "local-med",
            state = OutboxState.PENDING,
        )

        val syncCoordinator = SyncCoordinatorImpl(
            tokenManager = tokenManager,
            outbox = outbox,
            apiService = object : FakeApiService(
                profileUser = apiService.profileUser,
                medications = apiService.medications,
                medicationLogs = apiService.medicationLogs,
                moods = apiService.moods,
                appointments = apiService.appointments,
            ) {
                override suspend fun upsertMedication(id: String, request: MedicationCreateRequest) = remoteMedication
            },
            appointmentLocal = com.group8.comp2300.data.local.AppointmentLocalDataSource(db),
            medicationLocal = medicationLocal,
            medicationLogLocal = logLocal,
            moodLocal = moodLocal,
        )

        syncCoordinator.flushOutbox()
        syncCoordinator.refreshAuthenticatedData()

        assertTrue(outbox.getAll().isEmpty())
        assertEquals(listOf(remoteMedication), medicationLocal.getAll())
        assertEquals(listOf(remoteLog), logLocal.getAll())
        assertEquals(listOf(remoteMood), moodLocal.getAll())
    }

    @Test
    fun medicationLogRepositoryStoresLocalEntryAndQueuesWrite() = runTest {
        val db = newDatabase()
        val medicationLocal = MedicationLocalDataSource(db)
        val outbox = OutboxDataSource(db)
        medicationLocal.insert(sampleMedication())

        val repository = MedicationLogDataRepositoryImpl(
            medicationLocal = medicationLocal,
            medicationLogLocal = MedicationLogLocalDataSource(db),
            queuedWriteDispatcher = QueuedWriteDispatcher(
                tokenManager = TokenManagerImpl(SessionDataSource(db)),
                outbox = outbox,
                syncCoordinator = TestSyncCoordinator(),
            ),
        )

        repository.logMedication(MedicationLogRequest("med-1", MedicationLogStatus.TAKEN.name, 123L))

        assertEquals(1, MedicationLogLocalDataSource(db).getAll().size)
        assertEquals(1, outbox.getAll().size)
        assertEquals("MEDICATION_LOG", outbox.getAll().single().entityType)
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
