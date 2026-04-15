package com.group8.comp2300.presentation.screens.auth

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.group8.comp2300.domain.model.session.AuthSession
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.StringResource

/** Shared auth view-model contract for UI code and tests. */
abstract class AuthViewModel : ViewModel() {
    abstract val state: StateFlow<State>
    abstract val session: StateFlow<AuthSession>

    abstract fun onEvent(event: AuthUiEvent)

    abstract fun logout()

    @Immutable
    data class State(
        // Mode
        val isRegistering: Boolean = false,
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val errorMessageRes: StringResource? = null,

        // Credentials
        val email: String = "",
        val emailError: StringResource? = null,
        val password: String = "",
        val passwordError: StringResource? = null,
        val isPasswordVisible: Boolean = false,
        val termsAccepted: Boolean = false,
    ) {
        // Computed Property for Validation
        val isValid: Boolean
            get() =
                emailError == null &&
                    email.isNotBlank() &&
                    passwordError == null &&
                    password.isNotBlank() &&
                    (termsAccepted || !isRegistering)
    }

    sealed interface AuthUiEvent {
        data class EmailChanged(val email: String) : AuthUiEvent

        data class PasswordChanged(val password: String) : AuthUiEvent

        data object TogglePasswordVisibility : AuthUiEvent

        data object ToggleTerms : AuthUiEvent

        data object ToggleAuthMode : AuthUiEvent

        data class Submit(val onSuccess: () -> Unit) : AuthUiEvent

        data object ClearError : AuthUiEvent
    }
}
