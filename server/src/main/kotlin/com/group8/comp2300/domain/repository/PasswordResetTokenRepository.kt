package com.group8.comp2300.domain.repository

interface PasswordResetTokenRepository {
    fun insert(tokenHash: String, userId: String)
    fun findValid(tokenHash: String): String?
    fun markUsed(tokenHash: String)
    fun deleteExpired(cutoffMillis: Long)
}
