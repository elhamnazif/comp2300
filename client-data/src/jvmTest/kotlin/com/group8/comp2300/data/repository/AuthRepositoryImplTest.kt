package com.group8.comp2300.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.group8.comp2300.data.auth.TokenManagerImpl
import com.group8.comp2300.data.database.AppDatabase
import com.group8.comp2300.data.local.*
import com.group8.comp2300.data.remote.ApiException
import com.group8.comp2300.data.remote.ApiService
import com.group8.comp2300.data.remote.dto.*
import com.group8.comp2300.data.repository.medical.TestOfflineSyncCoordinator
import com.group8.comp2300.domain.model.chatbot.ChatbotMessage
import com.group8.comp2300.domain.model.chatbot.ChatbotRequest
import com.group8.comp2300.domain.model.chatbot.ChatbotResponse
import com.group8.comp2300.domain.model.chatbot.ChatbotRole
import com.group8.comp2300.domain.model.medical.*
import com.group8.comp2300.domain.model.session.AuthSession
import com.group8.comp2300.domain.model.shop.Order
import com.group8.comp2300.domain.model.shop.PlaceOrderRequest
import com.group8.comp2300.domain.model.user.Gender
import com.group8.comp2300.domain.model.user.SexualOrientation
import com.group8.comp2300.domain.model.user.UpdateProfileInput
import com.group8.comp2300.domain.model.user.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.time.Clock

class AuthRepositoryImplTest {
    @Test
    fun loginSetsSignedInSessionAndTriggersSync() = runTest {
        val db = newDatabase()
        val sessionDataSource = SessionDataSource(db)
        val tokenManager = TokenManagerImpl(sessionDataSource)
        val offlineSyncCoordinator = TestOfflineSyncCoordinator()
        val apiService = FakeApiService()
        val repository = AuthRepositoryImpl(
            apiService = apiService,
            tokenManager = tokenManager,
            sessionDataSource = sessionDataSource,
            personalDataCleaner = PersonalDataCleaner(db),
            offlineSyncCoordinator = offlineSyncCoordinator,
        )

        repository.awaitInitialRestore()
        val result = repository.login("user@example.com", "pw")

        assertTrue(result.isSuccess)
        val session = repository.session.value
        assertIs<AuthSession.SignedIn>(session)
        assertEquals("user-1", session.user.id)
        assertEquals("user-1", tokenManager.getUserId())
        assertEquals(1, offlineSyncCoordinator.syncNowCallCount)
        assertEquals(0, offlineSyncCoordinator.refreshCacheCallCount)
    }

    @Test
    fun expiredAccessTokenRestoreKeepsSessionAndPersonalDataWhenProfileLoads() = runTest {
        val db = newDatabase()
        val sessionDataSource = SessionDataSource(db)
        val tokenManager = TokenManagerImpl(sessionDataSource)
        val offlineSyncCoordinator = TestOfflineSyncCoordinator()
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
            sessionDataSource = sessionDataSource,
            personalDataCleaner = PersonalDataCleaner(db),
            offlineSyncCoordinator = offlineSyncCoordinator,
        )

        repository.awaitInitialRestore()

        val session = repository.session.value
        assertIs<AuthSession.SignedIn>(session)
        assertEquals("user-1", session.user.id)
        assertFalse(session.isStale)
        assertEquals("user-1", tokenManager.getUserId())
        assertEquals(1, offlineSyncCoordinator.syncNowCallCount)
        assertEquals(0, offlineSyncCoordinator.refreshCacheCallCount)
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
            sessionDataSource = sessionDataSource,
            personalDataCleaner = PersonalDataCleaner(db),
            offlineSyncCoordinator = TestOfflineSyncCoordinator(),
        )

        repository.awaitInitialRestore()

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
            sessionDataSource = sessionDataSource,
            personalDataCleaner = PersonalDataCleaner(db),
            offlineSyncCoordinator = TestOfflineSyncCoordinator(),
        )

        repository.awaitInitialRestore()

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
            sessionDataSource = sessionDataSource,
            personalDataCleaner = PersonalDataCleaner(db),
            offlineSyncCoordinator = TestOfflineSyncCoordinator(),
        )

        repository.awaitInitialRestore()
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

    @Test
    fun cachedSessionIsAvailableBeforeBackgroundRestoreFinishes() = runTest {
        val db = newDatabase()
        val sessionDataSource = SessionDataSource(db)
        val tokenManager = TokenManagerImpl(sessionDataSource)
        tokenManager.saveTokens(
            userId = "user-1",
            accessToken = "access",
            refreshToken = "refresh",
            expiresAt = Clock.System.now().toEpochMilliseconds() - 1,
        )
        val repository = AuthRepositoryImpl(
            apiService = object : FakeApiService() {
                override suspend fun getProfile(): User {
                    delay(250)
                    return super.getProfile()
                }
            },
            tokenManager = tokenManager,
            sessionDataSource = sessionDataSource,
            personalDataCleaner = PersonalDataCleaner(db),
            offlineSyncCoordinator = TestOfflineSyncCoordinator(),
        )

        val immediateSession = repository.session.value
        assertIs<AuthSession.SignedIn>(immediateSession)
        assertEquals("user-1", immediateSession.user.id)
        assertTrue(immediateSession.isStale)

        repository.awaitInitialRestore()
        assertFalse((repository.session.value as AuthSession.SignedIn).isStale)
    }

    @Test
    fun updateProfileRefreshesSignedInSession() = runTest {
        val db = newDatabase()
        val sessionDataSource = SessionDataSource(db)
        val repository = AuthRepositoryImpl(
            apiService = FakeApiService(),
            tokenManager = TokenManagerImpl(sessionDataSource),
            personalDataCleaner = PersonalDataCleaner(db),
            offlineSyncCoordinator = TestOfflineSyncCoordinator(),
            sessionDataSource = sessionDataSource,
        )

        repository.awaitInitialRestore()
        repository.login("user@example.com", "pw")

        val result = repository.updateProfile(
            UpdateProfileInput(
                firstName = "Updated",
                lastName = "Name",
                phone = "12345678",
            ),
        )

        assertTrue(result.isSuccess)
        val session = repository.session.value as AuthSession.SignedIn
        assertEquals("Updated", session.user.firstName)
        assertEquals("Name", session.user.lastName)
        assertEquals("12345678", session.user.phone)
    }

    @Test
    fun uploadProfilePhotoRefreshesSignedInSession() = runTest {
        val db = newDatabase()
        val repository = AuthRepositoryImpl(
            apiService = FakeApiService(),
            tokenManager = TokenManagerImpl(SessionDataSource(db)),
            personalDataCleaner = PersonalDataCleaner(db),
            offlineSyncCoordinator = TestOfflineSyncCoordinator(),
            sessionDataSource = SessionDataSource(db),
        )

        repository.awaitInitialRestore()
        repository.login("user@example.com", "pw")

        val result = repository.uploadProfilePhoto("image".encodeToByteArray(), "avatar.png")

        assertTrue(result.isSuccess)
        val session = repository.session.value as AuthSession.SignedIn
        assertEquals("/images/profile/avatar.png", session.user.profileImageUrl)
    }

    @Test
    fun removeProfilePhotoRefreshesSignedInSession() = runTest {
        val db = newDatabase()
        val apiService = FakeApiService(
            profileUser = User(
                id = "user-1",
                email = "user@example.com",
                firstName = "Test",
                lastName = "User",
                profileImageUrl = "/images/profile/avatar.png",
            ),
        )
        val repository = AuthRepositoryImpl(
            apiService = apiService,
            tokenManager = TokenManagerImpl(SessionDataSource(db)),
            personalDataCleaner = PersonalDataCleaner(db),
            offlineSyncCoordinator = TestOfflineSyncCoordinator(),
            sessionDataSource = SessionDataSource(db),
        )

        repository.awaitInitialRestore()
        repository.login("user@example.com", "pw")

        val result = repository.removeProfilePhoto()

        assertTrue(result.isSuccess)
        val session = repository.session.value as AuthSession.SignedIn
        assertNull(session.user.profileImageUrl)
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
    var orders: MutableList<Order> = mutableListOf(),
) : ApiService {
    override suspend fun getHealth(): Map<String, String> = emptyMap()
    override suspend fun sendChatbotMessage(request: ChatbotRequest): ChatbotResponse = ChatbotResponse(
        message = ChatbotMessage(
            role = ChatbotRole.ASSISTANT,
            content = "ok",
        ),
    )
    override suspend fun getProducts(): List<com.group8.comp2300.data.remote.dto.ProductDto> = emptyList()
    override suspend fun getProduct(id: String): com.group8.comp2300.data.remote.dto.ProductDto = error("unused")
    override suspend fun placeOrder(request: PlaceOrderRequest): Order = error("unused")
    override suspend fun getOrders(): List<Order> = orders.toList()
    override suspend fun login(request: LoginRequest): AuthResponse = AuthResponse(profileUser, "access", "refresh")

    override suspend fun refreshToken(request: RefreshTokenRequest): TokenResponse = error("unused")

    override suspend fun logout() = Unit

    override suspend fun getProfile(): User = profileUser

    override suspend fun activateAccount(token: String): AuthResponse = AuthResponse(profileUser, "access", "refresh")

    override suspend fun forgotPassword(email: String): MessageResponse = MessageResponse("ok")

    override suspend fun resetPassword(token: String, newPassword: String): MessageResponse = MessageResponse("ok")

    override suspend fun preregister(request: PreregisterRequest): PreregisterResponse =
        PreregisterResponse(request.email, "ok")

    override suspend fun updateProfile(request: UpdateProfileRequest): User {
        profileUser = profileUser.copy(
            firstName = request.firstName,
            lastName = request.lastName,
            phone = request.phone,
            gender = request.gender?.let(Gender::valueOf),
            sexualOrientation = request.sexualOrientation?.let(SexualOrientation::valueOf),
        )
        return profileUser
    }

    override suspend fun uploadProfilePhoto(fileBytes: ByteArray, fileName: String): User {
        profileUser = profileUser.copy(profileImageUrl = "/images/profile/$fileName")
        return profileUser
    }

    override suspend fun removeProfilePhoto(): User {
        profileUser = profileUser.copy(profileImageUrl = null)
        return profileUser
    }

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

    override suspend fun getEducationCategories(): List<CategoryDto> = emptyList()

    override suspend fun getEducationArticles(): List<ArticleSummaryDto> = emptyList()

    override suspend fun getEducationArticle(id: String): ArticleDetailDto = error("unused")

    override suspend fun getEducationQuiz(id: String): QuizDto = error("unused")

    override suspend fun submitEducationQuiz(
        quizId: String,
        request: QuizSubmissionRequestDto,
    ): QuizSubmissionResultDto = error("unused")

    override suspend fun getEducationQuizStats(): UserQuizStatsDto = error("unused")

    override suspend fun getEducationEarnedBadges(): List<EarnedBadgeDto> = emptyList()
}
