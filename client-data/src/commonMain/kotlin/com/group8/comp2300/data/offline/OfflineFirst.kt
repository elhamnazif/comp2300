package com.group8.comp2300.data.offline

import com.group8.comp2300.data.remote.ApiException

/**
 * Returns true if this error is transient and the operation may succeed on retry.
 * Client errors (4xx) are NOT retryable — they indicate validation, auth, or not-found issues.
 */
fun Throwable.isRetryable(): Boolean = when (this) {
    is ApiException -> statusCode >= 500
    else -> true // network errors, timeouts, unknown — treat as transient
}

/**
 * Read: return cached data, attempt network refresh, fall back to stale cache on transient errors.
 * Non-retryable errors (e.g. 400, 401, 404) are propagated to the caller.
 */
suspend fun <T> cacheFirstRead(
    cached: () -> T,
    fetch: suspend () -> T,
    save: (T) -> Unit,
): T {
    val local = cached()
    return try {
        val remote = fetch()
        save(remote)
        remote
    } catch (e: Exception) {
        if (!e.isRetryable()) throw e
        local
    }
}
