package com.group8.comp2300.feature.profile

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.group8.comp2300.core.Validation
import com.group8.comp2300.domain.usecase.auth.ConfirmEmailChangeUseCase
import com.group8.comp2300.domain.usecase.auth.RequestEmailChangeUseCase
import com.group8.comp2300.feature.auth.AuthRequestState
import com.group8.comp2300.feature.auth.components.sanitizeVerificationCode
import com.group8.comp2300.feature.auth.launchAuthRequest
import comp2300.i18n.generated.resources.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.jetbrains.compose.resources.StringResource

class ChangeEmailViewModel(
    initialEmail: String,
    private val requestEmailChangeUseCase: RequestEmailChangeUseCase,
    private val confirmEmailChangeUseCase: ConfirmEmailChangeUseCase,
) : ViewModel() {
    val state: StateFlow<State>
        field = MutableStateFlow(State(currentEmail = initialEmail))

    fun onEvent(event: Event) {
        when (event) {
            is Event.NewEmailChanged -> {
                val normalized = event.email.trim()
                val isValid = Validation.isValidEmail(normalized)
                state.update {
                    it.copy(
                        newEmail = event.email,
                        newEmailError = if (normalized.isEmpty() || isValid) {
                            null
                        } else {
                            Res.string.auth_error_invalid_email
                        },
                        errorMessage = null,
                        errorMessageRes = null,
                    )
                }
            }

            is Event.CurrentPasswordChanged -> {
                state.update {
                    it.copy(currentPassword = event.password, errorMessage = null, errorMessageRes = null)
                }
            }

            is Event.CodeChanged -> {
                state.update {
                    it.copy(code = sanitizeVerificationCode(event.code), errorMessage = null, errorMessageRes = null)
                }
            }

            Event.TogglePasswordVisibility -> {
                state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            }

            Event.SubmitRequest -> submitRequest()

            Event.ConfirmCode -> confirmCode()

            Event.EditEmail -> {
                state.update {
                    it.copy(requestSent = false, code = "", errorMessage = null, errorMessageRes = null)
                }
            }

            Event.ClearError -> state.update { it.copy(errorMessage = null, errorMessageRes = null) }
        }
    }

    @Immutable
    data class State(
        val currentEmail: String = "",
        val newEmail: String = "",
        val currentPassword: String = "",
        val code: String = "",
        val newEmailError: StringResource? = null,
        val isPasswordVisible: Boolean = false,
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val errorMessageRes: StringResource? = null,
        val requestSent: Boolean = false,
        val requestedEmail: String = "",
        val isComplete: Boolean = false,
    ) : AuthRequestState<State> {
        override fun withRequestStatus(isLoading: Boolean, errorMessage: String?, errorMessageRes: StringResource?) =
            copy(isLoading = isLoading, errorMessage = errorMessage, errorMessageRes = errorMessageRes)

        val canSubmitRequest: Boolean
            get() = currentPassword.isNotBlank() && newEmail.isNotBlank() && newEmailError == null

        val canConfirmCode: Boolean
            get() = code.length == 6
    }

    sealed interface Event {
        data class NewEmailChanged(val email: String) : Event
        data class CurrentPasswordChanged(val password: String) : Event
        data class CodeChanged(val code: String) : Event
        data object TogglePasswordVisibility : Event
        data object SubmitRequest : Event
        data object ConfirmCode : Event
        data object EditEmail : Event
        data object ClearError : Event
    }

    private fun submitRequest() {
        if (!state.value.canSubmitRequest) return

        val pendingEmail = state.value.newEmail.trim()
        launchAuthRequest(
            state = state,
            request = { requestEmailChangeUseCase(state.value.currentPassword, pendingEmail) },
            onSuccess = { current, _ ->
                current.copy(requestSent = true, requestedEmail = pendingEmail, code = "")
            },
            networkErrorRes = Res.string.auth_error_network,
        )
    }

    private fun confirmCode() {
        if (!state.value.canConfirmCode) return

        launchAuthRequest(
            state = state,
            request = { confirmEmailChangeUseCase(state.value.code) },
            onSuccess = { current, _ -> current.copy(isComplete = true) },
            networkErrorRes = Res.string.auth_error_network,
            invalidOrExpiredTokenRes = Res.string.email_verification_error_invalid_token,
        )
    }
}
