package com.group8.comp2300.data.remote

import com.group8.comp2300.ServerConfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun createHttpClient(): HttpClient = HttpClient {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            },
        )
    }
    defaultRequest {
        url("http://10.0.2.2:${ServerConfig.PORT}")
        // NOTE: Use above until we figure out why expect/actual is breaking
        // url(baseUrl)
        contentType(ContentType.Application.Json)
    }
}
