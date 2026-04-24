package com.group8.comp2300.feature.profile

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.group8.comp2300.core.Validation
import com.group8.comp2300.domain.usecase.auth.ChangePasswordUseCase
import com.group8.comp2300.feature.auth.AuthRequestState
import com.group8.comp2300.feature.auth.launchAuthRequest
import comp2300.i18n.generated.resources.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.jetbrains.compose.resources.StringResource

class ChangePasswordViewModel(private val changePasswordUseCase: ChangePasswordUseCase) : ViewModel() {
    val state: StateFlow<State>
        field = MutableStateFlow(State())

    fun onEvent(event: Event) {
        when (event) {
            is Event.CurrentPasswordChanged -> {
                state.update {
                    it.copy(currentPassword = event.password, errorMessage = null, errorMessageRes = null)
                }
            }

            is Event.NewPasswordChanged -> {
                val isValid = Validation.isValidPassword(event.password)
                val passwordsMatch = event.password == state.value.confirmPassword
                state.update {
                    it.copy(
                        newPassword = event.password,
                        newPasswordError = if (isValid || event.password.isEmpty()) {
                            null
                        } else {
                            Res.string.auth_error_password_too_short
                        },
                        confirmPasswordError = if (state.value.confirmPassword.isEmpty() || passwordsMatch) {
                            null
                        } else {
                            Res.string.reset_password_error_mismatch
                        },
                        errorMessage = null,
                        errorMessageRes = null,
                    )
                }
            }

            is Event.ConfirmPasswordChanged -> {
                val passwordsMatch = event.password == state.value.newPassword
                state.update {
                    it.copy(
                        confirmPassword = event.password,
                        confirmPasswordError = if (event.password.isEmpty() || passwordsMatch) {
                            null
                        } else {
                            Res.string.reset_password_error_mismatch
                        },
                        errorMessage = null,
                        errorMessageRes = null,
                    )
                }
            }

            Event.TogglePasswordVisibility -> {
                state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            }

            Event.Submit -> submit()
            Event.ClearError -> state.update { it.copy(errorMessage = null, errorMessageRes = null) }
        }
    }

    @Immutable
    data class State(
        val currentPassword: String = "",
        val newPassword: String = "",
        val confirmPassword: String = "",
        val newPasswordError: StringResource? = null,
        val confirmPasswordError: StringResource? = null,
        val isPasswordVisible: Boolean = false,
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val errorMessageRes: StringResource? = null,
        val isPasswordChanged: Boolean = false,
    ) : AuthRequestState<State> {
        override fun withRequestStatus(isLoading: Boolean, errorMessage: String?, errorMessageRes: StringResource?) =
            copy(isLoading = isLoading, errorMessage = errorMessage, errorMessageRes = errorMessageRes)

        val isFormValid: Boolean
            get() = currentPassword.isNotBlank() &&
                newPassword.isNotBlank() &&
                confirmPassword.isNotBlank() &&
                newPasswordError == null &&
                confirmPasswordError == null
    }

    sealed interface Event {
        data class CurrentPasswordChanged(val password: String) : Event
        data class NewPasswordChanged(val password: String) : Event
        data class ConfirmPasswordChanged(val password: String) : Event
        data object TogglePasswordVisibility : Event
        data object Submit : Event
        data object ClearError : Event
    }

    private fun submit() {
        if (!state.value.isFormValid) return

        launchAuthRequest(
            state = state,
            request = { changePasswordUseCase(state.value.currentPassword, state.value.newPassword) },
            onSuccess = { current, _ -> current.copy(isPasswordChanged = true) },
            networkErrorRes = Res.string.auth_error_network,
        )
    }
}
