package com.group8.comp2300.core.security.pin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group8.comp2300.data.local.PinDataSource
import com.group8.comp2300.data.local.PinRateLimiter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PinLockViewModel(private val pinDataSource: PinDataSource, private val rateLimiter: PinRateLimiter) :
    ViewModel() {

    val isLocked: StateFlow<Boolean>
        field = MutableStateFlow(true)

    /** null = not yet checked, true = PIN exists, false = no PIN set */
    val isPinSet: StateFlow<Boolean?>
        field = MutableStateFlow(null)

    val error: StateFlow<String?>
        field = MutableStateFlow(null)

    init {
        viewModelScope.launch {
            val hasPin = pinDataSource.isPinSet()
            isPinSet.value = hasPin
            if (!hasPin) {
                isLocked.value = false
            } else if (rateLimiter.isLockedOut()) {
                startLockoutTicker()
            }
        }
    }

    fun onPinEntered(pin: String) {
        if (rateLimiter.isLockedOut()) {
            updateLockoutError()
            return
        }
        viewModelScope.launch {
            if (pinDataSource.verifyPin(pin)) {
                rateLimiter.resetAttempts()
                isLocked.value = false
            } else {
                rateLimiter.recordFailedAttempt()
                if (rateLimiter.isLockedOut()) {
                    startLockoutTicker()
                }
                error.value = "Incorrect PIN"
            }
        }
    }

    fun clearError() {
        error.value = null
    }

    fun onBiometricUnlock() {
        rateLimiter.resetAttempts()
        isLocked.value = false
    }

    private fun updateLockoutError() {
        val remaining = rateLimiter.remainingLockoutMs()
        if (remaining <= 0L) {
            error.value = null
            return
        }
        val seconds = (remaining / 1000L).toInt().coerceAtLeast(1)
        error.value = if (seconds >= 60) {
            "Try again in ${seconds / 60}m ${seconds % 60}s"
        } else {
            "Try again in ${seconds}s"
        }
    }

    private fun startLockoutTicker() {
        viewModelScope.launch {
            while (rateLimiter.isLockedOut()) {
                updateLockoutError()
                delay(1000L)
            }
            error.value = null
        }
    }
}
