package com.group8.comp2300.domain.usecase.auth

import com.group8.comp2300.domain.repository.AuthRepository

class PreregisterUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<String> =
        repository.preregister(email, password)
}
