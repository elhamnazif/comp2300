package com.group8.comp2300.presentation.screens.auth

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group8.comp2300.core.Validation
import com.group8.comp2300.domain.usecase.auth.ResetPasswordUseCase
import comp2300.i18n.generated.resources.Res
import comp2300.i18n.generated.resources.auth_error_password_too_short
import comp2300.i18n.generated.resources.reset_password_error_invalid_token
import comp2300.i18n.generated.resources.reset_password_error_mismatch
import comp2300.i18n.generated.resources.reset_password_error_network
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

abstract class ResetPasswordViewModel : ViewModel() {
    abstract val state: StateFlow<State>
    abstract fun onEvent(event: Event)

    @Immutable
    data class State(
        val token: String = "",
        val newPassword: String = "",
        val confirmPassword: String = "",
        val passwordError: StringResource? = null,
        val confirmPasswordError: StringResource? = null,
        val isPasswordVisible: Boolean = false,
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val errorMessageRes: StringResource? = null,
        val isPasswordReset: Boolean = false,
    ) {
        val isFormValid: Boolean
            get() = newPassword.isNotBlank() &&
                confirmPassword.isNotBlank() &&
                passwordError == null &&
                confirmPasswordError == null
    }

    sealed interface Event {
        data class PasswordChanged(val password: String) : Event
        data class ConfirmPasswordChanged(val password: String) : Event
        data object TogglePasswordVisibility : Event
        data object Submit : Event
        data object ClearError : Event
    }
}

class RealResetPasswordViewModel(private val resetPasswordUseCase: ResetPasswordUseCase, initialToken: String) :
    ResetPasswordViewModel() {
    override val state: MutableStateFlow<State> = MutableStateFlow(State(token = initialToken))

    override fun onEvent(event: Event) {
        when (event) {
            is Event.PasswordChanged -> {
                val isValid = Validation.isValidPassword(event.password)
                val passwordsMatch = event.password == state.value.confirmPassword
                state.update {
                    it.copy(
                        newPassword = event.password,
                        passwordError = if (isValid || event.password.isEmpty()) {
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

            is Event.TogglePasswordVisibility -> {
                state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            }

            is Event.Submit -> submitReset()

            is Event.ClearError -> state.update { it.copy(errorMessage = null, errorMessageRes = null) }
        }
    }

    private fun submitReset() {
        if (!state.value.isFormValid) return

        state.update { it.copy(isLoading = true, errorMessage = null, errorMessageRes = null) }

        viewModelScope.launch {
            val result = resetPasswordUseCase(state.value.token, state.value.newPassword)
            if (result.isSuccess) {
                state.update { it.copy(isLoading = false, isPasswordReset = true) }
            } else {
                val exception = result.exceptionOrNull()
                val (errorMsg, errorRes) = analyzeError(exception)
                state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = errorMsg,
                        errorMessageRes = errorRes,
                    )
                }
            }
        }
    }

    private fun analyzeError(exception: Throwable?): Pair<String?, StringResource?> {
        if (exception == null) return null to null

        val exceptionName = exception::class.simpleName ?: ""
        val exceptionMessage = exception.message ?: ""

        val isNetworkError = exceptionName.contains("Connect") ||
            exceptionName.contains("Socket") ||
            exceptionName.contains("Timeout") ||
            exceptionName.contains("UnknownHost") ||
            exceptionMessage.contains("Failed to connect", ignoreCase = true) ||
            exceptionMessage.contains("Connection refused", ignoreCase = true)

        return when {
            isNetworkError -> null to Res.string.reset_password_error_network

            exceptionMessage.contains("invalid", ignoreCase = true) ||
                exceptionMessage.contains("expired", ignoreCase = true) ->
                null to Res.string.reset_password_error_invalid_token

            exceptionMessage.isNotBlank() && !exceptionMessage.contains("Exception") ->
                exceptionMessage to null

            else -> null to Res.string.reset_password_error_invalid_token
        }
    }
}
