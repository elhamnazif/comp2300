package com.group8.comp2300.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.group8.comp2300.domain.model.user.User
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for AuthViewModel to allow fake implementations for previews. This abstraction avoids
 * triggering viewModelScope/Dispatchers.Main in layout previews.
 */
abstract class AuthViewModel : ViewModel() {
    abstract val uiState: StateFlow<AuthUiState>
    abstract val currentUser: StateFlow<User?>

    abstract fun onEvent(event: AuthUiEvent)
    abstract fun logout()
    abstract fun isGuest(): Boolean
}
