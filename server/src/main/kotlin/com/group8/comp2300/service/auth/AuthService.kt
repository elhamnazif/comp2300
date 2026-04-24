package com.group8.comp2300.service.auth

import com.auth0.jwt.JWT
import com.group8.comp2300.core.PasswordValidationResult
import com.group8.comp2300.core.Validation
import com.group8.comp2300.domain.model.user.User
import com.group8.comp2300.domain.repository.EmailChangeTokenRepository
import com.group8.comp2300.domain.repository.PasswordResetTokenRepository
import com.group8.comp2300.domain.repository.RefreshTokenRepository
import com.group8.comp2300.domain.repository.UserRepository
import com.group8.comp2300.dto.*
import com.group8.comp2300.security.JwtService
import com.group8.comp2300.security.PasswordHasher
import com.group8.comp2300.service.email.EmailResult
import com.group8.comp2300.service.email.EmailService
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordResetTokenRepository: PasswordResetTokenRepository,
    private val emailChangeTokenRepository: EmailChangeTokenRepository,
    private val jwtService: JwtService,
    private val emailService: EmailService?,
    private val verificationRequestThrottle: VerificationRequestThrottle,
    private val profileImageStorage: ProfileImageStorage,
) {
    private val passwordHasher = PasswordHasher
    private val secureRandom = SecureRandom()

    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun register(request: RegisterRequest): Result<AuthResponse> {
        val validationResult = validateRegisterRequest(request)
        if (validationResult is ValidationResult.Invalid) {
            return Result.failure(IllegalArgumentException(validationResult.message))
        }

        return runWithDuplicateEmailHandling {
            prepareFreshAccountEmail(request.email)
            val userId = createUser(
                email = request.email,
                password = request.password,
                firstName = request.firstName,
                lastName = request.lastName,
                phone = request.phone,
                dateOfBirth = request.dateOfBirth,
                gender = request.gender,
                sexualOrientation = request.sexualOrientation,
            )

            val user = userRepository.findById(userId)
                ?: return Result.failure(IllegalStateException("Failed to retrieve newly created user"))

            sendActivationEmail(userId, request.email)
            val tokens = generateTokenPair(userId)

            Result.success(
                AuthResponse(
                    user = user,
                    accessToken = tokens.accessToken,
                    refreshToken = tokens.refreshToken,
                ),
            )
        }
    }

    fun login(request: LoginRequest): Result<AuthResponse> = try {
        val user = userRepository.findByEmail(request.email)
            ?: return Result.failure(IllegalArgumentException("Invalid email or password"))

        val passwordHash = userRepository.getPasswordHash(request.email)
            ?: return Result.failure(IllegalArgumentException("Invalid email or password"))

        if (!passwordHasher.verify(request.password, passwordHash)) {
            return Result.failure(IllegalArgumentException("Invalid email or password"))
        }

        if (userRepository.isDeactivated(user.id)) {
            return Result.failure(IllegalArgumentException("Account is deactivated"))
        }

        if (!userRepository.isActivated(user.id)) {
            return Result.failure(IllegalArgumentException("Please activate your account before logging in"))
        }

        val tokens = generateTokenPair(user.id)

        Result.success(
            AuthResponse(
                user = user,
                accessToken = tokens.accessToken,
                refreshToken = tokens.refreshToken,
            ),
        )
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun refreshToken(refreshToken: String): Result<TokenResponse> {
        val jwt = try {
            JWT.decode(refreshToken)
        } catch (_: Exception) {
            return Result.failure(IllegalArgumentException("Invalid refresh token"))
        }

        val userId = jwt.subject
        val tokenType = jwt.getClaim("type").asString()

        if (tokenType != "refresh") {
            return Result.failure(IllegalArgumentException("Invalid token type"))
        }

        val verifier = jwtService.refreshVerifier
        try {
            verifier.verify(refreshToken)
        } catch (_: Exception) {
            return Result.failure(IllegalArgumentException("Refresh token is expired or invalid"))
        }

        val tokenHash = hashToken(refreshToken)
        val tokenUserId = refreshTokenRepository.findValid(tokenHash)
            ?: return Result.failure(IllegalArgumentException("Refresh token is invalid or has been revoked"))

        if (tokenUserId != userId) {
            return Result.failure(IllegalArgumentException("Token does not belong to the specified user"))
        }

        if (!userRepository.isActive(userId)) {
            refreshTokenRepository.revoke(tokenHash)
            return Result.failure(IllegalArgumentException("Account is deactivated"))
        }

        refreshTokenRepository.revoke(tokenHash)

        val newTokens = generateTokenPair(userId)
        cleanExpiredTokens()

        return Result.success(
            TokenResponse(
                accessToken = newTokens.accessToken,
                refreshToken = newTokens.refreshToken,
            ),
        )
    }

    fun logout(userId: String) {
        refreshTokenRepository.revokeAllForUser(userId)
    }

    fun getUserById(userId: String): User? = userRepository.findById(userId)

    fun activateAccount(token: String): Result<AuthResponse> {
        val tokenHash = hashToken(token)
        val userId = passwordResetTokenRepository.findValid(tokenHash)
            ?: return Result.failure(IllegalArgumentException("Invalid or expired activation token"))

        if (userRepository.isActivated(userId)) {
            return Result.failure(IllegalArgumentException("Account is already activated"))
        }

        userRepository.activateUser(userId)
        passwordResetTokenRepository.markUsed(tokenHash)

        val user = userRepository.findById(userId)
            ?: return Result.failure(IllegalStateException("Failed to retrieve user after activation"))

        val tokens = generateTokenPair(userId)

        return Result.success(
            AuthResponse(
                user = user,
                accessToken = tokens.accessToken,
                refreshToken = tokens.refreshToken,
            ),
        )
    }

    suspend fun forgotPassword(email: String): Result<Unit> {
        if (!verificationRequestThrottle.canRequest(email)) {
            return Result.failure(IllegalArgumentException("Please wait before requesting another password reset"))
        }

        val user = userRepository.findByEmail(email)
        if (user == null || userRepository.isDeactivated(user.id)) {
            delay(TIMING_ATTACK_DELAY_MS)
            return Result.success(Unit)
        }

        verificationRequestThrottle.recordRequest(email)

        val token = generateVerificationCode()
        val tokenHash = hashToken(token)
        passwordResetTokenRepository.insert(tokenHash, user.id)

        when (val result = emailService?.sendPasswordResetEmail(email, token)) {
            is EmailResult.Success -> {
                log.debug("Password reset email sent successfully to {} (messageId={})", email, result.messageId)
            }

            is EmailResult.Failure -> {
                log.warn("Failed to send password reset email to {}: {}", email, result.error.message)
            }

            null -> {
                log.debug("Email service not configured - skipping password reset email for {}", email)
            }
        }

        return Result.success(Unit)
    }

    suspend fun resendVerificationEmail(email: String): Result<Unit> {
        if (!verificationRequestThrottle.canRequest(email)) {
            return Result.failure(IllegalArgumentException("Please wait before requesting another verification email"))
        }

        val user = userRepository.findByEmail(email)
        if (user == null) {
            delay(TIMING_ATTACK_DELAY_MS)
            return Result.success(Unit)
        }

        if (userRepository.isActivated(user.id)) {
            return Result.failure(IllegalArgumentException("Account is already activated"))
        }

        verificationRequestThrottle.recordRequest(email)
        sendActivationEmail(user.id, email)

        return Result.success(Unit)
    }

    fun resetPassword(token: String, newPassword: String): Result<Unit> {
        validatePasswordMessage(newPassword)?.let { message ->
            return Result.failure(IllegalArgumentException(message))
        }

        val tokenHash = hashToken(token)
        val userId = passwordResetTokenRepository.findValid(tokenHash)
            ?: return Result.failure(IllegalArgumentException("Invalid or expired reset token"))

        val newHash = passwordHasher.hash(newPassword)
        userRepository.updatePasswordHash(userId, newHash)
        passwordResetTokenRepository.markUsed(tokenHash)
        refreshTokenRepository.revokeAllForUser(userId)

        return Result.success(Unit)
    }

    fun changePassword(userId: String, currentPassword: String, newPassword: String): Result<Unit> {
        val user = requireMutableAccountUser(userId)
            ?: return Result.failure(IllegalArgumentException("User not found"))

        validatePasswordMessage(newPassword)?.let { message ->
            return Result.failure(IllegalArgumentException(message))
        }

        val passwordHash = userRepository.getPasswordHash(user.email)
            ?: return Result.failure(IllegalArgumentException("Invalid password"))

        if (!passwordHasher.verify(currentPassword, passwordHash)) {
            return Result.failure(IllegalArgumentException("Invalid password"))
        }

        userRepository.updatePasswordHash(userId, passwordHasher.hash(newPassword))
        refreshTokenRepository.revokeAllForUser(userId)
        return Result.success(Unit)
    }

    suspend fun requestEmailChange(userId: String, currentPassword: String, newEmail: String): Result<Unit> {
        val user = requireMutableAccountUser(userId)
            ?: return Result.failure(IllegalArgumentException("User not found"))
        val normalizedEmail = newEmail.trim().lowercase()

        if (!Validation.isValidEmail(normalizedEmail)) {
            return Result.failure(IllegalArgumentException("Invalid email format"))
        }

        if (normalizedEmail == user.email.lowercase()) {
            return Result.failure(IllegalArgumentException("New email must be different"))
        }

        val passwordHash = userRepository.getPasswordHash(user.email)
            ?: return Result.failure(IllegalArgumentException("Invalid password"))
        if (!passwordHasher.verify(currentPassword, passwordHash)) {
            return Result.failure(IllegalArgumentException("Invalid password"))
        }

        val existingUser = userRepository.findByEmail(normalizedEmail)
        if (existingUser != null && existingUser.id != userId) {
            return Result.failure(IllegalArgumentException("An account with this email already exists"))
        }

        emailChangeTokenRepository.deleteByUserId(userId)
        val token = generateVerificationCode()
        emailChangeTokenRepository.insert(
            tokenHash = hashToken(token),
            userId = userId,
            newEmail = normalizedEmail,
        )

        when (val result = emailService?.sendEmailChangeEmail(normalizedEmail, token)) {
            is EmailResult.Success -> {
                log.debug("Email change verification sent successfully to {} (messageId={})", normalizedEmail, result.messageId)
            }

            is EmailResult.Failure -> {
                log.warn("Failed to send email change verification to {}: {}", normalizedEmail, result.error.message)
            }

            null -> {
                log.debug("Email service not configured - skipping email change verification for {}", normalizedEmail)
            }
        }

        return Result.success(Unit)
    }

    fun confirmEmailChange(code: String): Result<Unit> {
        val tokenHash = hashToken(code)
        val token = emailChangeTokenRepository.findValid(tokenHash)
            ?: return Result.failure(IllegalArgumentException("Invalid or expired verification code"))
        val user = requireMutableAccountUser(token.userId)
            ?: return Result.failure(IllegalArgumentException("User not found"))

        val existingUser = userRepository.findByEmail(token.newEmail)
        if (existingUser != null && existingUser.id != token.userId) {
            return Result.failure(IllegalArgumentException("An account with this email already exists"))
        }

        userRepository.updateEmail(token.userId, token.newEmail)
        emailChangeTokenRepository.markUsed(tokenHash)
        emailChangeTokenRepository.deleteByUserId(token.userId)
        refreshTokenRepository.revokeAllForUser(token.userId)

        return if (userRepository.findById(user.id)?.email == token.newEmail) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Failed to update email"))
        }
    }

    fun deactivateAccount(userId: String, currentPassword: String): Result<Unit> {
        val user = requireMutableAccountUser(userId)
            ?: return Result.failure(IllegalArgumentException("User not found"))
        val passwordHash = userRepository.getPasswordHash(user.email)
            ?: return Result.failure(IllegalArgumentException("Invalid password"))

        if (!passwordHasher.verify(currentPassword, passwordHash)) {
            return Result.failure(IllegalArgumentException("Invalid password"))
        }

        userRepository.deactivateUser(userId, Clock.System.now().toEpochMilliseconds())
        refreshTokenRepository.revokeAllForUser(userId)
        passwordResetTokenRepository.deleteByUserId(userId)
        emailChangeTokenRepository.deleteByUserId(userId)

        return Result.success(Unit)
    }

    suspend fun preregister(request: PreregisterRequest): Result<PreregisterResponse> {
        if (request.email.isBlank() || !Validation.isValidEmail(request.email)) {
            return Result.failure(IllegalArgumentException("Invalid email format"))
        }

        validatePasswordMessage(request.password)?.let { message ->
            return Result.failure(IllegalArgumentException(message))
        }

        return runWithDuplicateEmailHandling {
            prepareFreshAccountEmail(request.email)
            if (!verificationRequestThrottle.canRequest(request.email)) {
                return Result.failure(
                    IllegalArgumentException("Please wait before requesting another verification email"),
                )
            }
            val userId = createUser(
                email = request.email,
                password = request.password,
                firstName = "",
                lastName = "",
                phone = null,
                dateOfBirth = null,
                gender = null,
                sexualOrientation = null,
            )

            verificationRequestThrottle.recordRequest(request.email)
            sendActivationEmail(userId, request.email)

            Result.success(
                PreregisterResponse(
                    email = request.email,
                    message = "Verification email sent. Please check your inbox.",
                ),
            )
        }
    }

    fun updateProfile(userId: String, request: UpdateProfileRequest): Result<User> {
        if (request.firstName.isBlank() || request.lastName.isBlank()) {
            return Result.failure(IllegalArgumentException("First name and last name are required"))
        }

        val user = userRepository.findById(userId)
            ?: return Result.failure(IllegalArgumentException("User not found"))

        if (!userRepository.isActivated(userId)) {
            return Result.failure(
                IllegalArgumentException("Please activate your account before updating your profile"),
            )
        }

        return try {
            userRepository.updateProfile(
                userId = userId,
                firstName = request.firstName,
                lastName = request.lastName,
                phone = request.phone?.trim().takeUnless { it.isNullOrEmpty() },
                dateOfBirth = request.dateOfBirth,
                gender = request.gender,
                sexualOrientation = request.sexualOrientation,
            )

            userRepository.findById(userId)
                ?.let { Result.success(it) }
                ?: Result.failure(IllegalStateException("Failed to retrieve updated user"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun uploadProfilePhoto(userId: String, fileName: String, fileBytes: ByteArray): Result<User> {
        val user = userRepository.findById(userId)
            ?: return Result.failure(IllegalArgumentException("User not found"))

        if (!userRepository.isActivated(userId)) {
            return Result.failure(IllegalArgumentException("Please activate your account before updating your profile"))
        }

        val previousImageUrl = user.profileImageUrl
        return profileImageStorage.save(userId, fileName, fileBytes).fold(
            onSuccess = { imageUrl -> persistProfilePhoto(userId, imageUrl, previousImageUrl) },
            onFailure = { error -> Result.failure(error) },
        )
    }

    fun removeProfilePhoto(userId: String): Result<User> {
        val user = userRepository.findById(userId)
            ?: return Result.failure(IllegalArgumentException("User not found"))

        if (!userRepository.isActivated(userId)) {
            return Result.failure(IllegalArgumentException("Please activate your account before updating your profile"))
        }

        return persistProfilePhoto(userId, imageUrl = null, previousImageUrl = user.profileImageUrl)
    }

    private fun persistProfilePhoto(userId: String, imageUrl: String?, previousImageUrl: String?): Result<User> {
        val persistResult = runCatching {
            userRepository.updateProfileImageUrl(userId, imageUrl)
        }
        if (persistResult.isFailure) {
            imageUrl?.let(::cleanupStagedProfilePhoto)
            return Result.failure(persistResult.exceptionOrNull()!!)
        }

        val updatedUser = userRepository.findById(userId)
        if (updatedUser == null) {
            rollbackProfilePhotoUpdate(userId, previousImageUrl, imageUrl)
            return Result.failure(IllegalStateException("Failed to retrieve updated user"))
        }

        if (previousImageUrl != imageUrl) {
            runCatching {
                profileImageStorage.deleteByUrl(previousImageUrl)
            }.onFailure { error ->
                log.warn("Failed to delete previous profile photo for {}", userId, error)
            }
        }

        return Result.success(updatedUser)
    }

    private fun rollbackProfilePhotoUpdate(userId: String, previousImageUrl: String?, stagedImageUrl: String?) {
        runCatching {
            userRepository.updateProfileImageUrl(userId, previousImageUrl)
        }.onFailure { error ->
            log.warn("Failed to rollback profile photo update for {}", userId, error)
        }

        stagedImageUrl?.let(::cleanupStagedProfilePhoto)
    }

    private fun cleanupStagedProfilePhoto(imageUrl: String) {
        runCatching {
            profileImageStorage.deleteByUrl(imageUrl)
        }.onFailure { error ->
            log.warn("Failed to clean up staged profile photo {}", imageUrl, error)
        }
    }

    private fun generateTokenPair(userId: String): TokenPair {
        val accessToken = jwtService.generateAccessToken(userId)
        val refreshToken = jwtService.generateRefreshToken(userId)
        val tokenHash = hashToken(refreshToken)

        refreshTokenRepository.insert(tokenHash, userId)

        return TokenPair(accessToken, refreshToken)
    }

    private fun prepareFreshAccountEmail(email: String) {
        ensureNoActivatedAccount(email)
        deleteUnverifiedAccount(email)
    }

    private fun ensureNoActivatedAccount(email: String) {
        val existingUser = userRepository.findByEmail(email)
        if (existingUser != null && (userRepository.isActivated(existingUser.id) || userRepository.isDeactivated(existingUser.id))) {
            throw IllegalArgumentException("An account with this email already exists")
        }
    }

    private fun deleteUnverifiedAccount(email: String) {
        userRepository.findByEmailAndNotActivated(email)?.let { unverifiedUser ->
            passwordResetTokenRepository.deleteByUserId(unverifiedUser.id)
            userRepository.deleteById(unverifiedUser.id)
            verificationRequestThrottle.clearRequest(email)
        }
    }

    private fun createUser(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        phone: String?,
        dateOfBirth: Long?,
        gender: String?,
        sexualOrientation: String?,
    ): String {
        val userId = generateUserId()
        userRepository.insert(
            id = userId,
            email = email,
            passwordHash = passwordHasher.hash(password),
            firstName = firstName,
            lastName = lastName,
            phone = phone,
            dateOfBirth = dateOfBirth,
            gender = gender,
            sexualOrientation = sexualOrientation,
            preferredLanguage = "en",
        )
        return userId
    }

    private fun validatePasswordMessage(password: String): String? = when (Validation.validatePassword(password)) {
        PasswordValidationResult.TooShort -> "Password must be at least 8 characters"
        PasswordValidationResult.MissingDigit -> "Password must contain at least one number"
        PasswordValidationResult.MissingLetter -> "Password must contain at least one letter"
        PasswordValidationResult.TooLong -> "Password must be 72 bytes or fewer"
        PasswordValidationResult.Valid -> null
    }

    private fun generateUserId(): String = "user_${java.util.UUID.randomUUID()}"

    private fun generateVerificationCode(): String {
        val code = secureRandom.nextInt(VERIFICATION_CODE_RANGE) + VERIFICATION_CODE_MIN
        return code.toString()
    }

    private fun hashToken(token: String): String = MessageDigest.getInstance("SHA-256")
        .digest(token.toByteArray())
        .joinToString(separator = "") { byte -> "%02x".format(byte) }

    private suspend fun sendActivationEmail(userId: String, email: String) {
        val token = generateVerificationCode()
        val tokenHash = hashToken(token)
        passwordResetTokenRepository.insert(tokenHash, userId)

        when (val result = emailService?.sendActivationEmail(email, token)) {
            is EmailResult.Success -> {
                log.debug("Activation email sent successfully to {} (messageId={})", email, result.messageId)
            }

            is EmailResult.Failure -> {
                log.warn("Failed to send activation email to {}: {}", email, result.error.message)
            }

            null -> {
                log.debug("Email service not configured - skipping activation email for {}", email)
            }
        }
    }

    private fun cleanExpiredTokens() {
        val cutoff = Clock.System.now() - 90.days
        refreshTokenRepository.deleteExpired(cutoff.toEpochMilliseconds())
        val unactivatedCutoff = Clock.System.now() - UNACTIVATED_ACCOUNT_MAX_AGE
        userRepository.deleteUnactivatedAccounts(unactivatedCutoff.toEpochMilliseconds())
        passwordResetTokenRepository.deleteExpired(Clock.System.now().toEpochMilliseconds())
        emailChangeTokenRepository.deleteExpired(Clock.System.now().toEpochMilliseconds())
    }

    private fun requireMutableAccountUser(userId: String): User? {
        val user = userRepository.findById(userId) ?: return null
        return when {
            userRepository.isDeactivated(userId) -> null
            !userRepository.isActivated(userId) -> null
            else -> user
        }
    }

    private inline fun <T> runWithDuplicateEmailHandling(block: () -> Result<T>): Result<T> = try {
        block()
    } catch (e: Exception) {
        if (e.isDuplicateEmailViolation()) {
            Result.failure(IllegalArgumentException("An account with this email already exists"))
        } else {
            Result.failure(e)
        }
    }

    private fun validateRegisterRequest(request: RegisterRequest): ValidationResult {
        val passwordMessage = validatePasswordMessage(request.password)
        when {
            request.email.isBlank() || !Validation.isValidEmail(request.email) ->
                return ValidationResult.Invalid("Invalid email format")

            passwordMessage != null ->
                return ValidationResult.Invalid(passwordMessage)

            request.firstName.isBlank() || request.lastName.isBlank() ->
                return ValidationResult.Invalid("Name fields cannot be blank")
        }
        return ValidationResult.Valid
    }

    private fun Throwable.isDuplicateEmailViolation(): Boolean {
        var current: Throwable? = this
        while (current != null) {
            val message = current.message.orEmpty()
            if (
                message.contains("UNIQUE constraint failed", ignoreCase = true) &&
                message.contains("UserEntity.email", ignoreCase = true)
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }

    private sealed class ValidationResult {
        data object Valid : ValidationResult()
        data class Invalid(val message: String) : ValidationResult()
    }

    private data class TokenPair(val accessToken: String, val refreshToken: String)

    private companion object {
        const val VERIFICATION_CODE_MIN = 100000
        const val VERIFICATION_CODE_MAX = 999999
        const val VERIFICATION_CODE_RANGE = VERIFICATION_CODE_MAX - VERIFICATION_CODE_MIN + 1
        const val TIMING_ATTACK_DELAY_MS = 500L
        val UNACTIVATED_ACCOUNT_MAX_AGE = 1.days
    }
}
