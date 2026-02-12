package com.group8.comp2300.service

import at.favre.lib.crypto.bcrypt.BCrypt
import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.domain.model.user.Gender
import com.group8.comp2300.domain.model.user.SexualOrientation
import com.group8.comp2300.domain.model.user.User
import com.group8.comp2300.dto.AuthResponse
import com.group8.comp2300.dto.LoginRequest
import com.group8.comp2300.dto.RefreshTokenRequest
import com.group8.comp2300.dto.RegisterRequest
import com.group8.comp2300.dto.TokenResponse
import com.group8.comp2300.security.JwtService
import com.group8.comp2300.security.PasswordHasher
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class AuthService(private val database: ServerDatabase, private val jwtService: JwtService) {
    private val passwordHasher = PasswordHasher

    suspend fun register(request: RegisterRequest): Result<AuthResponse> {
        // Validate input before checking database
        val validationResult = validateRegisterRequest(request)
        if (validationResult is ValidationResult.Invalid) {
            return Result.failure(IllegalArgumentException(validationResult.message))
        }

        // Check if user already exists
        val existingUser = database.serverDatabaseQueries.selectUserByEmail(request.email).executeAsOneOrNull()
        if (existingUser != null) {
            // Return generic error to prevent email enumeration
            return Result.failure(IllegalArgumentException("Registration failed"))
        }

        return try {
            // Hash password
            val passwordHash = passwordHasher.hash(request.password)

            // Create new user with UUID to prevent race conditions
            val userId = generateUserId()
            val now = Clock.System.now().toEpochMilliseconds()

            database.serverDatabaseQueries.insertUser(
                id = userId,
                email = request.email,
                passwordHash = passwordHash,
                firstName = request.firstName,
                lastName = request.lastName,
                phone = request.phone,
                dateOfBirth = request.dateOfBirth,
                gender = request.gender,
                sexualOrientation = request.sexualOrientation,
                profileImageUrl = null,
                createdAt = now,
                preferredLanguage = "en"
            )

            val user = database.serverDatabaseQueries.selectUserById(userId).executeAsOneOrNull()
                ?: return Result.failure(IllegalStateException("Failed to retrieve newly created user"))

            val accessToken = jwtService.generateAccessToken(userId)
            val refreshToken = jwtService.generateRefreshToken(userId)

            // Store refresh token in database for revocation tracking
            storeRefreshToken(refreshToken, userId)

            Result.success(
                AuthResponse(
                    user = user.toDomainUser(),
                    accessToken = accessToken,
                    refreshToken = refreshToken
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(request: LoginRequest): Result<AuthResponse> = try {
        val userEntity = database.serverDatabaseQueries.selectUserByEmail(request.email).executeAsOneOrNull()
            ?: return Result.failure(IllegalArgumentException("Invalid email or password"))

        if (!passwordHasher.verify(request.password, userEntity.passwordHash)) {
            return Result.failure(IllegalArgumentException("Invalid email or password"))
        }

        val accessToken = jwtService.generateAccessToken(userEntity.id)
        val refreshToken = jwtService.generateRefreshToken(userEntity.id)

        // Store refresh token in database for revocation tracking
        storeRefreshToken(refreshToken, userEntity.id)

        Result.success(
            AuthResponse(
                user = userEntity.toDomainUser(),
                accessToken = accessToken,
                refreshToken = refreshToken
            )
        )
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun refreshToken(refreshToken: String): Result<TokenResponse> {
        // Verify token signature and expiry
        val jwt = try {
            com.auth0.jwt.JWT.decode(refreshToken)
        } catch (e: Exception) {
            return Result.failure(IllegalArgumentException("Invalid refresh token"))
        }

        val userId = jwt.subject
        val tokenType = jwt.getClaim("type").asString()

        if (tokenType != "refresh") {
            return Result.failure(IllegalArgumentException("Invalid token type"))
        }

        // Check if token is expired
        val verifier = jwtService.verifier
        try {
            verifier.verify(refreshToken)
        } catch (e: Exception) {
            return Result.failure(IllegalArgumentException("Refresh token is expired or invalid"))
        }

        // Hash the token to check against database
        val tokenHash = hashToken(refreshToken)
        val now = System.currentTimeMillis()

        // Check if token exists and is not revoked
        val storedToken = database.serverDatabaseQueries.isRefreshTokenValid(tokenHash, now)
            .executeAsOneOrNull()

        if (storedToken == null) {
            return Result.failure(IllegalArgumentException("Refresh token is invalid or has been revoked"))
        }

        // Verify the token belongs to the user in the JWT
        if (storedToken.userId != userId) {
            return Result.failure(IllegalArgumentException("Token does not belong to the specified user"))
        }

        // Revoke the old refresh token (token rotation)
        database.serverDatabaseQueries.revokeRefreshToken(tokenHash)

        // Generate new token pair
        val newAccessToken = jwtService.generateAccessToken(userId)
        val newRefreshToken = jwtService.generateRefreshToken(userId)

        // Store the new refresh token
        storeRefreshToken(newRefreshToken, userId)

        // Clean up expired tokens periodically
        cleanExpiredTokens()

        return Result.success(
            TokenResponse(
                accessToken = newAccessToken,
                refreshToken = newRefreshToken
            )
        )
    }

    suspend fun logout(userId: String) {
        // Revoke all refresh tokens for this user
        database.serverDatabaseQueries.revokeAllUserRefreshTokens(userId)
    }

    suspend fun getUserById(userId: String): User? =
        database.serverDatabaseQueries.selectUserById(userId).executeAsOneOrNull()?.toDomainUser()

    private fun generateUserId(): String = "user_${UUID.randomUUID()}"

    private fun hashToken(token: String): String {
        // Use bcrypt to hash the token for secure storage
        val chars = token.toCharArray()
        return BCrypt.withDefaults().hashToString(4, chars)
    }

    private suspend fun storeRefreshToken(token: String, userId: String) {
        val tokenHash = hashToken(token)
        val now = Clock.System.now()
        val expiresAt = now + jwtService.refreshTokenExpiration

        database.serverDatabaseQueries.insertRefreshToken(
            token = tokenHash,
            userId = userId,
            expiresAt = expiresAt.toEpochMilliseconds(),
            createdAt = now.toEpochMilliseconds()
        )
    }

    private suspend fun cleanExpiredTokens() {
        val cutoff = Clock.System.now() - 90.days
        database.serverDatabaseQueries.deleteExpiredTokens(cutoff.toEpochMilliseconds())
    }

    private fun validateRegisterRequest(request: RegisterRequest): ValidationResult {
        when {
            request.email.isBlank() || !isValidEmail(request.email) ->
                return ValidationResult.Invalid("Invalid email format")

            request.password.length < 8 ->
                return ValidationResult.Invalid("Password must be at least 8 characters")

            !request.password.any { it.isDigit() } ->
                return ValidationResult.Invalid("Password must contain at least one number")

            !request.password.any { it.isLetter() } ->
                return ValidationResult.Invalid("Password must contain at least one letter")

            request.firstName.isBlank() || request.lastName.isBlank() ->
                return ValidationResult.Invalid("Name fields cannot be blank")
        }
        return ValidationResult.Valid
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return emailRegex.matches(email)
    }
}

private sealed class ValidationResult {
    data object Valid : ValidationResult()
    data class Invalid(val message: String) : ValidationResult()
}

private fun com.group8.comp2300.database.UserEntity.toDomainUser(): User = User(
    id = id,
    email = email,
    firstName = firstName,
    lastName = lastName,
    phone = phone,
    dateOfBirth = dateOfBirth?.let { epochMs ->
        Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(TimeZone.UTC).date
    },
    gender = gender?.let { Gender.entries.find { g -> g.name == it } },
    sexualOrientation = sexualOrientation?.let { SexualOrientation.entries.find { s -> s.name == it } },
    profileImageUrl = profileImageUrl,
    createdAt = createdAt,
    isAnonymous = false,
    hasCompletedOnboarding = false,
    preferredLanguage = preferredLanguage
)
