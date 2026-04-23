package com.group8.comp2300.feature.auth

import com.group8.comp2300.core.error.isNetworkConnectivityError

internal data class AuthErrorFlags(val isNetworkError: Boolean, val isInvalidOrExpiredToken: Boolean)

internal fun parseAuthError(exception: Throwable?): AuthErrorFlags {
    val exceptionMessage = exception?.message.orEmpty()
    val isNetworkError = exception.isNetworkConnectivityError()

    val isInvalidOrExpiredToken = exceptionMessage.contains("invalid", ignoreCase = true) ||
        exceptionMessage.contains("expired", ignoreCase = true)

    return AuthErrorFlags(
        isNetworkError = isNetworkError,
        isInvalidOrExpiredToken = isInvalidOrExpiredToken,
    )
}
