package com.group8.comp2300.app.navigation

import androidx.lifecycle.SavedStateHandle
import com.group8.comp2300.domain.model.session.AuthSession
import com.group8.comp2300.domain.model.user.User
import com.group8.comp2300.domain.repository.AuthRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class RealNavigatorTest {
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
    fun `tab start destinations resolve to MainShell`() = runTest(dispatcher) {
        val navigator = createNavigator()

        navigator.clearAndGoTo(Screen.Home)

        assertEquals(listOf(Screen.MainShell), navigator.backStack)
        assertEquals(listOf(Screen.Home), navigator.mainShellBackStack)
        assertEquals(Screen.Home, navigator.currentTab)
    }

    @Test
    fun `navigating to a tab updates shell state instead of pushing a root destination`() = runTest(dispatcher) {
        val navigator = createNavigator()
        navigator.clearAndGoTo(Screen.Home)

        navigator.navigate(Screen.Booking)

        assertEquals(listOf(Screen.MainShell), navigator.backStack)
        assertEquals(listOf(Screen.Home, Screen.Booking), navigator.mainShellBackStack)
        assertEquals(Screen.Booking, navigator.currentTab)
    }

    @Test
    fun `overlay destinations push above MainShell`() = runTest(dispatcher) {
        val navigator = createNavigator()
        navigator.clearAndGoTo(Screen.Booking)

        navigator.navigate(Screen.ClinicDetail("clinic-1"))

        assertEquals(listOf(Screen.MainShell, Screen.ClinicDetail("clinic-1")), navigator.backStack)
        assertEquals(Screen.Booking, navigator.currentTab)
    }

    @Test
    fun `home inbox pushes above MainShell as an overlay destination`() = runTest(dispatcher) {
        val navigator = createNavigator()
        navigator.clearAndGoTo(Screen.Home)

        navigator.navigate(Screen.HomeInbox)

        assertEquals(listOf(Screen.MainShell, Screen.HomeInbox), navigator.backStack)
        assertEquals(Screen.Home, navigator.currentTab)
    }

    @Test
    fun `shell destinations stay in the shell stack`() = runTest(dispatcher) {
        val navigator = createNavigator()
        navigator.clearAndGoTo(Screen.Booking)

        navigator.navigateWithinShell(Screen.ClinicDetail("clinic-1"))

        assertEquals(listOf(Screen.MainShell), navigator.backStack)
        assertEquals(listOf(Screen.Booking, Screen.ClinicDetail("clinic-1")), navigator.mainShellBackStack)
        assertEquals(Screen.Booking, navigator.currentTab)
    }

    @Test
    fun `auth success to a tab restores MainShell and selects the target tab`() = runTest(dispatcher) {
        val navigator = createNavigator()
        navigator.clearAndGoTo(Screen.Home)

        navigator.requireAuth(Screen.Profile)
        navigator.onAuthSuccess()

        assertEquals(listOf(Screen.MainShell), navigator.backStack)
        assertEquals(listOf(Screen.Profile), navigator.mainShellBackStack)
        assertEquals(Screen.Profile, navigator.currentTab)
    }

    @Test
    fun `restored pre shell tab stacks normalize into MainShell`() = runTest(dispatcher) {
        val navigator =
            createNavigator(
                savedStateHandle = SavedStateHandle(
                    mapOf(
                        "nav_stack" to listOf(
                            Json.encodeToString<Screen>(Screen.Booking),
                            Json.encodeToString<Screen>(Screen.ClinicDetail("clinic-1")),
                        ),
                    ),
                ),
            )

        assertEquals(listOf(Screen.MainShell, Screen.ClinicDetail("clinic-1")), navigator.backStack)
        assertEquals(listOf(Screen.Booking), navigator.mainShellBackStack)
        assertEquals(Screen.Booking, navigator.currentTab)
    }

    @Test
    fun `restored pre shell stacks keep the last visible tab inside MainShell`() = runTest(dispatcher) {
        val navigator =
            createNavigator(
                savedStateHandle = SavedStateHandle(
                    mapOf(
                        "nav_stack" to listOf(
                            Json.encodeToString<Screen>(Screen.Home),
                            Json.encodeToString<Screen>(Screen.Booking),
                            Json.encodeToString<Screen>(Screen.ClinicDetail("clinic-1")),
                        ),
                    ),
                ),
            )

        assertEquals(listOf(Screen.MainShell, Screen.ClinicDetail("clinic-1")), navigator.backStack)
        assertEquals(listOf(Screen.Booking), navigator.mainShellBackStack)
        assertEquals(Screen.Booking, navigator.currentTab)
    }

    @Test
    fun `guest navigate to chatbot pushes login without adding chatbot`() = runTest(dispatcher) {
        val navigator = createNavigator()
        navigator.clearAndGoTo(Screen.Home)

        navigator.navigate(Screen.Chatbot)

        assertEquals(listOf(Screen.MainShell, Screen.Login), navigator.backStack)
        assertEquals(listOf(Screen.Home), navigator.mainShellBackStack)
        assertEquals(Screen.Chatbot, navigator.postLoginTarget)
    }

    @Test
    fun `guest checkout redirect keeps cart visible underneath login`() = runTest(dispatcher) {
        val navigator = createNavigator()
        navigator.clearAndGoTo(Screen.Home)
        navigator.navigate(Screen.Cart)

        navigator.navigate(Screen.Checkout)

        assertEquals(listOf(Screen.MainShell, Screen.Cart, Screen.Login), navigator.backStack)
        assertEquals(Screen.Checkout, navigator.postLoginTarget)
    }

    @Test
    fun `guest shell protected navigation redirects through login without mutating shell stack`() = runTest(dispatcher) {
        val navigator = createNavigator()
        navigator.clearAndGoTo(Screen.Booking)

        navigator.navigateWithinShell(Screen.BookingHistory())

        assertEquals(listOf(Screen.MainShell, Screen.Login), navigator.backStack)
        assertEquals(listOf(Screen.Booking), navigator.mainShellBackStack)
        assertEquals(Screen.BookingHistory(), navigator.postLoginTarget)
    }

    @Test
    fun `signed in protected navigation reaches destination normally`() = runTest(dispatcher) {
        val authRepository = FakeAuthRepository(AuthSession.SignedIn(testUser()))
        val navigator = createNavigator(authRepository = authRepository)
        navigator.clearAndGoTo(Screen.Home)

        navigator.navigate(Screen.Chatbot)

        assertEquals(listOf(Screen.MainShell, Screen.Chatbot), navigator.backStack)
        assertEquals(listOf(Screen.Home), navigator.mainShellBackStack)
    }

    @Test
    fun `auth success after protected redirect lands on target once`() = runTest(dispatcher) {
        val authRepository = FakeAuthRepository()
        val navigator = createNavigator(authRepository = authRepository)
        navigator.clearAndGoTo(Screen.Home)
        navigator.navigate(Screen.Chatbot)

        authRepository.setSession(AuthSession.SignedIn(testUser()))
        advanceUntilIdle()
        navigator.onAuthSuccess()

        assertEquals(listOf(Screen.MainShell, Screen.Chatbot), navigator.backStack)
        assertEquals(listOf(Screen.Home), navigator.mainShellBackStack)
        assertEquals(null, navigator.postLoginTarget)
    }

    @Test
    fun `guest navigation to medical records redirects through login`() = runTest(dispatcher) {
        val navigator = createNavigator()
        navigator.clearAndGoTo(Screen.Home)

        navigator.navigate(Screen.MedicalRecords)

        assertEquals(listOf(Screen.MainShell, Screen.Login), navigator.backStack)
        assertEquals(Screen.MedicalRecords, navigator.postLoginTarget)
    }

    @Test
    fun `sign out prunes protected destinations from both stacks`() = runTest(dispatcher) {
        val authRepository = FakeAuthRepository(AuthSession.SignedIn(testUser()))
        val navigator = createNavigator(authRepository = authRepository)
        navigator.clearAndGoTo(Screen.Booking)
        navigator.navigate(Screen.Chatbot)
        navigator.navigateWithinShell(Screen.BookingHistory())

        authRepository.setSession(AuthSession.SignedOut)
        advanceUntilIdle()

        assertEquals(listOf(Screen.MainShell), navigator.backStack)
        assertEquals(listOf(Screen.Booking), navigator.mainShellBackStack)
        assertEquals(null, navigator.postLoginTarget)
    }

    @Test
    fun `sign out normalizes restored invalid protected stack to home plus login`() = runTest(dispatcher) {
        val authRepository = FakeAuthRepository(AuthSession.SignedOut)
        val navigator =
            createNavigator(
                authRepository = authRepository,
                savedStateHandle = SavedStateHandle(
                    mapOf(
                        "nav_stack" to listOf(
                            Json.encodeToString<Screen>(Screen.Chatbot),
                            Json.encodeToString<Screen>(Screen.Login),
                        ),
                    ),
                ),
            )

        advanceUntilIdle()

        assertEquals(listOf(Screen.MainShell, Screen.Login), navigator.backStack)
        assertEquals(listOf(Screen.Home), navigator.mainShellBackStack)
    }
}

private fun createNavigator(
    authRepository: FakeAuthRepository = FakeAuthRepository(),
    savedStateHandle: SavedStateHandle = SavedStateHandle(),
): RealNavigator = RealNavigator(savedStateHandle, authRepository)

private class FakeAuthRepository(initialSession: AuthSession = AuthSession.SignedOut) : AuthRepository {
    private val mutableSession = MutableStateFlow(initialSession)

    override val session: StateFlow<AuthSession> = mutableSession

    fun setSession(session: AuthSession) {
        mutableSession.value = session
    }

    override suspend fun login(email: String, password: String) = error("Not used in test")

    override suspend fun preregister(email: String, password: String) = error("Not used in test")

    override suspend fun completeProfile(
        firstName: String,
        lastName: String,
        gender: com.group8.comp2300.domain.model.user.Gender,
        sexualOrientation: com.group8.comp2300.domain.model.user.SexualOrientation,
        dateOfBirth: LocalDate?,
    ) = error("Not used in test")

    override suspend fun activateAccount(token: String) = error("Not used in test")

    override suspend fun forgotPassword(email: String) = error("Not used in test")

    override suspend fun resendVerificationEmail(email: String) = error("Not used in test")

    override suspend fun resetPassword(token: String, newPassword: String) = error("Not used in test")

    override suspend fun logout() = error("Not used in test")
}

private fun testUser() = User(
    id = "user-1",
    firstName = "Test",
    lastName = "User",
    email = "test@example.com",
)
