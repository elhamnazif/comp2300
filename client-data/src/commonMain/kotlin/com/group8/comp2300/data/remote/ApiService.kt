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
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody

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
        client.post("/api/auth/register") { setBody(request) }.body()

    override suspend fun login(request: LoginRequest): AuthResponse =
        client.post("/api/auth/login") { setBody(request) }.body()

    override suspend fun refreshToken(request: RefreshTokenRequest): TokenResponse =
        client.post("/api/auth/refresh") { setBody(request) }.body()

    override suspend fun logout() {
        client.post("/api/auth/logout")
    }

    override suspend fun getProfile(): User = client.get("/api/auth/profile").body()
}
