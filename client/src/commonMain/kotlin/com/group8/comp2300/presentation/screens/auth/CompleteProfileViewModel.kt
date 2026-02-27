package com.group8.comp2300.presentation.screens.auth

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group8.comp2300.domain.model.user.Gender
import com.group8.comp2300.domain.model.user.SexualOrientation
import com.group8.comp2300.domain.model.user.User
import com.group8.comp2300.domain.repository.AuthRepository
import com.group8.comp2300.domain.usecase.auth.CompleteProfileUseCase
import comp2300.i18n.generated.resources.Res
import comp2300.i18n.generated.resources.auth_error_network
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import kotlin.time.Instant

abstract class CompleteProfileViewModel : ViewModel() {
    abstract val state: StateFlow<State>
    abstract val currentUser: StateFlow<User?>
    abstract fun onEvent(event: Event)

    @Immutable
    data class State(
        val email: String = "",
        val firstName: String = "",
        val lastName: String = "",
        val dateOfBirth: Long? = null,
        val gender: String = "",
        val sexualOrientation: String = "",
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val errorMessageRes: StringResource? = null,
        val showDatePicker: Boolean = false,
        val isComplete: Boolean = false,
    ) {
        val isFormValid: Boolean
            get() = firstName.isNotBlank() && lastName.isNotBlank() && dateOfBirth != null

        fun getFormattedDate(): String = dateOfBirth?.let {
            val instant = Instant.fromEpochMilliseconds(it)
            val date = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
            "${date.day}/${date.month.number}/${date.year}"
        } ?: ""
    }

    sealed interface Event {
        data class FirstNameChanged(val name: String) : Event
        data class LastNameChanged(val name: String) : Event
        data class GenderChanged(val gender: String) : Event
        data class OrientationChanged(val orientation: String) : Event
        data class DateOfBirthChanged(val dateMillis: Long?) : Event
        data class ShowDatePicker(val show: Boolean) : Event
        data object Submit : Event
        data object ClearError : Event
    }
}

class RealCompleteProfileViewModel(
    private val completeProfileUseCase: CompleteProfileUseCase,
    authRepository: AuthRepository,
    initialEmail: String,
) : CompleteProfileViewModel() {

    override val state: MutableStateFlow<State> = MutableStateFlow(State(email = initialEmail))

    override val currentUser: StateFlow<User?> =
        authRepository.currentUser.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    override fun onEvent(event: Event) {
        when (event) {
            is Event.FirstNameChanged -> state.update { it.copy(firstName = event.name) }

            is Event.LastNameChanged -> state.update { it.copy(lastName = event.name) }

            is Event.GenderChanged -> state.update { it.copy(gender = event.gender) }

            is Event.OrientationChanged -> state.update { it.copy(sexualOrientation = event.orientation) }

            is Event.DateOfBirthChanged -> state.update {
                it.copy(dateOfBirth = event.dateMillis, showDatePicker = false)
            }

            is Event.ShowDatePicker -> state.update { it.copy(showDatePicker = event.show) }

            is Event.Submit -> submitProfile()

            is Event.ClearError -> state.update { it.copy(errorMessage = null, errorMessageRes = null) }
        }
    }

    private fun submitProfile() {
        val currentState = state.value
        if (!currentState.isFormValid) return

        state.update { it.copy(isLoading = true, errorMessage = null, errorMessageRes = null) }

        viewModelScope.launch {
            val gender = if (currentState.gender.isNotEmpty()) {
                Gender.fromDisplayName(currentState.gender) ?: Gender.PREFER_NOT_TO_SAY
            } else {
                Gender.PREFER_NOT_TO_SAY
            }

            val orientation = if (currentState.sexualOrientation.isNotEmpty()) {
                SexualOrientation.fromDisplayName(currentState.sexualOrientation) ?: SexualOrientation.HETEROSEXUAL
            } else {
                SexualOrientation.HETEROSEXUAL
            }

            val dateOfBirth = currentState.dateOfBirth?.let {
                Instant.fromEpochMilliseconds(it)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date
            }

            val result = completeProfileUseCase(
                firstName = currentState.firstName,
                lastName = currentState.lastName,
                gender = gender,
                sexualOrientation = orientation,
                dateOfBirth = dateOfBirth,
            )

            if (result.isSuccess) {
                state.update { it.copy(isLoading = false, isComplete = true) }
            } else {
                val exception = result.exceptionOrNull()
                val exceptionName = exception?.let { it::class.simpleName } ?: ""
                val exceptionMessage = exception?.message ?: ""

                val isNetworkError = exceptionName.contains("Connect") ||
                    exceptionName.contains("Socket") ||
                    exceptionName.contains("Timeout") ||
                    exceptionName.contains("UnknownHost") ||
                    exceptionMessage.contains("Failed to connect", ignoreCase = true) ||
                    exceptionMessage.contains("Connection refused", ignoreCase = true)

                val (errorText, errorRes) = when {
                    isNetworkError -> null to Res.string.auth_error_network

                    exceptionMessage.isNotBlank() && !exceptionMessage.contains("Exception") ->
                        exceptionMessage to null

                    else -> null to Res.string.auth_error_network
                }

                state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = errorText,
                        errorMessageRes = errorRes,
                    )
                }
            }
        }
    }
}
