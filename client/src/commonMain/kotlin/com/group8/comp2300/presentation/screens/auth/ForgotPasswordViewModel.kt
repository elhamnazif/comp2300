package com.group8.comp2300.presentation.screens.auth

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group8.comp2300.core.Validation
import com.group8.comp2300.domain.usecase.auth.ForgotPasswordUseCase
import comp2300.i18n.generated.resources.Res
import comp2300.i18n.generated.resources.auth_error_invalid_email
import comp2300.i18n.generated.resources.forgot_password_error_network
import comp2300.i18n.generated.resources.forgot_password_error_not_found
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

abstract class ForgotPasswordViewModel : ViewModel() {
    abstract val state: StateFlow<State>
    abstract fun onEvent(event: Event)

    @Immutable
    data class State(
        val email: String = "",
        val emailError: StringResource? = null,
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val errorMessageRes: StringResource? = null,
        val emailSent: Boolean = false,
        val code: String = "",
        val isCodeValid: Boolean = false,
    )

    sealed interface Event {
        data class EmailChanged(val email: String) : Event
        data object Submit : Event
        data object ClearError : Event
        data class CodeChanged(val code: String) : Event
    }
}

class RealForgotPasswordViewModel(private val forgotPasswordUseCase: ForgotPasswordUseCase) :
    ForgotPasswordViewModel() {
    override val state: MutableStateFlow<State> = MutableStateFlow(State())

    override fun onEvent(event: Event) {
        when (event) {
            is Event.EmailChanged -> {
                val isValid = Validation.isValidEmail(event.email)
                state.update {
                    it.copy(
                        email = event.email,
                        emailError = if (isValid || event.email.isEmpty()) {
                            null
                        } else {
                            Res.string.auth_error_invalid_email
                        },
                        errorMessage = null,
                        errorMessageRes = null,
                    )
                }
            }

            is Event.Submit -> submitEmail()

            is Event.ClearError -> state.update { it.copy(errorMessage = null, errorMessageRes = null) }

            is Event.CodeChanged -> {
                // Only allow digits and limit to 6 characters
                val digitsOnly = event.code.filter { char -> char.isDigit() }
                if (digitsOnly.length <= 6) {
                    state.update {
                        it.copy(
                            code = digitsOnly,
                            isCodeValid = digitsOnly.length == 6,
                        )
                    }
                }
            }
        }
    }

    private fun submitEmail() {
        val email = state.value.email.trim()
        if (email.isEmpty() || state.value.emailError != null) return

        state.update { it.copy(isLoading = true, errorMessage = null, errorMessageRes = null) }

        viewModelScope.launch {
            val result = forgotPasswordUseCase(email)
            if (result.isSuccess) {
                state.update { it.copy(isLoading = false, emailSent = true) }
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
            isNetworkError -> null to Res.string.forgot_password_error_network

            exceptionMessage.contains("not found", ignoreCase = true) ||
                exceptionMessage.contains("no account", ignoreCase = true) ->
                null to Res.string.forgot_password_error_not_found

            exceptionMessage.isNotBlank() && !exceptionMessage.contains("Exception") ->
                exceptionMessage to null

            else -> null to Res.string.forgot_password_error_network
        }
    }
}
