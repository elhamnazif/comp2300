package com.group8.comp2300.data.remote

import com.group8.comp2300.data.remote.dto.AuthResponse
import com.group8.comp2300.data.remote.dto.LoginRequest
import com.group8.comp2300.data.remote.dto.ProductDto
import com.group8.comp2300.data.remote.dto.RefreshTokenRequest
import com.group8.comp2300.data.remote.dto.RegisterRequest
import com.group8.comp2300.data.remote.dto.TokenResponse
import com.group8.comp2300.domain.model.user.User
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.JsonConvertException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

interface ApiService {
    suspend fun getHealth(): Map<String, String>

    suspend fun getProducts(): List<ProductDto>

    suspend fun getProduct(id: String): ProductDto

    suspend fun register(request: RegisterRequest): AuthResponse

    suspend fun login(request: LoginRequest): AuthResponse

    suspend fun refreshToken(request: RefreshTokenRequest): TokenResponse

    suspend fun logout()

    suspend fun getProfile(): User
}

class ApiServiceImpl(private val client: HttpClient) : ApiService {
    override suspend fun getHealth(): Map<String, String> = client.get("/api/health").body()

    override suspend fun getProducts(): List<ProductDto> = client.get("/api/products").body()

    override suspend fun getProduct(id: String): ProductDto = client.get("/api/products/$id").body()

    override suspend fun register(request: RegisterRequest): AuthResponse =
        handleAuthResponse(client.post("/api/auth/register") { setBody(request) })

    override suspend fun login(request: LoginRequest): AuthResponse =
        handleAuthResponse(client.post("/api/auth/login") { setBody(request) })

    override suspend fun refreshToken(request: RefreshTokenRequest): TokenResponse =
        client.post("/api/auth/refresh") { setBody(request) }.body()

    override suspend fun logout() {
        client.post("/api/auth/logout")
    }

    override suspend fun getProfile(): User = client.get("/api/auth/profile").body()

    /**
     * Catches JsonConvertException when server returns an error response like {"error": "..."}
     * instead of the expected AuthResponse format.
     */
    private suspend fun handleAuthResponse(response: io.ktor.client.statement.HttpResponse): AuthResponse = try {
        response.body()
    } catch (e: JsonConvertException) {
        // Extract the actual error message from the response body
        val responseException = e.cause as? ResponseException
        if (responseException != null) {
            val rawBody = responseException.response.bodyAsText()
            val errorMessage = rawBody.extractErrorMessage()
            throw ApiException(
                statusCode = responseException.response.status.value,
                message = errorMessage ?: "Authentication failed",
                cause = e,
            )
        }
        throw ApiException(statusCode = 500, message = "Invalid response from server", cause = e)
    }

    private fun String.extractErrorMessage(): String? = try {
        Json.parseToJsonElement(this)
            .jsonObject["error"]
            ?.jsonPrimitive
            ?.content
    } catch (_: Exception) {
        null
    }
}
