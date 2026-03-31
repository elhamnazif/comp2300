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

    val error: StateFlow<String?>
        field = MutableStateFlow(null)

    init {
        viewModelScope.launch {
            if (!pinDataSource.isPinSet()) {
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
