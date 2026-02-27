package com.group8.comp2300.core

object Validation {
    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun isValidEmail(email: String): Boolean = emailRegex.matches(email)

    fun isValidPassword(password: String): Boolean = password.length >= 8

    fun validatePassword(password: String): PasswordValidationResult = when {
        password.length < 8 -> PasswordValidationResult.TooShort
        password.length > 72 -> PasswordValidationResult.TooLong
        !password.any { it.isDigit() } -> PasswordValidationResult.MissingDigit
        !password.any { it.isLetter() } -> PasswordValidationResult.MissingLetter
        else -> PasswordValidationResult.Valid
    }
}

enum class PasswordValidationResult {
    Valid,
    TooShort,
    MissingDigit,
    MissingLetter,
    TooLong,
}
