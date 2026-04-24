package com.group8.comp2300.feature.profile

import com.group8.comp2300.domain.model.session.AuthSession
import com.group8.comp2300.domain.model.user.UpdateProfileInput
import com.group8.comp2300.domain.model.user.User
import com.group8.comp2300.domain.repository.AuthRepository
import com.group8.comp2300.domain.usecase.auth.UpdateProfileUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileEditorViewModelTest {
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
    fun `restored signed in session refreshes editor seed state`() = runTest(dispatcher) {
        val repository = FakeAuthRepository(
            initialSession = AuthSession.SignedIn(
                user = User(id = "user-1", firstName = "", lastName = "", email = ""),
                isStale = true,
            ),
        )
        val viewModel = ProfileEditorViewModel(
            authRepository = repository,
            updateProfileUseCase = UpdateProfileUseCase(repository),
        )

        repository.sessionFlow.value = AuthSession.SignedIn(
            user = User(
                id = "user-1",
                firstName = "Jamie",
                lastName = "Park",
                email = "jamie@example.com",
                phone = "12345678",
            ),
        )

        advanceUntilIdle()

        assertEquals("Jamie", viewModel.state.value.firstName)
        assertEquals("Park", viewModel.state.value.lastName)
        assertEquals("12345678", viewModel.state.value.phone)
    }
}

private class FakeAuthRepository(initialSession: AuthSession) : AuthRepository {
    val sessionFlow = MutableStateFlow(initialSession)

    override val session: StateFlow<AuthSession> = sessionFlow

    override suspend fun login(email: String, password: String) = error("unused")

    override suspend fun preregister(email: String, password: String) = error("unused")

    override suspend fun updateProfile(input: UpdateProfileInput) = error("unused")

    override suspend fun uploadProfilePhoto(fileBytes: ByteArray, fileName: String) = error("unused")

    override suspend fun removeProfilePhoto() = error("unused")

    override suspend fun activateAccount(token: String) = error("unused")

    override suspend fun forgotPassword(email: String) = error("unused")

    override suspend fun resendVerificationEmail(email: String) = error("unused")

    override suspend fun resetPassword(token: String, newPassword: String) = error("unused")

    override suspend fun changePassword(currentPassword: String, newPassword: String) = error("unused")

    override suspend fun requestEmailChange(currentPassword: String, newEmail: String) = error("unused")

    override suspend fun confirmEmailChange(code: String) = error("unused")

    override suspend fun deactivateAccount(currentPassword: String) = error("unused")

    override suspend fun logout() = error("unused")
}
