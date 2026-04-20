package com.group8.comp2300.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.group8.comp2300.data.auth.TokenManagerImpl
import com.group8.comp2300.data.database.AppDatabase
import com.group8.comp2300.data.local.MedicationLocalDataSource
import com.group8.comp2300.data.local.OutboxDataSource
import com.group8.comp2300.data.local.OutboxState
import com.group8.comp2300.data.local.PersonalDataCleaner
import com.group8.comp2300.data.local.SessionDataSource
import com.group8.comp2300.data.remote.ApiException
import com.group8.comp2300.data.remote.ApiService
import com.group8.comp2300.data.remote.dto.AuthResponse
import com.group8.comp2300.data.remote.dto.CompleteProfileRequest
import com.group8.comp2300.data.remote.dto.ForgotPasswordRequest
import com.group8.comp2300.data.remote.dto.LoginRequest
import com.group8.comp2300.data.remote.dto.MessageResponse
import com.group8.comp2300.data.remote.dto.PreregisterRequest
import com.group8.comp2300.data.remote.dto.PreregisterResponse
import com.group8.comp2300.data.remote.dto.RefreshTokenRequest
import com.group8.comp2300.data.remote.dto.ResendVerificationRequest
import com.group8.comp2300.data.remote.dto.ResetPasswordRequest
import com.group8.comp2300.data.remote.dto.TokenResponse
import com.group8.comp2300.data.repository.medical.TestSyncCoordinator
import com.group8.comp2300.domain.model.medical.Appointment
import com.group8.comp2300.domain.model.medical.AppointmentSlot
import com.group8.comp2300.domain.model.medical.Clinic
import com.group8.comp2300.domain.model.medical.ClinicBookingRequest
import com.group8.comp2300.domain.model.medical.Medication
import com.group8.comp2300.domain.model.medical.MedicationCreateRequest
import com.group8.comp2300.domain.model.medical.MedicationLog
import com.group8.comp2300.domain.model.medical.MedicationLogRequest
import com.group8.comp2300.domain.model.medical.MedicationStatus
import com.group8.comp2300.domain.model.medical.MedicationUnit
import com.group8.comp2300.domain.model.medical.Mood
import com.group8.comp2300.domain.model.medical.MoodEntryRequest
import com.group8.comp2300.domain.model.medical.Routine
import com.group8.comp2300.domain.model.medical.RoutineCreateRequest
import com.group8.comp2300.domain.model.medical.RoutineDayAgenda
import com.group8.comp2300.domain.model.medical.RoutineOccurrenceOverride
import com.group8.comp2300.domain.model.medical.RoutineOccurrenceOverrideRequest
import com.group8.comp2300.domain.model.session.AuthSession
import com.group8.comp2300.domain.model.user.Gender
import com.group8.comp2300.domain.model.user.SexualOrientation
import com.group8.comp2300.domain.model.user.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

class AuthRepositoryImplTest {
    @Test
    fun loginSetsSignedInSessionAndTriggersSync() = runTest {
        val db = newDatabase()
        val sessionDataSource = SessionDataSource(db)
        val tokenManager = TokenManagerImpl(sessionDataSource)
        val syncCoordinator = TestSyncCoordinator()
        val apiService = FakeApiService()
        val repository = AuthRepositoryImpl(
            apiService = apiService,
            tokenManager = tokenManager,
            personalDataCleaner = PersonalDataCleaner(db),
            syncCoordinator = syncCoordinator,
        )

        waitForSession(repository)
        val result = repository.login("user@example.com", "pw")

        assertTrue(result.isSuccess)
        val session = repository.session.value
        assertIs<AuthSession.SignedIn>(session)
        assertEquals("user-1", session.user.id)
        assertEquals("user-1", tokenManager.getUserId())
        assertEquals(1, syncCoordinator.flushCalls)
        assertEquals(1, syncCoordinator.refreshCalls)
    }

    @Test
    fun expiredAccessTokenRestoreKeepsSessionAndPersonalDataWhenProfileLoads() = runTest {
        val db = newDatabase()
        val sessionDataSource = SessionDataSource(db)
        val tokenManager = TokenManagerImpl(sessionDataSource)
        val syncCoordinator = TestSyncCoordinator()
        tokenManager.saveTokens(
            userId = "user-1",
            accessToken = "access",
            refreshToken = "refresh",
            expiresAt = Clock.System.now().toEpochMilliseconds() - 1,
        )

        MedicationLocalDataSource(db).insert(sampleMedication())
        OutboxDataSource(db).enqueue(
            entityType = "MEDICATION_UPSERT",
            payload = "{}",
            localId = "med-1",
            state = OutboxState.PENDING,
        )

        val repository = AuthRepositoryImpl(
            apiService = FakeApiService(),
            tokenManager = tokenManager,
            personalDataCleaner = PersonalDataCleaner(db),
            syncCoordinator = syncCoordinator,
        )

        waitForSession(repository)

        val session = repository.session.value
        assertIs<AuthSession.SignedIn>(session)
        assertEquals("user-1", session.user.id)
        assertFalse(session.isStale)
        assertEquals("user-1", tokenManager.getUserId())
        assertEquals(1, syncCoordinator.flushCalls)
        assertEquals(1, syncCoordinator.refreshCalls)
        assertEquals(1, MedicationLocalDataSource(db).getAll().size)
        assertEquals(1, OutboxDataSource(db).getAll().size)
    }

    @Test
    fun transientRestoreFailureKeepsSignedInSessionMarkedStale() = runTest {
        val db = newDatabase()
        val sessionDataSource = SessionDataSource(db)
        val tokenManager = TokenManagerImpl(sessionDataSource)
        tokenManager.saveTokens(
            userId = "user-1",
            accessToken = "access",
            refreshToken = "refresh",
            expiresAt = Clock.System.now().toEpochMilliseconds() - 1,
        )

        MedicationLocalDataSource(db).insert(sampleMedication())
        OutboxDataSource(db).enqueue(
            entityType = "MEDICATION_UPSERT",
            payload = "{}",
            localId = "med-1",
            state = OutboxState.PENDING,
        )

        val repository = AuthRepositoryImpl(
            apiService = object : FakeApiService() {
                override suspend fun getProfile(): User = throw Exception("offline")
            },
            tokenManager = tokenManager,
            personalDataCleaner = PersonalDataCleaner(db),
            syncCoordinator = TestSyncCoordinator(),
        )

        waitForSession(repository)

        val session = repository.session.value
        assertIs<AuthSession.SignedIn>(session)
        assertTrue(session.isStale)
        assertEquals("user-1", session.user.id)
        assertEquals("user-1", tokenManager.getUserId())
        assertEquals(1, MedicationLocalDataSource(db).getAll().size)
        assertEquals(1, OutboxDataSource(db).getAll().size)
    }

    @Test
    fun restoreClearsSessionAndPersonalDataWhenProfileIsUnauthorized() = runTest {
        val db = newDatabase()
        val sessionDataSource = SessionDataSource(db)
        val tokenManager = TokenManagerImpl(sessionDataSource)
        tokenManager.saveTokens(
            userId = "user-1",
            accessToken = "access",
            refreshToken = "refresh",
            expiresAt = Clock.System.now().toEpochMilliseconds() - 1,
        )

        MedicationLocalDataSource(db).insert(sampleMedication())
        OutboxDataSource(db).enqueue(
            entityType = "MEDICATION_UPSERT",
            payload = "{}",
            localId = "med-1",
            state = OutboxState.PENDING,
        )

        val repository = AuthRepositoryImpl(
            apiService = object : FakeApiService() {
                override suspend fun getProfile(): User = throw ApiException(401, "Authentication failed")
            },
            tokenManager = tokenManager,
            personalDataCleaner = PersonalDataCleaner(db),
            syncCoordinator = TestSyncCoordinator(),
        )

        waitForSession(repository)

        assertIs<AuthSession.SignedOut>(repository.session.value)
        assertNull(tokenManager.getUserId())
        assertTrue(MedicationLocalDataSource(db).getAll().isEmpty())
        assertTrue(OutboxDataSource(db).getAll().isEmpty())
    }

    @Test
    fun logoutClearsLocalPersonalData() = runTest {
        val db = newDatabase()
        val sessionDataSource = SessionDataSource(db)
        val tokenManager = TokenManagerImpl(sessionDataSource)
        val repository = AuthRepositoryImpl(
            apiService = FakeApiService(),
            tokenManager = tokenManager,
            personalDataCleaner = PersonalDataCleaner(db),
            syncCoordinator = TestSyncCoordinator(),
        )

        waitForSession(repository)
        repository.login("user@example.com", "pw")
        MedicationLocalDataSource(db).insert(sampleMedication())
        OutboxDataSource(db).enqueue(
            entityType = "MEDICATION_UPSERT",
            payload = "{}",
            localId = "med-1",
            state = OutboxState.PENDING,
        )

        repository.logout()

        assertIs<AuthSession.SignedOut>(repository.session.value)
        assertNull(tokenManager.getUserId())
        assertTrue(MedicationLocalDataSource(db).getAll().isEmpty())
        assertTrue(OutboxDataSource(db).getAll().isEmpty())
    }

    private suspend fun waitForSession(repository: AuthRepositoryImpl) {
        val deadline = Clock.System.now() + 2.seconds
        while (repository.session.value is AuthSession.Restoring && Clock.System.now() < deadline) {
            delay(10)
        }
    }
}

internal fun newDatabase(): AppDatabase {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    AppDatabase.Schema.create(driver)
    return AppDatabase(driver)
}

internal fun sampleMedication(id: String = "med-1", userId: String = "") = Medication(
    id = id,
    userId = userId,
    name = "Vitamin D",
    doseAmount = "1",
    doseUnit = MedicationUnit.TABLET,
    stockAmount = "30",
    stockUnit = MedicationUnit.TABLET,
    instruction = null,
    colorHex = "#42A5F5",
    status = MedicationStatus.ACTIVE,
)

internal open class FakeApiService(
    var profileUser: User = User(
        id = "user-1",
        email = "user@example.com",
        firstName = "Test",
        lastName = "User",
        gender = Gender.PREFER_NOT_TO_SAY,
        sexualOrientation = SexualOrientation.PREFER_NOT_TO_SAY,
        dateOfBirth = null,
    ),
    var medications: MutableList<Medication> = mutableListOf(),
    var routines: MutableList<Routine> = mutableListOf(),
    var routineOccurrenceOverrides: MutableList<RoutineOccurrenceOverride> = mutableListOf(),
    var medicationLogs: MutableList<MedicationLog> = mutableListOf(),
    var moods: MutableList<Mood> = mutableListOf(),
    var appointments: MutableList<Appointment> = mutableListOf(),
) : ApiService {
    override suspend fun getHealth(): Map<String, String> = emptyMap()
    override suspend fun getProducts() = emptyList<com.group8.comp2300.data.remote.dto.ProductDto>()
    override suspend fun getProduct(id: String) = error("unused")
    override suspend fun login(request: LoginRequest): AuthResponse = AuthResponse(profileUser, "access", "refresh")

    override suspend fun refreshToken(request: RefreshTokenRequest): TokenResponse = error("unused")

    override suspend fun logout() = Unit

    override suspend fun getProfile(): User = profileUser

    override suspend fun activateAccount(token: String): AuthResponse = AuthResponse(profileUser, "access", "refresh")

    override suspend fun forgotPassword(email: String): MessageResponse = MessageResponse("ok")

    override suspend fun resetPassword(token: String, newPassword: String): MessageResponse = MessageResponse("ok")

    override suspend fun preregister(request: PreregisterRequest): PreregisterResponse =
        PreregisterResponse(request.email, "ok")

    override suspend fun completeProfile(request: CompleteProfileRequest): User = profileUser

    override suspend fun resendVerificationEmail(email: String): MessageResponse = MessageResponse("ok")

    override suspend fun getClinics(): List<Clinic> = emptyList()

    override suspend fun getClinic(id: String): Clinic = error("unused")

    override suspend fun getClinicAvailability(clinicId: String): List<AppointmentSlot> = emptyList()

    override suspend fun getAppointments(): List<Appointment> = appointments.toList()

    override suspend fun bookClinicAppointment(request: ClinicBookingRequest): Appointment = error("unused")

    override suspend fun cancelAppointment(id: String): Appointment = error("unused")

    override suspend fun rescheduleAppointment(id: String, request: ClinicBookingRequest): Appointment = error("unused")

    override suspend fun logMedication(request: MedicationLogRequest): MedicationLog = error("unused")

    override suspend fun getMedicationLogHistory(): List<MedicationLog> = medicationLogs.toList()

    override suspend fun getMedicationAgenda(date: String): List<MedicationLog> = medicationLogs.toList()

    override suspend fun getRoutineAgenda(date: String): List<RoutineDayAgenda> = error("unused")

    override suspend fun logMood(request: MoodEntryRequest): Mood = error("unused")

    override suspend fun getMoodHistory(): List<Mood> = moods.toList()

    override suspend fun getUserMedications(): List<Medication> = medications.toList()

    override suspend fun upsertMedication(id: String, request: MedicationCreateRequest): Medication = error("unused")

    override suspend fun deleteMedication(id: String) = Unit

    override suspend fun getUserRoutines(): List<Routine> = routines.toList()

    override suspend fun upsertRoutine(id: String, request: RoutineCreateRequest): Routine = error("unused")

    override suspend fun deleteRoutine(id: String) = Unit

    override suspend fun getRoutineOccurrenceOverrides(): List<RoutineOccurrenceOverride> =
        routineOccurrenceOverrides.toList()

    override suspend fun upsertRoutineOccurrenceOverride(
        request: RoutineOccurrenceOverrideRequest,
    ): RoutineOccurrenceOverride = error("unused")

    override suspend fun getMedicalRecords(sort: String) =
        emptyList<com.group8.comp2300.domain.model.medical.MedicalRecordResponse>()

    override suspend fun uploadMedicalRecord(
        fileBytes: ByteArray,
        fileName: String,
        category: com.group8.comp2300.domain.model.medical.MedicalRecordCategory,
    ) = Unit

    override suspend fun downloadMedicalRecord(id: String): ByteArray = error("unused")

    override suspend fun deleteMedicalRecord(id: String) = Unit
}
