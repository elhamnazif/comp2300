package com.group8.comp2300.data.repository

import com.group8.comp2300.data.auth.TokenManager
import com.group8.comp2300.data.remote.ApiService
import com.group8.comp2300.data.remote.dto.LoginRequest
import com.group8.comp2300.data.remote.dto.RegisterRequest
import com.group8.comp2300.domain.model.user.Gender
import com.group8.comp2300.domain.model.user.SexualOrientation
import com.group8.comp2300.domain.model.user.User
import com.group8.comp2300.domain.repository.AuthRepository
import kotlin.time.Duration.Companion.minutes
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

class AuthRepositoryImpl(private val apiService: ApiService, private val tokenManager: TokenManager) : AuthRepository {
    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val repositoryScope = CoroutineScope(Dispatchers.IO + Job())

    init {
        // Auto-login: restore session from stored tokens on init
        // We do this in a non-blocking way - if tokens exist, we try to fetch the profile
        // If it fails, the user will need to login again
        tryRestoreSession()
    }

    override suspend fun login(email: String, password: String): Result<User> = try {
        val response = apiService.login(LoginRequest(email, password))
        val expiresAt = Clock.System.now().toEpochMilliseconds() + ACCESS_TOKEN_EXPIRATION_MS
        tokenManager.saveTokens(response.user.id, response.accessToken, response.refreshToken, expiresAt)
        _currentUser.value = response.user
        Result.success(response.user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        gender: Gender,
        sexualOrientation: SexualOrientation,
        dateOfBirth: LocalDate?
    ): Result<User> = try {
        val request = RegisterRequest(
            email = email,
            password = password,
            firstName = firstName,
            lastName = lastName,
            phone = null,
            gender = gender.name,
            sexualOrientation = sexualOrientation.name,
            dateOfBirth = dateOfBirth?.toEpochDays()?.let { epochDays ->
                // Convert LocalDate epoch days to milliseconds
                epochDays * 24L * 60L * 60L * 1000L
            }
        )
        val response = apiService.register(request)
        val expiresAt = Clock.System.now().toEpochMilliseconds() + ACCESS_TOKEN_EXPIRATION_MS
        tokenManager.saveTokens(response.user.id, response.accessToken, response.refreshToken, expiresAt)
        _currentUser.value = response.user
        Result.success(response.user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun logout() {
        try {
            apiService.logout()
        } catch (e: Exception) {
            // Ignore logout errors, just clear local session
        } finally {
            tokenManager.clearTokens()
            _currentUser.value = null
        }
    }

    override fun isGuest(): Boolean = _currentUser.value == null

    private fun tryRestoreSession() {
        repositoryScope.launch {
            try {
                val userId = tokenManager.getUserId() ?: return@launch

                // Check if token is expired before attempting to fetch profile
                if (tokenManager.isTokenExpired()) {
                    tokenManager.clearTokens()
                    return@launch
                }

                // Fetch the user's profile from the server using the stored token
                val user = apiService.getProfile()
                _currentUser.value = user
            } catch (e: Exception) {
                // Session restore failed - tokens might be expired or invalid
                // Clear tokens so user needs to login again
                tokenManager.clearTokens()
            }
        }
    }

    private companion object {
        val ACCESS_TOKEN_EXPIRATION = 15.minutes
        val ACCESS_TOKEN_EXPIRATION_MS = ACCESS_TOKEN_EXPIRATION.inWholeMilliseconds
    }
}
