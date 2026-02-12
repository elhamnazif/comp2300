package com.group8.comp2300.data.remote

import com.group8.comp2300.ServerConfig
import com.group8.comp2300.data.remote.dto.RefreshTokenRequest
import com.group8.comp2300.data.remote.dto.TokenResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

val tokenProviderDelegate = TokenProviderDelegate()

interface TokenProvider {
    suspend fun getAccessToken(): String?
    suspend fun getRefreshToken(): String?
    suspend fun saveTokens(accessToken: String, refreshToken: String, expiresAt: Long)
    suspend fun clearTokens()
}

class TokenProviderDelegate : TokenProvider {
    private var delegate: TokenProvider? = null

    fun setDelegate(provider: TokenProvider) {
        delegate = provider
    }

    override suspend fun getAccessToken(): String? = delegate?.getAccessToken()

    override suspend fun getRefreshToken(): String? = delegate?.getRefreshToken()

    override suspend fun saveTokens(accessToken: String, refreshToken: String, expiresAt: Long) {
        delegate?.saveTokens(accessToken, refreshToken, expiresAt)
    }

    override suspend fun clearTokens() {
        delegate?.clearTokens()
    }
}

fun createHttpClient(): HttpClient = HttpClient {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            }
        )
    }

    install(Auth) {
        bearer {
            loadTokens {
                val accessToken = tokenProviderDelegate.getAccessToken()
                val refreshToken = tokenProviderDelegate.getRefreshToken()
                if (accessToken != null && refreshToken != null) {
                    BearerTokens(accessToken = accessToken, refreshToken = refreshToken)
                } else {
                    null
                }
            }
            refreshTokens {
                val refreshToken = tokenProviderDelegate.getRefreshToken()
                if (refreshToken != null) {
                    try {
                        // Create a separate client for refresh to avoid circular dependency
                        val refreshClient = HttpClient {
                            install(ContentNegotiation) {
                                json(Json { ignoreUnknownKeys = true })
                            }
                            defaultRequest {
                                url(ServerConfig.BASE_URL)
                                contentType(ContentType.Application.Json)
                            }
                        }

                        val response =
                            refreshClient.post("/api/auth/refresh") {
                                setBody(RefreshTokenRequest(refreshToken))
                            }.body<TokenResponse>()

                        // Use Duration-based expiration time
                        val expiresAt = Clock.System.now().toEpochMilliseconds() + ACCESS_TOKEN_EXPIRATION_MS
                        tokenProviderDelegate.saveTokens(response.accessToken, response.refreshToken, expiresAt)
                        BearerTokens(accessToken = response.accessToken, refreshToken = response.refreshToken)
                    } catch (e: Exception) {
                        // Refresh failed - clear tokens so user must re-login
                        tokenProviderDelegate.clearTokens()
                        null
                    }
                } else {
                    null
                }
            }
        }
    }

    defaultRequest {
        url(ServerConfig.BASE_URL)
        contentType(ContentType.Application.Json)
    }
}

private val ACCESS_TOKEN_EXPIRATION = 15.minutes
private val ACCESS_TOKEN_EXPIRATION_MS = ACCESS_TOKEN_EXPIRATION.inWholeMilliseconds
