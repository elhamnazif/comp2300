package com.group8.comp2300.presentation.ui.screens.auth

import androidx.lifecycle.viewModelScope
import com.group8.comp2300.domain.model.user.Gender
import com.group8.comp2300.domain.model.user.SexualOrientation
import com.group8.comp2300.domain.model.user.User
import com.group8.comp2300.domain.repository.AuthRepository
import comp2300.i18n.generated.resources.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.group8.comp2300.domain.usecase.auth.LoginUseCase
import com.group8.comp2300.domain.usecase.auth.RegisterUseCase
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class RealAuthViewModel(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val authRepository: AuthRepository
) : AuthViewModel() {

    final override val state: StateFlow<State>
        field = MutableStateFlow(State())

    override val currentUser: StateFlow<User?> =
        authRepository.currentUser.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // --- Actions (Intents) ---

    override fun onEvent(event: AuthUiEvent) {
        when (event) {
            is AuthUiEvent.EmailChanged -> {
                val isValid = isValidEmail(event.email)
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
                val isValid = isValidPassword(event.password)
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

            is AuthUiEvent.FirstNameChanged -> state.update { it.copy(firstName = event.name) }

            is AuthUiEvent.LastNameChanged -> state.update { it.copy(lastName = event.name) }

            is AuthUiEvent.GenderChanged -> state.update { it.copy(gender = event.gender) }

            is AuthUiEvent.OrientationChanged ->
                state.update { it.copy(sexualOrientation = event.orientation) }

            is AuthUiEvent.DateOfBirthChanged ->
                state.update {
                    it.copy(dateOfBirth = event.dateMillis, showDatePicker = false)
                }

            is AuthUiEvent.ShowDatePicker ->
                state.update { it.copy(showDatePicker = event.show) }

            is AuthUiEvent.ToggleAuthMode ->
                state.update {
                    State(isRegistering = !it.isRegistering) // Reset form on switch
                }

            is AuthUiEvent.NextStep -> state.update { it.copy(step = 1) }

            is AuthUiEvent.PrevStep -> state.update { it.copy(step = 0) }

            is AuthUiEvent.Submit -> submitData(event.onSuccess)

            is AuthUiEvent.ClearError -> state.update { it.copy(errorMessage = null) }
        }
    }

    private fun submitData(onSuccess: () -> Unit) {
        state.update { it.copy(isLoading = true, errorMessage = null) }

        if (state.value.isRegistering) {
            performRegister(state.value, onSuccess)
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

    private fun performRegister(state: State, onSuccess: () -> Unit) {
        viewModelScope.launch {
            // Convert UI strings to enums, with defaults if empty
            val gender =
                if (state.gender.isNotEmpty()) {
                    Gender.fromDisplayName(state.gender) ?: Gender.PREFER_NOT_TO_SAY
                } else {
                    Gender.PREFER_NOT_TO_SAY
                }

            val orientation =
                if (state.sexualOrientation.isNotEmpty()) {
                    SexualOrientation.fromDisplayName(state.sexualOrientation)
                        ?: SexualOrientation.HETEROSEXUAL
                } else {
                    SexualOrientation.HETEROSEXUAL
                }

            val result =
                registerUseCase(
                    state.email,
                    state.password,
                    firstName = state.firstName,
                    lastName = state.lastName,
                    gender = gender,
                    sexualOrientation = orientation,
                    dateOfBirth =
                    state.dateOfBirth?.let {
                        Instant.fromEpochMilliseconds(it)
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                            .date
                    },
                )
            handleResult(result, onSuccess)
        }
    }

    private fun handleResult(result: Result<User>, onSuccess: () -> Unit) {
        if (result.isSuccess) {
            state.update { it.copy(isLoading = false) }
            onSuccess()
        } else {
            state.update {
                it.copy(
                    isLoading = false,
                    errorMessage = Res.string.auth_error_authentication_failed,
                )
            }
        }
    }

    private fun isValidEmail(email: String): Boolean = email.contains("@") && email.contains(".")
    private fun isValidPassword(password: String): Boolean = password.length >= 8

    override fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }

    override fun isGuest() = authRepository.isGuest()
}
