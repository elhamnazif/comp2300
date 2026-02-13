package com.group8.comp2300.data.remote

import co.touchlab.kermit.Logger
import com.group8.comp2300.ServerConfig
import com.group8.comp2300.data.remote.dto.RefreshTokenRequest
import com.group8.comp2300.data.remote.dto.TokenResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

private val logger = Logger.withTag("HttpClientFactory")

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

    // Must handle non-success responses BEFORE ContentNegotiation tries to deserialize
    // Otherwise JsonConvertException is thrown when error body doesn't match expected type
    expectSuccess = true

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
                        logger.d { "Attempting to refresh access token" }
                        val response =
                            client.post("/api/auth/refresh") {
                                markAsRefreshTokenRequest()
                                setBody(RefreshTokenRequest(refreshToken))
                            }.body<TokenResponse>()

                        // Use Duration-based expiration time
                        val expiresAt = Clock.System.now().toEpochMilliseconds() + ACCESS_TOKEN_EXPIRATION_MS
                        tokenProviderDelegate.saveTokens(response.accessToken, response.refreshToken, expiresAt)
                        logger.i { "Token refresh successful" }
                        BearerTokens(accessToken = response.accessToken, refreshToken = response.refreshToken)
                    } catch (e: Exception) {
                        // Refresh failed - clear tokens so user must re-login
                        logger.w(e) { "Token refresh failed, clearing tokens" }
                        tokenProviderDelegate.clearTokens()
                        null
                    }
                } else {
                    logger.d { "No refresh token available" }
                    null
                }
            }
        }
    }

    HttpResponseValidator {
        handleResponseExceptionWithRequest { cause, _ ->
            val responseException = cause as? ResponseException ?: return@handleResponseExceptionWithRequest
            val response = responseException.response
            val rawBody = runCatching { response.bodyAsText() }.getOrNull().orEmpty().trim()
            val parsedMessage = rawBody.extractApiErrorMessage()
            val fallbackMessage = defaultErrorMessage(response.status)
            val message = parsedMessage ?: fallbackMessage
            throw ApiException(statusCode = response.status.value, message = message, cause = responseException)
        }
    }

    defaultRequest {
        url(ServerConfig.BASE_URL)
        contentType(ContentType.Application.Json)
    }
}

private fun String.extractApiErrorMessage(): String? {
    if (isBlank()) return null

    val parsedError =
        runCatching {
            Json.parseToJsonElement(this)
                .jsonObject["error"]
                ?.jsonPrimitive
                ?.content
                ?.trim()
        }.getOrNull()

    if (!parsedError.isNullOrBlank()) {
        return parsedError
    }

    // Keep plain-text error responses if they are not HTML error pages.
    if (!startsWith("<")) {
        return this
    }

    return null
}

private fun defaultErrorMessage(status: HttpStatusCode): String =
    when (status) {
        HttpStatusCode.BadRequest -> "Invalid request"
        HttpStatusCode.Unauthorized -> "Authentication failed"
        HttpStatusCode.NotFound -> "Requested resource was not found"
        HttpStatusCode.Conflict -> "Request conflicts with current data"
        else -> "Request failed (${status.value})"
    }

private val ACCESS_TOKEN_EXPIRATION = 15.minutes
private val ACCESS_TOKEN_EXPIRATION_MS = ACCESS_TOKEN_EXPIRATION.inWholeMilliseconds
