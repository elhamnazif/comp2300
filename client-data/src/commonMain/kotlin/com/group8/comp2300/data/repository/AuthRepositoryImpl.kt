package com.group8.comp2300.data.repository

import co.touchlab.kermit.Logger
import com.group8.comp2300.data.auth.TokenManager
import com.group8.comp2300.data.remote.ApiService
import com.group8.comp2300.data.remote.dto.CompleteProfileRequest
import com.group8.comp2300.data.remote.dto.LoginRequest
import com.group8.comp2300.data.remote.dto.PreregisterRequest
import com.group8.comp2300.data.remote.dto.RegisterRequest
import com.group8.comp2300.domain.model.user.Gender
import com.group8.comp2300.domain.model.user.SexualOrientation
import com.group8.comp2300.domain.model.user.User
import com.group8.comp2300.domain.repository.AuthRepository
import com.group8.comp2300.util.toEpochMilliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

class AuthRepositoryImpl(
    private val apiService: ApiService,
    private val tokenManager: TokenManager,
    private val logger: Logger,
) : AuthRepository {
    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val repositoryScope = CoroutineScope(Dispatchers.IO + Job())

    init {
        // Auto-login: restore session from stored tokens on init
        // We do this in a non-blocking way - if tokens exist, we try to fetch the profile
        // If it fails, the user will need to login again
        logger.d { "AuthRepository initialized" }
        tryRestoreSession()
    }

    override suspend fun login(email: String, password: String): Result<User> = try {
        logger.i { "Login attempt for email: $email" }
        val response = apiService.login(LoginRequest(email, password))
        val expiresAt = Clock.System.now().toEpochMilliseconds() + ACCESS_TOKEN_EXPIRATION_MS
        tokenManager.saveTokens(response.user.id, response.accessToken, response.refreshToken, expiresAt)
        _currentUser.value = response.user
        logger.i { "Login successful for user: ${response.user.id}" }
        Result.success(response.user)
    } catch (e: Exception) {
        logger.e(e) { "Login failed for email: $email" }
        Result.failure(e)
    }

    override suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        gender: Gender,
        sexualOrientation: SexualOrientation,
        dateOfBirth: LocalDate?,
    ): Result<User> = try {
        logger.i { "Registration attempt for email: $email, name: $firstName $lastName" }
        val request = RegisterRequest(
            email = email,
            password = password,
            firstName = firstName,
            lastName = lastName,
            phone = null,
            gender = gender.name,
            sexualOrientation = sexualOrientation.name,
            dateOfBirth = dateOfBirth.toEpochMilliseconds(),
        )
        val response = apiService.register(request)
        val expiresAt = Clock.System.now().toEpochMilliseconds() + ACCESS_TOKEN_EXPIRATION_MS
        tokenManager.saveTokens(response.user.id, response.accessToken, response.refreshToken, expiresAt)
        _currentUser.value = response.user
        logger.i { "Registration successful for user: ${response.user.id}" }
        Result.success(response.user)
    } catch (e: Exception) {
        logger.e(e) { "Registration failed for email: $email" }
        Result.failure(e)
    }

    override suspend fun logout() {
        try {
            logger.i { "Logout attempt for user: ${_currentUser.value?.id}" }
            apiService.logout()
            logger.i { "Logout successful" }
        } catch (e: Exception) {
            logger.w(e) { "Logout API call failed, clearing local session anyway" }
            // Ignore logout errors, just clear local session
        } finally {
            tokenManager.clearTokens()
            _currentUser.value = null
        }
    }

    override fun isGuest(): Boolean = _currentUser.value == null

    override suspend fun activateAccount(token: String): Result<Unit> = try {
        logger.i { "Account activation attempt with token" }
        val response = apiService.activateAccount(token)
        val expiresAt = Clock.System.now().toEpochMilliseconds() + ACCESS_TOKEN_EXPIRATION_MS
        tokenManager.saveTokens(response.user.id, response.accessToken, response.refreshToken, expiresAt)
        _currentUser.value = response.user
        logger.i { "Account activation successful for user: ${response.user.id}" }
        Result.success(Unit)
    } catch (e: Exception) {
        logger.e(e) { "Account activation failed" }
        Result.failure(e)
    }

    override suspend fun forgotPassword(email: String): Result<Unit> = try {
        logger.i { "Forgot password request for email: $email" }
        apiService.forgotPassword(email)
        logger.i { "Forgot password email sent successfully" }
        Result.success(Unit)
    } catch (e: Exception) {
        logger.e(e) { "Forgot password request failed for email: $email" }
        Result.failure(e)
    }

    override suspend fun resetPassword(token: String, newPassword: String): Result<Unit> = try {
        logger.i { "Password reset attempt with token" }
        apiService.resetPassword(token, newPassword)
        logger.i { "Password reset successful" }
        Result.success(Unit)
    } catch (e: Exception) {
        logger.e(e) { "Password reset failed" }
        Result.failure(e)
    }

    override suspend fun preregister(email: String, password: String): Result<String> = try {
        logger.i { "Preregistration attempt for email: $email" }
        val response = apiService.preregister(PreregisterRequest(email, password))
        logger.i { "Preregistration successful for email: $email" }
        Result.success(response.email)
    } catch (e: Exception) {
        logger.e(e) { "Preregistration failed for email: $email" }
        Result.failure(e)
    }

    override suspend fun completeProfile(
        firstName: String,
        lastName: String,
        gender: Gender,
        sexualOrientation: SexualOrientation,
        dateOfBirth: LocalDate?,
    ): Result<User> = try {
        logger.i { "Profile completion attempt for user" }
        val request = CompleteProfileRequest(
            firstName = firstName,
            lastName = lastName,
            dateOfBirth = dateOfBirth.toEpochMilliseconds(),
            gender = gender.name,
            sexualOrientation = sexualOrientation.name,
        )
        val user = apiService.completeProfile(request)
        _currentUser.value = user
        logger.i { "Profile completion successful for user: ${user.id}" }
        Result.success(user)
    } catch (e: Exception) {
        logger.e(e) { "Profile completion failed" }
        Result.failure(e)
    }

    private fun tryRestoreSession() {
        repositoryScope.launch {
            try {
                val userId = tokenManager.getUserId()
                logger.d { "Attempting to restore session for user: $userId" }
                if (userId == null) {
                    logger.d { "No stored user ID found, skipping session restore" }
                    return@launch
                }

                // Check if token is expired before attempting to fetch profile
                if (tokenManager.isTokenExpired()) {
                    logger.d { "Stored token is expired, clearing tokens" }
                    tokenManager.clearTokens()
                    return@launch
                }

                // Fetch the user's profile from the server using the stored token
                val user = apiService.getProfile()
                _currentUser.value = user
                logger.i { "Session restored successfully for user: ${user.id}" }
            } catch (e: Exception) {
                // Session restore failed - tokens might be expired or invalid
                // Clear tokens so user needs to login again
                logger.w(e) { "Session restore failed, clearing tokens" }
                tokenManager.clearTokens()
            }
        }
    }

    private companion object {
        val ACCESS_TOKEN_EXPIRATION = 15.minutes
        val ACCESS_TOKEN_EXPIRATION_MS = ACCESS_TOKEN_EXPIRATION.inWholeMilliseconds
    }
}
