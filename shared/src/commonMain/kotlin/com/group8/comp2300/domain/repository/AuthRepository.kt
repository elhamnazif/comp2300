package com.group8.comp2300.domain.repository

import com.group8.comp2300.domain.model.session.AuthSession
import com.group8.comp2300.domain.model.user.Gender
import com.group8.comp2300.domain.model.user.SexualOrientation
import com.group8.comp2300.domain.model.user.User
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDate

interface AuthRepository {
    val session: StateFlow<AuthSession>

    suspend fun login(email: String, password: String): Result<User>

    suspend fun preregister(email: String, password: String): Result<String>

    suspend fun completeProfile(
        firstName: String,
        lastName: String,
        gender: Gender,
        sexualOrientation: SexualOrientation,
        dateOfBirth: LocalDate?,
    ): Result<User>

    suspend fun activateAccount(token: String): Result<Unit>
    suspend fun forgotPassword(email: String): Result<Unit>
    suspend fun resendVerificationEmail(email: String): Result<Unit>
    suspend fun resetPassword(token: String, newPassword: String): Result<Unit>

    suspend fun logout()
}
