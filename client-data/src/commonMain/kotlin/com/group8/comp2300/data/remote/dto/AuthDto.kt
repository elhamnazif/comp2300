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
    val dateOfBirth: Long? = null,
)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class AuthResponse(val user: User, val accessToken: String, val refreshToken: String)

@Serializable
data class TokenResponse(val accessToken: String, val refreshToken: String)

@Serializable
data class RefreshTokenRequest(val refreshToken: String)

@Serializable
data class ActivateRequest(val token: String)

@Serializable
data class ForgotPasswordRequest(val email: String)

@Serializable
data class ResetPasswordRequest(val token: String, val newPassword: String)

@Serializable
data class MessageResponse(val message: String)

@Serializable
data class PreregisterRequest(val email: String, val password: String)

@Serializable
data class PreregisterResponse(val email: String, val message: String)

@Serializable
data class CompleteProfileRequest(
    val firstName: String,
    val lastName: String,
    val dateOfBirth: Long? = null,
    val gender: String? = null,
    val sexualOrientation: String? = null,
)
