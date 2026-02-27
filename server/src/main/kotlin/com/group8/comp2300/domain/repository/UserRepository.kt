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
    fun updateProfile(
        userId: String,
        firstName: String,
        lastName: String,
        dateOfBirth: Long?,
        gender: String?,
        sexualOrientation: String?,
    )

    /**
     * Checks if a verification request can be made for the given email.
     * Returns false if a request was made within the last minute.
     */
    fun canRequestVerification(email: String): Boolean

    /**
     * Records a verification request timestamp for the given email.
     */
    fun recordVerificationRequest(email: String)

    /**
     * Deletes unactivated accounts created before the given cutoff timestamp.
     */
    fun deleteUnactivatedAccounts(cutoffMillis: Long)
}
