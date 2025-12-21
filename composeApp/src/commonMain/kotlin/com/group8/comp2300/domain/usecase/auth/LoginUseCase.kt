package com.group8.comp2300.domain.usecase.auth

import com.group8.comp2300.domain.model.user.User
import com.group8.comp2300.domain.repository.AuthRepository

class LoginUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<User> {
        return repository.login(email, password)
    }
}
