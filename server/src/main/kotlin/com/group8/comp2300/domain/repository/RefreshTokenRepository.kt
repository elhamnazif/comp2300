package com.group8.comp2300.domain.repository

interface RefreshTokenRepository {
    fun insert(tokenHash: String, userId: String)
    fun findValid(tokenHash: String): String?
    fun revoke(tokenHash: String)
    fun revokeAllForUser(userId: String)
    fun deleteExpired(beforeMs: Long)
}
