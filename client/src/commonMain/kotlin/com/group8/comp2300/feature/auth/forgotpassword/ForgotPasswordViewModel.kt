package com.group8.comp2300.feature.auth.forgotpassword

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.group8.comp2300.core.Validation
import com.group8.comp2300.domain.usecase.auth.ForgotPasswordUseCase
import com.group8.comp2300.feature.auth.AuthRequestState
import com.group8.comp2300.feature.auth.components.sanitizeVerificationCode
import com.group8.comp2300.feature.auth.launchAuthRequest
import comp2300.i18n.generated.resources.Res
import comp2300.i18n.generated.resources.auth_error_invalid_email
import comp2300.i18n.generated.resources.forgot_password_error_network
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.jetbrains.compose.resources.StringResource

class ForgotPasswordViewModel(private val forgotPasswordUseCase: ForgotPasswordUseCase) : ViewModel() {
    val state: StateFlow<State>
        field = MutableStateFlow(State())

    fun onEvent(event: Event) {
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
                val digitsOnly = sanitizeVerificationCode(event.code)
                state.update {
                    it.copy(
                        code = digitsOnly,
                        isCodeValid = digitsOnly.length == 6,
                    )
                }
            }
        }
    }

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
    ) : AuthRequestState<State> {
        override fun withRequestStatus(isLoading: Boolean, errorMessage: String?, errorMessageRes: StringResource?) =
            copy(isLoading = isLoading, errorMessage = errorMessage, errorMessageRes = errorMessageRes)
    }

    sealed interface Event {
        data class EmailChanged(val email: String) : Event
        data object Submit : Event
        data object ClearError : Event
        data class CodeChanged(val code: String) : Event
    }

    private fun submitEmail() {
        val email = state.value.email.trim()
        if (email.isEmpty() || state.value.emailError != null) return

        launchAuthRequest(
            state = state,
            request = { forgotPasswordUseCase(email) },
            onSuccess = { current, _ ->
                current.copy(emailSent = true)
            },
            networkErrorRes = Res.string.forgot_password_error_network,
        )
    }
}
