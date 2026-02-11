package com.group8.comp2300.domain.repository

import com.group8.comp2300.domain.model.user.Gender
import com.group8.comp2300.domain.model.user.SexualOrientation
import com.group8.comp2300.domain.model.user.User
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDate

interface AuthRepository {
    val currentUser: StateFlow<User?>

    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        gender: Gender,
        sexualOrientation: SexualOrientation,
        dateOfBirth: LocalDate?
    ): Result<User>

    suspend fun logout()
    fun isGuest(): Boolean
}
