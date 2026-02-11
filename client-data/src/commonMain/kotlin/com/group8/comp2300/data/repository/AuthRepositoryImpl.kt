package com.group8.comp2300.data.repository

import com.group8.comp2300.data.remote.ApiService
import com.group8.comp2300.domain.model.user.Gender
import com.group8.comp2300.domain.model.user.SexualOrientation
import com.group8.comp2300.domain.model.user.User
import com.group8.comp2300.domain.repository.AuthRepository
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalDate

class AuthRepositoryImpl(private val apiService: ApiService) : AuthRepository {
    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    override suspend fun login(email: String, password: String): Result<User> =
        if (email.isNotEmpty() && password.isNotEmpty()) {
            val user =
                User(
                    id = "user_123",
                    email = email,
                    firstName = "Jane",
                    lastName = "Doe",
                    gender = Gender.FEMALE,
                    sexualOrientation = SexualOrientation.HETEROSEXUAL,
                    dateOfBirth = LocalDate(2000, 1, 1)
                )
            _currentUser.value = user
            Result.success(user)
        } else {
            Result.failure(Exception("Invalid credentials"))
        }

    override suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        gender: Gender,
        sexualOrientation: SexualOrientation,
        dateOfBirth: LocalDate?
    ): Result<User> {
        val user =
            User(
                id = "user_new_${Clock.System.now()}",
                email = email,
                firstName = firstName,
                lastName = lastName,
                gender = gender,
                sexualOrientation = sexualOrientation,
                dateOfBirth = dateOfBirth
            )
        _currentUser.value = user
        return Result.success(user)
    }

    override suspend fun logout() {
        _currentUser.value = null
    }

    override fun isGuest(): Boolean = _currentUser.value == null
}
