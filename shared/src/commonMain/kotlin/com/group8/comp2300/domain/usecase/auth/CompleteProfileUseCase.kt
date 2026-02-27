package com.group8.comp2300.domain.usecase.auth

import com.group8.comp2300.domain.model.user.Gender
import com.group8.comp2300.domain.model.user.SexualOrientation
import com.group8.comp2300.domain.model.user.User
import com.group8.comp2300.domain.repository.AuthRepository
import kotlinx.datetime.LocalDate

class CompleteProfileUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(
        firstName: String,
        lastName: String,
        gender: Gender,
        sexualOrientation: SexualOrientation,
        dateOfBirth: LocalDate?,
    ): Result<User> = repository.completeProfile(firstName, lastName, gender, sexualOrientation, dateOfBirth)
}
