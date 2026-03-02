package com.group8.comp2300.domain.usecase.auth

import com.group8.comp2300.domain.repository.AuthRepository

class ResendVerificationEmailUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String): Result<Unit> = repository.resendVerificationEmail(email)
}
