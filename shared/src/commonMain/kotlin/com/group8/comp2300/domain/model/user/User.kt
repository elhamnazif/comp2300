package com.group8.comp2300.domain.model.user

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class User(
        val id: String,
        val firstName: String,
        val lastName: String,
        val email: String,
        val phone: String? = null,
        val dateOfBirth: LocalDate? = null,
        val gender: Gender? = null,
        val sexualOrientation: SexualOrientation? = null,
        val profileImageUrl: String? = null,
        val createdAt: Long = 0L,
        val isAnonymous: Boolean = false,
        val hasCompletedOnboarding: Boolean = false,
        val preferredLanguage: String = "en"
)
