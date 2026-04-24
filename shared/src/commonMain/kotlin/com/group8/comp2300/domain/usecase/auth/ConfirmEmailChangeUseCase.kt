package com.group8.comp2300.domain.usecase.auth

import com.group8.comp2300.domain.repository.AuthRepository

class ConfirmEmailChangeUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(code: String): Result<Unit> = repository.confirmEmailChange(code)
}
