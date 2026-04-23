package com.group8.comp2300.domain.usecase.auth

import com.group8.comp2300.domain.model.user.UpdateProfileInput
import com.group8.comp2300.domain.model.user.User
import com.group8.comp2300.domain.repository.AuthRepository

class UpdateProfileUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(input: UpdateProfileInput): Result<User> = repository.updateProfile(input)
}
