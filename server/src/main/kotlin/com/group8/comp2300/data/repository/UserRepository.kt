package com.group8.comp2300.data.repository

import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.database.UserEntity
import com.group8.comp2300.domain.model.user.Gender
import com.group8.comp2300.domain.model.user.SexualOrientation
import com.group8.comp2300.domain.model.user.User
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class UserRepository(private val database: ServerDatabase) {

    fun findByEmail(email: String): User? =
        database.userQueries.selectUserByEmail(email).executeAsOneOrNull()?.toDomainUser()

    fun findById(id: String): User? =
        database.userQueries.selectUserById(id).executeAsOneOrNull()?.toDomainUser()

    fun existsByEmail(email: String): Boolean =
        database.userQueries.selectUserByEmail(email).executeAsOneOrNull() != null

    fun insert(
        id: String,
        email: String,
        passwordHash: String,
        firstName: String,
        lastName: String,
        phone: String?,
        dateOfBirth: Long?,
        gender: String?,
        sexualOrientation: String?,
        preferredLanguage: String = "en"
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
            preferredLanguage = preferredLanguage
        )
    }

    fun getPasswordHash(email: String): String? =
        database.userQueries.selectUserByEmail(email).executeAsOneOrNull()?.passwordHash

    fun updatePasswordHash(userId: String, newHash: String) {
        database.userQueries.updatePassword(newHash, userId)
    }
}

private fun UserEntity.toDomainUser(): User = User(
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
