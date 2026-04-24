package com.group8.comp2300.domain.usecase.auth

import com.group8.comp2300.domain.repository.AuthRepository

class RequestEmailChangeUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(currentPassword: String, newEmail: String): Result<Unit> =
        repository.requestEmailChange(currentPassword, newEmail)
}
