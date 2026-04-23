package com.group8.comp2300.domain.model.user

import kotlinx.datetime.LocalDate

data class UpdateProfileInput(
    val firstName: String,
    val lastName: String,
    val phone: String? = null,
    val dateOfBirth: LocalDate? = null,
    val gender: Gender? = null,
    val sexualOrientation: SexualOrientation? = null,
)
