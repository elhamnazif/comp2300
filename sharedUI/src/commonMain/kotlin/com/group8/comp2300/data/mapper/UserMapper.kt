package com.group8.comp2300.data.mapper

import com.group8.comp2300.data.remote.dto.UserDto
import com.group8.comp2300.domain.model.user.Gender
import com.group8.comp2300.domain.model.user.SexualOrientation
import com.group8.comp2300.domain.model.user.User
import kotlinx.datetime.LocalDate

fun UserDto.toDomain(): User = User(
    id = id,
    firstName = firstName,
    lastName = lastName,
    email = email,
    phone = phone,
    dateOfBirth = dateOfBirth?.let {
        try {
            LocalDate.parse(it)
        } catch (e: Exception) {
            null
        }
    },
    gender = gender?.let {
        try {
            Gender.valueOf(it.uppercase())
        } catch (e: Exception) {
            Gender.PREFER_NOT_TO_SAY
        }
    },
    sexualOrientation = sexualOrientation?.let {
        try {
            SexualOrientation.valueOf(it.uppercase())
        } catch (
            e: Exception
        ) {
            SexualOrientation.PREFER_NOT_TO_SAY
        }
    },
    profileImageUrl = profileImageUrl,
    createdAt = createdAt,
    isAnonymous = isAnonymous,
    hasCompletedOnboarding = hasCompletedOnboarding,
    preferredLanguage = preferredLanguage
)
