package com.group8.comp2300.service.auth

import com.auth0.jwt.JWT
import com.group8.comp2300.core.PasswordValidationResult
import com.group8.comp2300.core.Validation
import com.group8.comp2300.domain.model.user.User
import com.group8.comp2300.domain.repository.PasswordResetTokenRepository
import com.group8.comp2300.domain.repository.RefreshTokenRepository
import com.group8.comp2300.domain.repository.UserRepository
import com.group8.comp2300.dto.AuthResponse
import com.group8.comp2300.dto.CompleteProfileRequest
import com.group8.comp2300.dto.LoginRequest
import com.group8.comp2300.dto.PreregisterRequest
import com.group8.comp2300.dto.PreregisterResponse
import com.group8.comp2300.dto.RegisterRequest
import com.group8.comp2300.dto.TokenResponse
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
import kotlin.time.Duration.Companion.minutes

class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordResetTokenRepository: PasswordResetTokenRepository,
    private val jwtService: JwtService,
    private val emailService: EmailService?,
) {
    private val passwordHasher = PasswordHasher
    private val secureRandom = SecureRandom()

    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun register(request: RegisterRequest): Result<AuthResponse> {
        val validationResult = validateRegisterRequest(request)
        if (validationResult is ValidationResult.Invalid) {
            return Result.failure(IllegalArgumentException(validationResult.message))
        }

        if (userRepository.existsByEmail(request.email)) {
            return Result.failure(IllegalArgumentException("An account with this email already exists"))
        }

        return try {
            val passwordHash = passwordHasher.hash(request.password)
            val userId = generateUserId()

            userRepository.insert(
                id = userId,
                email = request.email,
                passwordHash = passwordHash,
                firstName = request.firstName,
                lastName = request.lastName,
                phone = request.phone,
                dateOfBirth = request.dateOfBirth,
                gender = request.gender,
                sexualOrientation = request.sexualOrientation,
                preferredLanguage = "en",
            )

            val user = userRepository.findById(userId)
                ?: return Result.failure(IllegalStateException("Failed to retrieve newly created user"))

            // Send activation email
            sendActivationEmail(userId, request.email)

            val tokens = generateTokenPair(userId)

            Result.success(
                AuthResponse(
                    user = user,
                    accessToken = tokens.accessToken,
                    refreshToken = tokens.refreshToken,
                ),
            )
        } catch (e: Exception) {
            if (e.isDuplicateEmailViolation()) {
                return Result.failure(IllegalArgumentException("An account with this email already exists"))
            }
            Result.failure(e)
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

        // Token rotation: revoke old, issue new
        refreshTokenRepository.revoke(tokenHash)

        val newTokens = generateTokenPair(userId)

        // Periodic cleanup
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
        // Rate limiting check
        if (!userRepository.canRequestVerification(email)) {
            return Result.failure(IllegalArgumentException("Please wait before requesting another password reset"))
        }

        val user = userRepository.findByEmail(email)
        if (user == null) {
            // Add delay to normalize response times and prevent timing attacks
            delay(TIMING_ATTACK_DELAY_MS)
            // Return success even if user not found to prevent email enumeration
            return Result.success(Unit)
        }

        // Record the verification request for rate limiting
        userRepository.recordVerificationRequest(email)

        val token = generateVerificationCode()
        val tokenHash = hashToken(token)
        passwordResetTokenRepository.insert(tokenHash, user.id)

        when (val result = emailService?.sendPasswordResetEmail(email, token)) {
            is EmailResult.Success -> {
                log.debug("Password reset email sent successfully to {} (messageId={})", email, result.messageId)
            }

            is EmailResult.Failure -> {
                log.warn("Failed to send password reset email to {}: {}", email, result.error.message)
                // Token is stored, user can retry
            }

            null -> {
                log.debug("Email service not configured - skipping password reset email for {}", email)
            }
        }

        return Result.success(Unit)
    }

    fun resetPassword(token: String, newPassword: String): Result<Unit> {
        val passwordResult = Validation.validatePassword(newPassword)
        when (passwordResult) {
            PasswordValidationResult.TooShort ->
                return Result.failure(IllegalArgumentException("Password must be at least 8 characters"))

            PasswordValidationResult.MissingDigit ->
                return Result.failure(IllegalArgumentException("Password must contain at least one number"))

            PasswordValidationResult.MissingLetter ->
                return Result.failure(IllegalArgumentException("Password must contain at least one letter"))

            PasswordValidationResult.TooLong ->
                return Result.failure(IllegalArgumentException("Password must be 72 bytes or fewer"))

            PasswordValidationResult.Valid -> { /* continue */ }
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

    suspend fun preregister(request: PreregisterRequest): Result<PreregisterResponse> {
        // Validate email and password
        if (request.email.isBlank() || !Validation.isValidEmail(request.email)) {
            return Result.failure(IllegalArgumentException("Invalid email format"))
        }

        val passwordResult = Validation.validatePassword(request.password)
        val passwordError = when (passwordResult) {
            PasswordValidationResult.TooShort -> "Password must be at least 8 characters"
            PasswordValidationResult.MissingDigit -> "Password must contain at least one number"
            PasswordValidationResult.MissingLetter -> "Password must contain at least one letter"
            PasswordValidationResult.TooLong -> "Password must be 72 bytes or fewer"
            PasswordValidationResult.Valid -> null
        }

        if (passwordError != null) {
            return Result.failure(IllegalArgumentException(passwordError))
        }

        // Check for existing email before rate limiting (to maintain existing error behavior)
        if (userRepository.existsByEmail(request.email)) {
            return Result.failure(IllegalArgumentException("An account with this email already exists"))
        }

        // Rate limiting check
        if (!userRepository.canRequestVerification(request.email)) {
            return Result.failure(IllegalArgumentException("Please wait before requesting another verification email"))
        }

        return try {
            val passwordHash = passwordHasher.hash(request.password)
            val userId = generateUserId()

            // Create user with placeholder profile data (inactive until profile completed)
            userRepository.insert(
                id = userId,
                email = request.email,
                passwordHash = passwordHash,
                firstName = "", // Placeholder - will be filled during complete profile
                lastName = "", // Placeholder - will be filled during complete profile
                phone = null,
                dateOfBirth = null,
                gender = null,
                sexualOrientation = null,
                preferredLanguage = "en",
            )

            // Record the verification request for rate limiting
            userRepository.recordVerificationRequest(request.email)

            // Send verification email
            sendActivationEmail(userId, request.email)

            Result.success(
                PreregisterResponse(
                    email = request.email,
                    message = "Verification email sent. Please check your inbox.",
                ),
            )
        } catch (e: Exception) {
            if (e.isDuplicateEmailViolation()) {
                return Result.failure(IllegalArgumentException("An account with this email already exists"))
            }
            Result.failure(e)
        }
    }

    fun completeProfile(userId: String, request: CompleteProfileRequest): Result<User> {
        // Validate required fields
        if (request.firstName.isBlank() || request.lastName.isBlank()) {
            return Result.failure(IllegalArgumentException("First name and last name are required"))
        }

        val user = userRepository.findById(userId)
            ?: return Result.failure(IllegalArgumentException("User not found"))

        // Ensure the account is activated before allowing profile completion
        if (!userRepository.isActivated(userId)) {
            return Result.failure(
                IllegalArgumentException("Please activate your account before completing your profile"),
            )
        }

        return try {
            userRepository.updateProfile(
                userId = userId,
                firstName = request.firstName,
                lastName = request.lastName,
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

    private fun generateTokenPair(userId: String): TokenPair {
        val accessToken = jwtService.generateAccessToken(userId)
        val refreshToken = jwtService.generateRefreshToken(userId)
        val tokenHash = hashToken(refreshToken)

        refreshTokenRepository.insert(tokenHash, userId)

        return TokenPair(accessToken, refreshToken)
    }

    private fun generateUserId(): String = "user_${java.util.UUID.randomUUID()}"

    /**
     * Generates a 6-digit verification code (100000-999999)
     */
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
                // Token is stored, user can retry
            }

            null -> {
                log.debug("Email service not configured - skipping activation email for {}", email)
            }
        }
    }

    private fun cleanExpiredTokens() {
        val cutoff = Clock.System.now() - 90.days
        refreshTokenRepository.deleteExpired(cutoff.toEpochMilliseconds())
        // Also clean up unactivated accounts older than 24 hours
        val unactivatedCutoff = Clock.System.now() - UNACTIVATED_ACCOUNT_MAX_AGE
        userRepository.deleteUnactivatedAccounts(unactivatedCutoff.toEpochMilliseconds())
        passwordResetTokenRepository.deleteExpired(cutoff.toEpochMilliseconds())
    }

    private fun validateRegisterRequest(request: RegisterRequest): ValidationResult {
        val passwordResult = Validation.validatePassword(request.password)
        when {
            request.email.isBlank() || !Validation.isValidEmail(request.email) ->
                return ValidationResult.Invalid("Invalid email format")

            passwordResult == PasswordValidationResult.TooShort ->
                return ValidationResult.Invalid("Password must be at least 8 characters")

            passwordResult == PasswordValidationResult.MissingDigit ->
                return ValidationResult.Invalid("Password must contain at least one number")

            passwordResult == PasswordValidationResult.MissingLetter ->
                return ValidationResult.Invalid("Password must contain at least one letter")

            passwordResult == PasswordValidationResult.TooLong ->
                return ValidationResult.Invalid("Password must be 72 bytes or fewer")

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
