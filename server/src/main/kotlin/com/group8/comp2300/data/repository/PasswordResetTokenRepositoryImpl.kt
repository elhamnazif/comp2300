package com.group8.comp2300.data.repository

import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.domain.repository.PasswordResetTokenRepository
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

class PasswordResetTokenRepositoryImpl(private val database: ServerDatabase) : PasswordResetTokenRepository {

    override fun insert(tokenHash: String, userId: String) {
        val now = Clock.System.now()
        val expiresAt = now + TOKEN_EXPIRATION

        database.passwordResetTokenQueries.insertPasswordResetToken(
            token = tokenHash,
            userId = userId,
            expiresAt = expiresAt.toEpochMilliseconds(),
            createdAt = now.toEpochMilliseconds(),
        )
    }

    override fun findValid(tokenHash: String): String? {
        val now = Clock.System.now().toEpochMilliseconds()
        return database.passwordResetTokenQueries.findValidToken(tokenHash, now)
            .executeAsOneOrNull()
            ?.userId
    }

    override fun markUsed(tokenHash: String) {
        database.passwordResetTokenQueries.markTokenUsed(tokenHash)
    }

    override fun deleteExpired(cutoffMillis: Long) {
        database.passwordResetTokenQueries.deleteExpiredPasswordResetTokens(cutoffMillis)
    }

    companion object {
        val TOKEN_EXPIRATION = 1.hours
    }
}
