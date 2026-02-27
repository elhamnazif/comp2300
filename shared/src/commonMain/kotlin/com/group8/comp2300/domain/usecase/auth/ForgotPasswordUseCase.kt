package com.group8.comp2300.domain.usecase.auth

import com.group8.comp2300.domain.repository.AuthRepository

class ForgotPasswordUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String): Result<Unit> = repository.forgotPassword(email)
}
