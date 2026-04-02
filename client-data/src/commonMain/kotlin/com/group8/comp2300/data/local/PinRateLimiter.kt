package com.group8.comp2300.data.local

import com.russhwolf.settings.Settings
import kotlin.time.Clock

class PinRateLimiter(private val settings: Settings) {

    var failedAttempts: Int
        get() = settings.getInt(KeyFailedAttempts, 0)
        private set(value) = settings.putInt(KeyFailedAttempts, value)

    var lockoutUntilMs: Long
        get() = settings.getString(KeyLockoutUntil, "0").toLong()
        private set(value) = settings.putString(KeyLockoutUntil, value.toString())

    fun isLockedOut(): Boolean = lockoutUntilMs > 0L && Clock.System.now().toEpochMilliseconds() < lockoutUntilMs

    fun remainingLockoutMs(): Long = (lockoutUntilMs - Clock.System.now().toEpochMilliseconds()).coerceAtLeast(0L)

    fun recordFailedAttempt() {
        val attempts = failedAttempts + 1
        failedAttempts = attempts
        val duration = lockoutDuration(attempts)
        if (duration > 0L) {
            lockoutUntilMs = Clock.System.now().toEpochMilliseconds() + duration
        }
    }

    fun resetAttempts() {
        failedAttempts = 0
        lockoutUntilMs = 0L
    }

    private companion object {
        private const val KeyFailedAttempts = "pin_failed_attempts"
        private const val KeyLockoutUntil = "pin_lockout_until"

        private fun lockoutDuration(attempts: Int): Long = when {
            attempts < 6 -> 0L
            attempts < 10 -> 30_000L
            attempts < 15 -> 300_000L
            else -> 1_800_000L
        }
    }
}
