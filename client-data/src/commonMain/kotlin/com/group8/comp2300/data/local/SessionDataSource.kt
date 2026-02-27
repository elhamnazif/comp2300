package com.group8.comp2300.data.local

import com.group8.comp2300.data.database.AppDatabase

class SessionDataSource(private val database: AppDatabase) {
    suspend fun saveSession(userId: String, accessToken: String, refreshToken: String, expiresAt: Long) {
        database.appDatabaseQueries.upsertSession(
            userId = userId,
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAt = expiresAt,
        )
    }

    suspend fun getSession(): Session? =
        database.appDatabaseQueries.selectSession().executeAsOneOrNull()?.let { sessionEntity ->
            Session(
                userId = sessionEntity.userId,
                accessToken = sessionEntity.accessToken,
                refreshToken = sessionEntity.refreshToken,
                expiresAt = sessionEntity.expiresAt,
            )
        }

    suspend fun clearSession() {
        database.appDatabaseQueries.deleteSession()
    }
}

data class Session(val userId: String, val accessToken: String, val refreshToken: String, val expiresAt: Long)
