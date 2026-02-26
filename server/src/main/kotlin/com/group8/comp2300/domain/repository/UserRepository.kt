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
        preferredLanguage: String
    )
    fun getPasswordHash(email: String): String?
    fun updatePasswordHash(userId: String, newHash: String)
}
