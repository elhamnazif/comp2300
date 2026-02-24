package com.group8.comp2300.data.auth

import com.group8.comp2300.data.local.Session
import com.group8.comp2300.data.local.SessionDataSource
import kotlin.time.Clock

interface TokenManager {
    suspend fun saveTokens(userId: String, accessToken: String, refreshToken: String, expiresAt: Long)
    suspend fun getAccessToken(): String?
    suspend fun getRefreshToken(): String?
    suspend fun getUserId(): String?
    suspend fun clearTokens()
    suspend fun isTokenExpired(): Boolean
}

class TokenManagerImpl(private val sessionDataSource: SessionDataSource) : TokenManager {

    override suspend fun saveTokens(userId: String, accessToken: String, refreshToken: String, expiresAt: Long) {
        sessionDataSource.saveSession(userId, accessToken, refreshToken, expiresAt)
    }

    override suspend fun getAccessToken(): String? = sessionDataSource.getSession()?.accessToken

    override suspend fun getRefreshToken(): String? = sessionDataSource.getSession()?.refreshToken

    override suspend fun getUserId(): String? = sessionDataSource.getSession()?.userId

    override suspend fun clearTokens() {
        sessionDataSource.clearSession()
    }

    override suspend fun isTokenExpired(): Boolean {
        val session = sessionDataSource.getSession() ?: return true
        val currentTime = Clock.System.now().toEpochMilliseconds()
        return currentTime >= session.expiresAt
    }
}
