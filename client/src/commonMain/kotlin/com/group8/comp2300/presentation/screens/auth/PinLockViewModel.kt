package com.group8.comp2300.presentation.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group8.comp2300.data.local.PinDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PinLockViewModel(private val pinDataSource: PinDataSource) : ViewModel() {

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
            }
        }
    }

    fun onPinEntered(pin: String) {
        viewModelScope.launch {
            if (pinDataSource.verifyPin(pin)) {
                isLocked.value = false
            } else {
                error.value = "Incorrect PIN"
            }
        }
    }

    fun clearError() {
        error.value = null
    }
}
