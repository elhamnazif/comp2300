package com.group8.comp2300.service.auth

import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

interface VerificationRequestThrottle {
    fun canRequest(email: String): Boolean
    fun recordRequest(email: String)
    fun clearRequest(email: String)
}

class InMemoryVerificationRequestThrottle(
    private val rateLimitPeriod: Duration = 1.minutes,
    private val clock: Clock = Clock.System,
) : VerificationRequestThrottle {
    private val lastRequestByEmail = ConcurrentHashMap<String, Long>()

    override fun canRequest(email: String): Boolean {
        val lastRequestAt = lastRequestByEmail[email] ?: return true
        val elapsed = clock.now().toEpochMilliseconds() - lastRequestAt
        return elapsed >= rateLimitPeriod.inWholeMilliseconds
    }

    override fun recordRequest(email: String) {
        lastRequestByEmail[email] = clock.now().toEpochMilliseconds()
    }

    override fun clearRequest(email: String) {
        lastRequestByEmail.remove(email)
    }
}
