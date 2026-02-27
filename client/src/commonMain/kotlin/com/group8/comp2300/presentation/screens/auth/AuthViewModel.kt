package com.group8.comp2300.presentation.screens.auth

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.group8.comp2300.domain.model.user.User
import com.group8.comp2300.presentation.util.DateFormatter
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.StringResource

/**
 * Interface for AuthViewModel to allow fake implementations for previews. This abstraction avoids triggering
 * viewModelScope/Dispatchers.Main in layout previews.
 */
abstract class AuthViewModel : ViewModel() {
    abstract val state: StateFlow<State>
    abstract val currentUser: StateFlow<User?>

    abstract fun onEvent(event: AuthUiEvent)

    abstract fun logout()

    abstract fun isGuest(): Boolean

    @Immutable
    data class State(
        // Mode
        val isRegistering: Boolean = false,
        val step: Int = 0,
        val isLoading: Boolean = false,
        val errorMessage: String? = null,

        // Step 1: Credentials
        val email: String = "",
        val emailError: StringResource? = null,
        val password: String = "",
        val passwordError: StringResource? = null,
        val isPasswordVisible: Boolean = false,
        val termsAccepted: Boolean = false,

        // Step 2: Personal Details
        val firstName: String = "",
        val lastName: String = "",
        val dateOfBirth: Long? = null, // Epoch millis
        val gender: String = "",
        val sexualOrientation: String = "",

        // UI Control
        val showDatePicker: Boolean = false,
    ) {
        // Computed Properties for Validation
        val isStep1Valid: Boolean
            get() =
                emailError == null &&
                    email.isNotBlank() &&
                    passwordError == null &&
                    password.isNotBlank() &&
                    termsAccepted

        val isStep2Valid: Boolean
            get() = firstName.isNotBlank() && lastName.isNotBlank() && dateOfBirth != null

        fun getFormattedDate(): String = dateOfBirth?.let { DateFormatter.formatDayMonthYear(it) } ?: ""
    }

    sealed interface AuthUiEvent {
        data class EmailChanged(val email: String) : AuthUiEvent

        data class PasswordChanged(val password: String) : AuthUiEvent

        data object TogglePasswordVisibility : AuthUiEvent

        data object ToggleTerms : AuthUiEvent

        data class FirstNameChanged(val name: String) : AuthUiEvent

        data class LastNameChanged(val name: String) : AuthUiEvent

        data class GenderChanged(val gender: String) : AuthUiEvent

        data class OrientationChanged(val orientation: String) : AuthUiEvent

        data class DateOfBirthChanged(val dateMillis: Long?) : AuthUiEvent

        data class ShowDatePicker(val show: Boolean) : AuthUiEvent

        data object ToggleAuthMode : AuthUiEvent

        data object NextStep : AuthUiEvent

        data object PrevStep : AuthUiEvent

        data class Submit(val onSuccess: () -> Unit) : AuthUiEvent

        data object ClearError : AuthUiEvent
    }
}
