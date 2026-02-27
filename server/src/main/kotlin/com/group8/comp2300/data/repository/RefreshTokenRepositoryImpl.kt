package com.group8.comp2300.data.repository

import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.domain.repository.RefreshTokenRepository
import kotlin.time.Clock
import kotlin.time.Duration

class RefreshTokenRepositoryImpl(private val database: ServerDatabase, private val refreshTokenExpiration: Duration) :
    RefreshTokenRepository {

    override fun insert(tokenHash: String, userId: String) {
        val now = Clock.System.now()
        val expiresAt = now + refreshTokenExpiration

        database.refreshTokenQueries.insertRefreshToken(
            token = tokenHash,
            userId = userId,
            expiresAt = expiresAt.toEpochMilliseconds(),
            createdAt = now.toEpochMilliseconds(),
        )
    }

    override fun findValid(tokenHash: String): String? {
        val now = Clock.System.now().toEpochMilliseconds()
        return database.refreshTokenQueries.isRefreshTokenValid(tokenHash, now)
            .executeAsOneOrNull()
            ?.userId
    }

    override fun revoke(tokenHash: String) {
        database.refreshTokenQueries.revokeRefreshToken(tokenHash)
    }

    override fun revokeAllForUser(userId: String) {
        database.refreshTokenQueries.revokeAllUserRefreshTokens(userId)
    }

    override fun deleteExpired(beforeMs: Long) {
        database.refreshTokenQueries.deleteExpiredTokens(beforeMs)
    }
}
