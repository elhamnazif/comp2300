package com.group8.comp2300.presentation.screens.auth

import androidx.lifecycle.viewModelScope
import com.group8.comp2300.core.Validation
import com.group8.comp2300.domain.model.user.User
import com.group8.comp2300.domain.repository.AuthRepository
import com.group8.comp2300.domain.usecase.auth.LoginUseCase
import com.group8.comp2300.domain.usecase.auth.PreregisterUseCase
import comp2300.i18n.generated.resources.Res
import comp2300.i18n.generated.resources.auth_error_authentication_failed
import comp2300.i18n.generated.resources.auth_error_invalid_email
import comp2300.i18n.generated.resources.auth_error_network
import comp2300.i18n.generated.resources.auth_error_password_too_short
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

class RealAuthViewModel(
    private val loginUseCase: LoginUseCase,
    private val preregisterUseCase: PreregisterUseCase,
    private val authRepository: AuthRepository,
) : AuthViewModel() {

    final override val state: StateFlow<State>
        field = MutableStateFlow(State())

    override val currentUser: StateFlow<User?> =
        authRepository.currentUser.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    override fun onEvent(event: AuthUiEvent) {
        when (event) {
            is AuthUiEvent.EmailChanged -> {
                val isValid = Validation.isValidEmail(event.email)
                state.update {
                    it.copy(
                        email = event.email,
                        emailError =
                        if (isValid || event.email.isEmpty()) {
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
                        passwordError =
                        if (isValid || event.password.isEmpty()) null else Res.string.auth_error_password_too_short,
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
                val exception = result.exceptionOrNull()
                handlePreregisterError(exception)
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

    private fun handleResult(result: Result<User>, onSuccess: () -> Unit) {
        if (result.isSuccess) {
            state.update { it.copy(isLoading = false) }
            onSuccess()
        } else {
            val exception = result.exceptionOrNull()
            handleLoginError(exception)
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
        val exceptionName = exception?.let { it::class.simpleName } ?: ""
        val exceptionMessage = exception?.message ?: ""

        val isNetworkError = exceptionName.contains("Connect") ||
            exceptionName.contains("Socket") ||
            exceptionName.contains("Timeout") ||
            exceptionName.contains("UnknownHost") ||
            exceptionName.contains("EOF") ||
            exceptionMessage.contains("Failed to connect", ignoreCase = true) ||
            exceptionMessage.contains("Connection refused", ignoreCase = true) ||
            exceptionMessage.contains("unexpected end of stream", ignoreCase = true)

        return when {
            isNetworkError -> null to Res.string.auth_error_network
            exceptionMessage.isNotBlank() && !exceptionMessage.contains("Exception") -> exceptionMessage to null
            else -> null to Res.string.auth_error_authentication_failed
        }
    }

    override fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }

    override fun isGuest() = authRepository.isGuest()
}
