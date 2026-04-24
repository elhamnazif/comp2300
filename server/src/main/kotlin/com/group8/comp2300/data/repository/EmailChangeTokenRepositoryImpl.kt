package com.group8.comp2300.data.repository

import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.domain.repository.EmailChangeToken
import com.group8.comp2300.domain.repository.EmailChangeTokenRepository
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

class EmailChangeTokenRepositoryImpl(private val database: ServerDatabase) : EmailChangeTokenRepository {
    override fun insert(tokenHash: String, userId: String, newEmail: String) {
        val now = Clock.System.now()
        val expiresAt = now + TOKEN_EXPIRATION

        database.emailChangeTokenQueries.insertEmailChangeToken(
            token = tokenHash,
            userId = userId,
            newEmail = newEmail,
            expiresAt = expiresAt.toEpochMilliseconds(),
            createdAt = now.toEpochMilliseconds(),
        )
    }

    override fun findValid(tokenHash: String): EmailChangeToken? {
        val now = Clock.System.now().toEpochMilliseconds()
        return database.emailChangeTokenQueries.findValidEmailChangeToken(tokenHash, now)
            .executeAsOneOrNull()
            ?.let { EmailChangeToken(userId = it.userId, newEmail = it.newEmail) }
    }

    override fun markUsed(tokenHash: String) {
        database.emailChangeTokenQueries.markEmailChangeTokenUsed(tokenHash)
    }

    override fun deleteExpired(cutoffMillis: Long) {
        database.emailChangeTokenQueries.deleteExpiredEmailChangeTokens(cutoffMillis)
    }

    override fun deleteByUserId(userId: String) {
        database.emailChangeTokenQueries.deleteEmailChangeTokensByUserId(userId)
    }

    companion object {
        val TOKEN_EXPIRATION = 15.minutes
    }
}
