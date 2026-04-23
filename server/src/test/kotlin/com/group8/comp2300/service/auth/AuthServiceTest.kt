package com.group8.comp2300.service.auth

import com.group8.comp2300.domain.model.user.User
import com.group8.comp2300.domain.repository.PasswordResetTokenRepository
import com.group8.comp2300.domain.repository.RefreshTokenRepository
import com.group8.comp2300.domain.repository.UserRepository
import com.group8.comp2300.security.JwtService
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthServiceTest {
    @Test
    fun uploadProfilePhotoPreservesExistingAvatarWhenDbUpdateFails() {
        val uploadDir = createTempDirectory("auth-service-profile-images").toFile()
        val oldFile = uploadDir.resolve("user-1-old.png").apply { writeText("old-image") }
        val repository = FailingProfileImageRepository(
            user = User(
                id = "user-1",
                firstName = "Jamie",
                lastName = "Park",
                email = "jamie@example.com",
                profileImageUrl = "/images/profile/${oldFile.name}",
            ),
        )
        val authService = AuthService(
            userRepository = repository,
            refreshTokenRepository = NoopRefreshTokenRepository(),
            passwordResetTokenRepository = NoopPasswordResetTokenRepository(),
            jwtService = NoopJwtService(),
            emailService = null,
            verificationRequestThrottle = InMemoryVerificationRequestThrottle(),
            profileImageStorage = ProfileImageStorage(uploadDir.path),
        )

        val result = authService.uploadProfilePhoto(
            userId = "user-1",
            fileName = "avatar.png",
            fileBytes = "new-image".encodeToByteArray(),
        )

        assertTrue(result.isFailure)
        assertEquals("/images/profile/${oldFile.name}", repository.currentUser.profileImageUrl)
        assertTrue(oldFile.exists())
        assertEquals(listOf(oldFile.name), uploadDir.listFiles().orEmpty().map { it.name })
    }
}

private class FailingProfileImageRepository(user: User) : UserRepository {
    var currentUser: User = user

    override fun findByEmail(email: String): User? = null

    override fun findById(id: String): User? = currentUser.takeIf { it.id == id }

    override fun existsByEmail(email: String): Boolean = false

    override fun insert(
        id: String,
        email: String,
        passwordHash: String,
        firstName: String,
        lastName: String,
        phone: String?,
        dateOfBirth: Long?,
        gender: String?,
        sexualOrientation: String?,
        preferredLanguage: String,
    ) = error("unused")

    override fun getPasswordHash(email: String): String? = null

    override fun updatePasswordHash(userId: String, newHash: String) = error("unused")

    override fun activateUser(userId: String) = error("unused")

    override fun isActivated(userId: String): Boolean = true

    override fun updateProfile(
        userId: String,
        firstName: String,
        lastName: String,
        phone: String?,
        dateOfBirth: Long?,
        gender: String?,
        sexualOrientation: String?,
    ) = error("unused")

    override fun updateProfileImageUrl(userId: String, profileImageUrl: String?) {
        if (profileImageUrl != currentUser.profileImageUrl) {
            throw IllegalStateException("db write failed")
        }
        currentUser = currentUser.copy(profileImageUrl = profileImageUrl)
    }

    override fun deleteUnactivatedAccounts(cutoffMillis: Long) = error("unused")

    override fun existsByEmailAndActivated(email: String): Boolean = false

    override fun findByEmailAndNotActivated(email: String): User? = null

    override fun deleteById(userId: String) = error("unused")
}

private class NoopRefreshTokenRepository : RefreshTokenRepository {
    override fun insert(tokenHash: String, userId: String) = Unit

    override fun findValid(tokenHash: String): String? = null

    override fun revoke(tokenHash: String) = Unit

    override fun revokeAllForUser(userId: String) = Unit

    override fun deleteExpired(beforeMs: Long) = Unit
}

private class NoopPasswordResetTokenRepository : PasswordResetTokenRepository {
    override fun insert(tokenHash: String, userId: String) = Unit

    override fun findValid(tokenHash: String): String? = null

    override fun markUsed(tokenHash: String) = Unit

    override fun deleteExpired(cutoffMillis: Long) = Unit

    override fun deleteByUserId(userId: String) = Unit
}

private class NoopJwtService : JwtService {
    override val verifier
        get() = error("unused")
    override val refreshVerifier
        get() = error("unused")
    override val accessTokenExpiration = kotlin.time.Duration.ZERO
    override val refreshTokenExpiration = kotlin.time.Duration.ZERO

    override fun generateAccessToken(userId: String): String = error("unused")

    override fun generateRefreshToken(userId: String): String = error("unused")
}
