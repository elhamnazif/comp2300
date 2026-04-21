package com.group8.comp2300.core.security.pin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group8.comp2300.data.local.LocalAuthSettingsDataSource
import com.group8.comp2300.data.local.PinDataSource
import com.group8.comp2300.data.local.PinRateLimiter
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PinLockViewModel(
    private val pinDataSource: PinDataSource,
    private val rateLimiter: PinRateLimiter,
    localAuthSettingsDataSource: LocalAuthSettingsDataSource,
) : ViewModel() {

    val isLocked: StateFlow<Boolean>
        field = MutableStateFlow(true)

    val error: StateFlow<String?>
        field = MutableStateFlow(null)

    init {
        viewModelScope.launch {
            var initialized = false
            combine(pinDataSource.pinSet, localAuthSettingsDataSource.state) { hasPin, localAuthSettings ->
                hasPin && localAuthSettings.appLockEnabled
            }.collect { shouldRequireLock ->
                when {
                    !shouldRequireLock -> {
                        rateLimiter.resetAttempts()
                        error.value = null
                        isLocked.value = false
                    }

                    !initialized -> {
                        isLocked.value = true
                        if (rateLimiter.isLockedOut()) {
                            startLockoutTicker()
                        }
                    }
                }
                initialized = true
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
