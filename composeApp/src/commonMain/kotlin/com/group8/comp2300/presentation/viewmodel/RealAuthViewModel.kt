package com.group8.comp2300.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.group8.comp2300.domain.model.user.Gender
import com.group8.comp2300.domain.model.user.SexualOrientation
import com.group8.comp2300.domain.model.user.User
import com.group8.comp2300.domain.repository.AuthRepository
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime

class RealAuthViewModel(private val authRepository: AuthRepository) : AuthViewModel() {

    // 1. Single source of truth for UI State
    private val _uiState = MutableStateFlow(AuthUiState())
    override val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    override val currentUser: StateFlow<User?> =
        authRepository.currentUser.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // --- Actions (Intents) ---

    override fun onEvent(event: AuthUiEvent) {
        when (event) {
            is AuthUiEvent.EmailChanged -> {
                val isValid = isValidEmail(event.email)
                _uiState.update {
                    it.copy(
                        email = event.email,
                        emailError =
                            if (isValid || event.email.isEmpty()) null
                            else "Invalid email format"
                    )
                }
            }

            is AuthUiEvent.PasswordChanged -> {
                val isValid = isValidPassword(event.password)
                _uiState.update {
                    it.copy(
                        password = event.password,
                        passwordError =
                            if (isValid || event.password.isEmpty()) null else "Min 8 chars"
                    )
                }
            }

            is AuthUiEvent.TogglePasswordVisibility -> {
                _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            }

            is AuthUiEvent.ToggleTerms -> {
                _uiState.update { it.copy(termsAccepted = !it.termsAccepted) }
            }

            is AuthUiEvent.FirstNameChanged -> _uiState.update { it.copy(firstName = event.name) }
            is AuthUiEvent.LastNameChanged -> _uiState.update { it.copy(lastName = event.name) }
            is AuthUiEvent.GenderChanged -> _uiState.update { it.copy(gender = event.gender) }
            is AuthUiEvent.OrientationChanged ->
                _uiState.update { it.copy(sexualOrientation = event.orientation) }

            is AuthUiEvent.DateOfBirthChanged ->
                _uiState.update {
                    it.copy(dateOfBirth = event.dateMillis, showDatePicker = false)
                }

            is AuthUiEvent.ShowDatePicker ->
                _uiState.update { it.copy(showDatePicker = event.show) }

            is AuthUiEvent.ToggleAuthMode ->
                _uiState.update {
                    AuthUiState(isRegistering = !it.isRegistering) // Reset form on switch
                }

            is AuthUiEvent.NextStep -> _uiState.update { it.copy(step = 1) }
            is AuthUiEvent.PrevStep -> _uiState.update { it.copy(step = 0) }
            is AuthUiEvent.Submit -> submitData(event.onSuccess)
            is AuthUiEvent.ClearError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun submitData(onSuccess: () -> Unit) {
        val state = _uiState.value
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        if (state.isRegistering) {
            performRegister(state, onSuccess)
        } else {
            performLogin(state, onSuccess)
        }
    }

    private fun performLogin(state: AuthUiState, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = authRepository.login(state.email, state.password)
            handleResult(result, onSuccess)
        }
    }

    private fun performRegister(state: AuthUiState, onSuccess: () -> Unit) {
        viewModelScope.launch {
            // Convert UI strings to enums, with defaults if empty
            val gender =
                if (state.gender.isNotEmpty())
                    Gender.fromDisplayName(state.gender) ?: Gender.PREFER_NOT_TO_SAY
                else Gender.PREFER_NOT_TO_SAY

            val orientation =
                if (state.sexualOrientation.isNotEmpty())
                    SexualOrientation.fromDisplayName(state.sexualOrientation)
                        ?: SexualOrientation.HETEROSEXUAL
                else SexualOrientation.HETEROSEXUAL

            val result =
                authRepository.register(
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
                        }
                )
            handleResult(result, onSuccess)
        }
    }

    private fun handleResult(result: Result<User>, onSuccess: () -> Unit) {
        if (result.isSuccess) {
            _uiState.update { it.copy(isLoading = false) }
            onSuccess()
        } else {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Authentication failed"
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

// Sealed Interface for UI Events
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

data class AuthUiState(
    // Mode
    val isRegistering: Boolean = false,
    val step: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,

    // Step 1: Credentials
    val email: String = "",
    val emailError: String? = null,
    val password: String = "",
    val passwordError: String? = null,
    val isPasswordVisible: Boolean = false,
    val termsAccepted: Boolean = false,

    // Step 2: Personal Details
    val firstName: String = "",
    val lastName: String = "",
    val dateOfBirth: Long? = null, // Epoch millis
    val gender: String = "",
    val sexualOrientation: String = "",

    // UI Control
    val showDatePicker: Boolean = false
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

    // Formatters (KMP Friendly)
    fun getFormattedDate(): String {
        if (dateOfBirth == null) return ""

        val instant = Instant.fromEpochMilliseconds(dateOfBirth)
        val date = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date

        // Manual formatting to DD/MM/YYYY to avoid java.time.format dependency
        // TODO: Make this multiplatform
        val day = date.day.toString().padStart(2, '0')
        val month = date.month.number.toString().padStart(2, '0')

        return "$day/$month/${date.year}"
    }
}
