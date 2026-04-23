package com.group8.comp2300.data.repository

import co.touchlab.kermit.Logger
import com.group8.comp2300.data.auth.TokenManager
import com.group8.comp2300.data.auth.extractJwtExpiration
import com.group8.comp2300.data.local.PersonalDataCleaner
import com.group8.comp2300.data.local.SessionDataSource
import com.group8.comp2300.data.offline.isRetryable
import com.group8.comp2300.data.remote.ApiException
import com.group8.comp2300.data.remote.ApiService
import com.group8.comp2300.data.remote.dto.CompleteProfileRequest
import com.group8.comp2300.data.remote.dto.LoginRequest
import com.group8.comp2300.data.remote.dto.PreregisterRequest
import com.group8.comp2300.domain.model.session.AuthSession
import com.group8.comp2300.domain.model.user.Gender
import com.group8.comp2300.domain.model.user.SexualOrientation
import com.group8.comp2300.domain.model.user.User
import com.group8.comp2300.domain.repository.AuthRepository
import com.group8.comp2300.domain.repository.medical.OfflineSyncCoordinator
import com.group8.comp2300.util.toEpochMilliseconds
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalDate

class AuthRepositoryImpl(
    private val apiService: ApiService,
    private val tokenManager: TokenManager,
    private val sessionDataSource: SessionDataSource,
    private val personalDataCleaner: PersonalDataCleaner,
    private val offlineSyncCoordinator: OfflineSyncCoordinator,
) : AuthRepository {
    private val repositoryScope = CoroutineScope(Dispatchers.IO + Job())
    private val logger = Logger.withTag("AuthRepository")
    private val initialRestoreCompleted = CompletableDeferred<Unit>()
    private val cachedUserId = sessionDataSource.currentSession()?.userId

    private val _session = MutableStateFlow<AuthSession>(
        cachedUserId?.let { AuthSession.SignedIn(staleUser(it), isStale = true) } ?: AuthSession.SignedOut,
    )
    override val session: StateFlow<AuthSession> = _session.asStateFlow()

    init {
        if (cachedUserId == null) {
            initialRestoreCompleted.complete(Unit)
        } else {
            tryRestoreSession(cachedUserId)
        }
    }

    override suspend fun login(email: String, password: String): Result<User> = runCatching {
        val response = apiService.login(LoginRequest(email, password))
        establishAuthenticatedSession(response.user.id, response.accessToken, response.refreshToken)
    }

    override suspend fun preregister(email: String, password: String): Result<String> = runCatching {
        apiService.preregister(PreregisterRequest(email, password)).email
    }

    override suspend fun completeProfile(
        firstName: String,
        lastName: String,
        gender: Gender,
        sexualOrientation: SexualOrientation,
        dateOfBirth: LocalDate?,
    ): Result<User> = runCatching {
        val user = apiService.completeProfile(
            CompleteProfileRequest(
                firstName = firstName,
                lastName = lastName,
                dateOfBirth = dateOfBirth.toEpochMilliseconds(),
                gender = gender.name,
                sexualOrientation = sexualOrientation.name,
            ),
        )
        _session.value = AuthSession.SignedIn(user)
        user
    }

    override suspend fun activateAccount(token: String): Result<Unit> = runCatching {
        val response = apiService.activateAccount(token)
        establishAuthenticatedSession(response.user.id, response.accessToken, response.refreshToken)
        Unit
    }

    override suspend fun forgotPassword(email: String): Result<Unit> = runCatching {
        apiService.forgotPassword(email)
        Unit
    }

    override suspend fun resendVerificationEmail(email: String): Result<Unit> = runCatching {
        apiService.resendVerificationEmail(email)
        Unit
    }

    override suspend fun resetPassword(token: String, newPassword: String): Result<Unit> = runCatching {
        apiService.resetPassword(token, newPassword)
        Unit
    }

    override suspend fun logout() {
        try {
            if (session.value is AuthSession.SignedIn) {
                apiService.logout()
            }
        } catch (e: Exception) {
            logger.w(e) { "Logout API call failed, clearing local state anyway" }
        } finally {
            tokenManager.clearTokens()
            personalDataCleaner.clearAllPersonalData()
            _session.value = AuthSession.SignedOut
        }
    }

    internal suspend fun awaitInitialRestore() {
        initialRestoreCompleted.await()
    }

    private fun tryRestoreSession(storedUserId: String) {
        repositoryScope.launch {
            try {
                val user = apiService.getProfile()
                synchronizeAuthenticatedData(user)
            } catch (e: Exception) {
                if (e.isRetryable()) {
                    logger.w(e) { "Session restore failed due to transient issue" }
                    _session.value = AuthSession.SignedIn(staleUser(storedUserId), isStale = true)
                } else {
                    logger.w(e) { "Session restore failed; clearing local authenticated state" }
                    tokenManager.clearTokens()
                    personalDataCleaner.clearAllPersonalData()
                    _session.value = AuthSession.SignedOut
                }
            } finally {
                initialRestoreCompleted.complete(Unit)
            }
        }
    }

    private suspend fun establishAuthenticatedSession(userId: String, accessToken: String, refreshToken: String): User {
        val expiresAt = extractJwtExpiration(accessToken) ?: 0L
        tokenManager.saveTokens(userId, accessToken, refreshToken, expiresAt)
        val user = apiService.getProfile()
        synchronizeAuthenticatedData(user)
        return user
    }

    private suspend fun synchronizeAuthenticatedData(user: User) {
        _session.value = AuthSession.SignedIn(user)
        try {
            offlineSyncCoordinator.syncNow()
            _session.value = AuthSession.SignedIn(user)
        } catch (e: Exception) {
            if (e.isAuthenticationFailure()) {
                throw e
            }
            logger.w(e) { "Authenticated sync failed; keeping cached signed-in state" }
            _session.value = AuthSession.SignedIn(user, isStale = true)
        }
    }

    private fun staleUser(userId: String): User = User(
        id = userId,
        firstName = "",
        lastName = "",
        email = "",
    )

    private fun Throwable.isAuthenticationFailure(): Boolean = this is ApiException && statusCode == 401
}
