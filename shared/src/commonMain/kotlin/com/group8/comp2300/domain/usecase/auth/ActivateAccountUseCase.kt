package com.group8.comp2300.domain.usecase.auth

import com.group8.comp2300.domain.repository.AuthRepository

class ActivateAccountUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(token: String): Result<Unit> = repository.activateAccount(token)
}
