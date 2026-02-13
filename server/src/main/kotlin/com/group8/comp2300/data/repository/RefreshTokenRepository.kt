package com.group8.comp2300.data.repository

import com.group8.comp2300.database.ServerDatabase
import kotlin.time.Clock

class RefreshTokenRepository(
    private val database: ServerDatabase,
    private val refreshTokenExpiration: kotlin.time.Duration
) {

    fun insert(tokenHash: String, userId: String) {
        val now = Clock.System.now()
        val expiresAt = now + refreshTokenExpiration

        database.refreshTokenQueries.insertRefreshToken(
            token = tokenHash,
            userId = userId,
            expiresAt = expiresAt.toEpochMilliseconds(),
            createdAt = now.toEpochMilliseconds()
        )
    }

    fun findValid(tokenHash: String): String? {
        val now = Clock.System.now().toEpochMilliseconds()
        return database.refreshTokenQueries.isRefreshTokenValid(tokenHash, now)
            .executeAsOneOrNull()
            ?.userId
    }

    fun revoke(tokenHash: String) {
        database.refreshTokenQueries.revokeRefreshToken(tokenHash)
    }

    fun revokeAllForUser(userId: String) {
        database.refreshTokenQueries.revokeAllUserRefreshTokens(userId)
    }

    fun deleteExpired(beforeMs: Long) {
        database.refreshTokenQueries.deleteExpiredTokens(beforeMs)
    }
}
