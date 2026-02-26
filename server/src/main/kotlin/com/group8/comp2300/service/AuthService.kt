package com.group8.comp2300.service

import com.auth0.jwt.JWT
import com.group8.comp2300.core.PasswordValidationResult
import com.group8.comp2300.core.Validation
import com.group8.comp2300.data.repository.RefreshTokenRepository
import com.group8.comp2300.data.repository.UserRepository
import com.group8.comp2300.domain.model.user.User
import com.group8.comp2300.dto.AuthResponse
import com.group8.comp2300.dto.LoginRequest
import com.group8.comp2300.dto.RegisterRequest
import com.group8.comp2300.dto.TokenResponse
import com.group8.comp2300.security.JwtService
import com.group8.comp2300.security.PasswordHasher
import java.security.MessageDigest
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtService: JwtService
) {
    private val passwordHasher = PasswordHasher

    fun register(request: RegisterRequest): Result<AuthResponse> {
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
                sexualOrientation = request.sexualOrientation
            )

            val user = userRepository.findById(userId)
                ?: return Result.failure(IllegalStateException("Failed to retrieve newly created user"))

            val tokens = generateTokenPair(userId)

            Result.success(
                AuthResponse(
                    user = user,
                    accessToken = tokens.accessToken,
                    refreshToken = tokens.refreshToken
                )
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

        val tokens = generateTokenPair(user.id)

        Result.success(
            AuthResponse(
                user = user,
                accessToken = tokens.accessToken,
                refreshToken = tokens.refreshToken
            )
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
                refreshToken = newTokens.refreshToken
            )
        )
    }

    fun logout(userId: String) {
        refreshTokenRepository.revokeAllForUser(userId)
    }

    fun getUserById(userId: String): User? = userRepository.findById(userId)

    private fun generateTokenPair(userId: String): TokenPair {
        val accessToken = jwtService.generateAccessToken(userId)
        val refreshToken = jwtService.generateRefreshToken(userId)
        val tokenHash = hashToken(refreshToken)

        refreshTokenRepository.insert(tokenHash, userId)

        return TokenPair(accessToken, refreshToken)
    }

    private fun generateUserId(): String = "user_${UUID.randomUUID()}"

    private fun hashToken(token: String): String = MessageDigest.getInstance("SHA-256")
        .digest(token.toByteArray())
        .joinToString(separator = "") { byte -> "%02x".format(byte) }

    private fun cleanExpiredTokens() {
        val cutoff = Clock.System.now() - 90.days
        refreshTokenRepository.deleteExpired(cutoff.toEpochMilliseconds())
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
}
