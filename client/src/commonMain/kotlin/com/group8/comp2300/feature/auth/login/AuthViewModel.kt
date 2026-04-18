package com.group8.comp2300.feature.auth.login

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group8.comp2300.core.Validation
import com.group8.comp2300.domain.model.session.AuthSession
import com.group8.comp2300.domain.repository.AuthRepository
import com.group8.comp2300.domain.usecase.auth.LoginUseCase
import com.group8.comp2300.domain.usecase.auth.PreregisterUseCase
import com.group8.comp2300.feature.auth.parseAuthError
import comp2300.i18n.generated.resources.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

class AuthViewModel(
    private val loginUseCase: LoginUseCase,
    private val preregisterUseCase: PreregisterUseCase,
    private val authRepository: AuthRepository,
) : ViewModel() {
    val state: StateFlow<State>
        field = MutableStateFlow(State())

    val session: StateFlow<AuthSession> =
        authRepository.session.stateIn(viewModelScope, SharingStarted.Eagerly, authRepository.session.value)

    fun onEvent(event: AuthUiEvent) {
        when (event) {
            is AuthUiEvent.EmailChanged -> {
                val isValid = Validation.isValidEmail(event.email)
                state.update {
                    it.copy(
                        email = event.email,
                        emailError = if (isValid ||
                            event.email.isEmpty()
                        ) {
                            null
                        } else {
                            Res.string.auth_error_invalid_email
                        },
                    )
                }
            }

            is AuthUiEvent.PasswordChanged -> {
                val isValid = Validation.isValidPassword(event.password)
                state.update {
                    it.copy(
                        password = event.password,
                        passwordError = if (isValid || event.password.isEmpty()) {
                            null
                        } else {
                            Res.string.auth_error_password_too_short
                        },
                    )
                }
            }

            is AuthUiEvent.TogglePasswordVisibility -> {
                state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            }

            is AuthUiEvent.ToggleTerms -> {
                state.update { it.copy(termsAccepted = !it.termsAccepted) }
            }

            is AuthUiEvent.ToggleAuthMode -> {
                state.update { State(isRegistering = !it.isRegistering) }
            }

            is AuthUiEvent.Submit -> submitData(event.onSuccess)

            is AuthUiEvent.ClearError -> state.update { it.copy(errorMessage = null, errorMessageRes = null) }
        }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }

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

    private fun submitData(onSuccess: () -> Unit) {
        state.update { it.copy(isLoading = true, errorMessage = null, errorMessageRes = null) }

        if (state.value.isRegistering) {
            performPreregister(state.value, onSuccess)
        } else {
            performLogin(state.value, onSuccess)
        }
    }

    private fun performLogin(state: State, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = loginUseCase(state.email, state.password)
            handleResult(result, onSuccess)
        }
    }

    private fun performPreregister(formState: State, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = preregisterUseCase(formState.email, formState.password)
            if (result.isSuccess) {
                state.update { it.copy(isLoading = false) }
                onSuccess()
            } else {
                handlePreregisterError(result.exceptionOrNull())
            }
        }
    }

    private fun handlePreregisterError(exception: Throwable?) {
        val (errorText, errorRes) = categorizeError(exception)
        state.update {
            it.copy(
                isLoading = false,
                errorMessage = errorText,
                errorMessageRes = errorRes,
            )
        }
    }

    private fun handleResult(result: Result<com.group8.comp2300.domain.model.user.User>, onSuccess: () -> Unit) {
        if (result.isSuccess) {
            state.update { it.copy(isLoading = false) }
            onSuccess()
        } else {
            handleLoginError(result.exceptionOrNull())
        }
    }

    private fun handleLoginError(exception: Throwable?) {
        val (errorText, errorRes) = categorizeError(exception)
        state.update {
            it.copy(
                isLoading = false,
                errorMessage = errorText,
                errorMessageRes = errorRes,
            )
        }
    }

    private fun categorizeError(exception: Throwable?): Pair<String?, StringResource?> {
        val exceptionMessage = exception?.message.orEmpty()
        val errorFlags = parseAuthError(exception)

        return when {
            errorFlags.isNetworkError -> null to Res.string.auth_error_network
            exceptionMessage.isNotBlank() && !exceptionMessage.contains("Exception") -> exceptionMessage to null
            else -> null to Res.string.auth_error_authentication_failed
        }
    }
}
