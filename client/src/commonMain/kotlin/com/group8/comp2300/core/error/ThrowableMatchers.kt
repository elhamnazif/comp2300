package com.group8.comp2300.core.error

private val networkExceptionMarkers = listOf("Connect", "Socket", "Timeout", "UnknownHost", "EOF")
private val networkMessageMarkers = listOf("Failed to connect", "Connection refused", "unexpected end of stream")
private val timeoutMessageMarkers = listOf("timeout", "timed out")

internal fun Throwable?.isNetworkConnectivityError(): Boolean = generateSequence(this) { it.cause }
    .any { throwable ->
        val exceptionName = throwable::class.simpleName.orEmpty()
        val exceptionMessage = throwable.message.orEmpty()
        networkExceptionMarkers.any { marker -> exceptionName.contains(marker) } ||
            networkMessageMarkers.any { marker -> exceptionMessage.contains(marker, ignoreCase = true) }
    }

internal fun Throwable?.isTimeoutLikeError(): Boolean = generateSequence(this) { it.cause }
    .any { throwable ->
        val exceptionName = throwable::class.simpleName.orEmpty()
        val exceptionMessage = throwable.message.orEmpty()
        exceptionName.contains("Timeout", ignoreCase = true) ||
            timeoutMessageMarkers.any { marker -> exceptionMessage.contains(marker, ignoreCase = true) }
    }
