package com.group8.comp2300.domain.usecase.auth

import com.group8.comp2300.domain.repository.AuthRepository

class ResetPasswordUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(token: String, newPassword: String): Result<Unit> =
        repository.resetPassword(token, newPassword)
}
