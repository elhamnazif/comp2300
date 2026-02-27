package com.group8.comp2300.data.repository

import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.database.data.UserEntity
import com.group8.comp2300.domain.model.user.Gender
import com.group8.comp2300.domain.model.user.SexualOrientation
import com.group8.comp2300.domain.model.user.User
import com.group8.comp2300.domain.repository.UserRepository
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class UserRepositoryImpl(private val database: ServerDatabase) : UserRepository {

    // In-memory rate limiting cache for verification requests
    // Maps email to last request timestamp
    private val verificationRequestCache = mutableMapOf<String, Long>()
    private val rateLimitPeriod = 1.minutes

    override fun findByEmail(email: String): User? =
        database.userQueries.selectUserByEmail(email).executeAsOneOrNull()?.toDomain()

    override fun findById(id: String): User? = database.userQueries.selectUserById(id).executeAsOneOrNull()?.toDomain()

    override fun existsByEmail(email: String): Boolean =
        database.userQueries.selectUserByEmail(email).executeAsOneOrNull() != null

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
    ) {
        val now = Clock.System.now().toEpochMilliseconds()
        database.userQueries.insertUser(
            id = id,
            email = email,
            passwordHash = passwordHash,
            firstName = firstName,
            lastName = lastName,
            phone = phone,
            dateOfBirth = dateOfBirth,
            gender = gender,
            sexualOrientation = sexualOrientation,
            profileImageUrl = null,
            createdAt = now,
            preferredLanguage = preferredLanguage,
            isActivated = 0,
        )
    }

    override fun activateUser(userId: String) {
        database.userQueries.activateUser(userId)
    }

    override fun isActivated(userId: String): Boolean {
        val user = database.userQueries.selectUserById(userId).executeAsOneOrNull() ?: return false
        return user.isActivated != 0L
    }

    override fun getPasswordHash(email: String): String? =
        database.userQueries.selectUserByEmail(email).executeAsOneOrNull()?.passwordHash

    override fun updatePasswordHash(userId: String, newHash: String) {
        database.userQueries.updatePassword(newHash, userId)
    }

    override fun updateProfile(
        userId: String,
        firstName: String,
        lastName: String,
        dateOfBirth: Long?,
        gender: String?,
        sexualOrientation: String?,
    ) {
        database.userQueries.updateProfile(
            firstName = firstName,
            lastName = lastName,
            dateOfBirth = dateOfBirth,
            gender = gender,
            sexualOrientation = sexualOrientation,
            id = userId,
        )
    }

    override fun canRequestVerification(email: String): Boolean {
        val lastRequest = verificationRequestCache[email] ?: return true
        val now = Clock.System.now().toEpochMilliseconds()
        val elapsed = now - lastRequest
        return elapsed >= rateLimitPeriod.inWholeMilliseconds
    }

    override fun recordVerificationRequest(email: String) {
        verificationRequestCache[email] = Clock.System.now().toEpochMilliseconds()
    }

    override fun deleteUnactivatedAccounts(cutoffMillis: Long) {
        database.userQueries.deleteUnactivatedAccounts(cutoffMillis)
    }
}

private fun UserEntity.toDomain(): User = User(
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
    preferredLanguage = preferredLanguage,
)
