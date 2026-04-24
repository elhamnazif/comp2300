package com.group8.comp2300.domain.repository

import com.group8.comp2300.domain.model.user.User

interface UserRepository {
    fun findByEmail(email: String): User?
    fun findById(id: String): User?
    fun existsByEmail(email: String): Boolean
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
        preferredLanguage: String,
    )
    fun getPasswordHash(email: String): String?
    fun updatePasswordHash(userId: String, newHash: String)
    fun activateUser(userId: String)
    fun isActivated(userId: String): Boolean
    fun isDeactivated(userId: String): Boolean
    fun isActive(userId: String): Boolean
    fun updateProfile(
        userId: String,
        firstName: String,
        lastName: String,
        phone: String?,
        dateOfBirth: Long?,
        gender: String?,
        sexualOrientation: String?,
    )

    fun updateEmail(userId: String, email: String)

    fun updateProfileImageUrl(userId: String, profileImageUrl: String?)

    fun deactivateUser(userId: String, deactivatedAt: Long)

    fun clearDeactivatedAt(userId: String)

    /**
     * Deletes unactivated accounts created before the given cutoff timestamp.
     */
    fun deleteUnactivatedAccounts(cutoffMillis: Long)

    /**
     * Checks if a verified (activated) account exists with the given email.
     */
    fun existsByEmailAndActivated(email: String): Boolean

    /**
     * Finds an unverified (not activated) account by email.
     */
    fun findByEmailAndNotActivated(email: String): User?

    /**
     * Deletes a user by their ID.
     */
    fun deleteById(userId: String)
}
