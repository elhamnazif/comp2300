package com.group8.comp2300.routes

import com.group8.comp2300.dto.AuthResponse
import com.group8.comp2300.dto.LoginRequest
import com.group8.comp2300.dto.RefreshTokenRequest
import com.group8.comp2300.dto.RegisterRequest
import com.group8.comp2300.dto.TokenResponse
import com.group8.comp2300.service.AuthService
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.authRoutes(authService: AuthService) {
    post("/api/auth/register") {
        val request = call.receive<RegisterRequest>()
        val result = authService.register(request)
        result.fold(
            onSuccess = { authResponse -> call.respond(HttpStatusCode.Created, authResponse) },
            onFailure = { error ->
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (error.message ?: "Registration failed")))
            }
        )
    }

    post("/api/auth/login") {
        val request = call.receive<LoginRequest>()
        val result = authService.login(request)
        result.fold(
            onSuccess = { authResponse -> call.respond(HttpStatusCode.OK, authResponse) },
            onFailure = { error ->
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to (error.message ?: "Login failed")))
            }
        )
    }

    post("/api/auth/refresh") {
        val request = call.receive<RefreshTokenRequest>()
        val result = authService.refreshToken(request.refreshToken)
        result.fold(
            onSuccess = { tokenResponse -> call.respond(HttpStatusCode.OK, tokenResponse) },
            onFailure = { error ->
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to (error.message ?: "Token refresh failed")))
            }
        )
    }

    authenticate("auth-jwt") {
        get("/api/auth/profile") {
            val userId = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()!!.payload.subject
            val user = authService.getUserById(userId)
            if (user != null) {
                call.respond(HttpStatusCode.OK, user)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
            }
        }

        post("/api/auth/logout") {
            val userId = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()!!.payload.subject
            authService.logout(userId)
            call.respond(HttpStatusCode.OK, mapOf("message" to "Logged out successfully"))
        }
    }
}
