package com.group8.comp2300.domain.usecase.auth

import com.group8.comp2300.domain.repository.AuthRepository

class ChangePasswordUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(currentPassword: String, newPassword: String): Result<Unit> =
        repository.changePassword(currentPassword, newPassword)
}
