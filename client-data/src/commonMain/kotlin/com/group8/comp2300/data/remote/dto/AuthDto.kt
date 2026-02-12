package com.group8.comp2300.data.remote.dto

import com.group8.comp2300.domain.model.user.User
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val phone: String? = null,
    val gender: String? = null,
    val sexualOrientation: String? = null,
    val dateOfBirth: Long? = null
)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class AuthResponse(val user: User, val accessToken: String, val refreshToken: String)

@Serializable
data class TokenResponse(val accessToken: String, val refreshToken: String)

@Serializable
data class RefreshTokenRequest(val refreshToken: String)
