package com.group8.comp2300.data.remote

import com.group8.comp2300.data.remote.dto.ProductDto
import com.group8.comp2300.data.remote.dto.UserDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

interface ApiService {
    suspend fun getHealth(): Map<String, String>

    suspend fun getProducts(): List<ProductDto>

    suspend fun getProduct(id: String): ProductDto
}

class ApiServiceImpl(private val client: HttpClient) : ApiService {
    override suspend fun getHealth(): Map<String, String> = client.get("/api/health").body()

    override suspend fun getProducts(): List<ProductDto> = client.get("/api/products").body()

    override suspend fun getProduct(id: String): ProductDto = client.get("/api/products/$id").body()
}
