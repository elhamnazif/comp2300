package com.group8.comp2300.domain.usecase.auth

import com.group8.comp2300.domain.repository.AuthRepository

class DeactivateAccountUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(currentPassword: String): Result<Unit> =
        repository.deactivateAccount(currentPassword)
}
