package com.group8.comp2300.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String? = null,
    val dateOfBirth: String? = null, // String date in API
    val gender: String? = null,
    val sexualOrientation: String? = null,
    val profileImageUrl: String? = null,
    val createdAt: Long = 0L,
    val isAnonymous: Boolean = false,
    val hasCompletedOnboarding: Boolean = false,
    val preferredLanguage: String = "en",
)
