package com.group8.comp2300.feature.auth

internal data class AuthErrorFlags(
    val isNetworkError: Boolean,
    val isInvalidOrExpiredToken: Boolean,
)

internal fun parseAuthError(exception: Throwable?): AuthErrorFlags {
    val exceptionName = exception?.let { it::class.simpleName }.orEmpty()
    val exceptionMessage = exception?.message.orEmpty()

    val isNetworkError = exceptionName.contains("Connect") ||
        exceptionName.contains("Socket") ||
        exceptionName.contains("Timeout") ||
        exceptionName.contains("UnknownHost") ||
        exceptionName.contains("EOF") ||
        exceptionMessage.contains("Failed to connect", ignoreCase = true) ||
        exceptionMessage.contains("Connection refused", ignoreCase = true) ||
        exceptionMessage.contains("unexpected end of stream", ignoreCase = true)

    val isInvalidOrExpiredToken = exceptionMessage.contains("invalid", ignoreCase = true) ||
        exceptionMessage.contains("expired", ignoreCase = true)

    return AuthErrorFlags(
        isNetworkError = isNetworkError,
        isInvalidOrExpiredToken = isInvalidOrExpiredToken,
    )
}
