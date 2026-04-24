package com.group8.comp2300.feature.profile

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.group8.comp2300.domain.usecase.auth.DeactivateAccountUseCase
import com.group8.comp2300.feature.auth.AuthRequestState
import com.group8.comp2300.feature.auth.launchAuthRequest
import comp2300.i18n.generated.resources.Res
import comp2300.i18n.generated.resources.auth_error_network
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.jetbrains.compose.resources.StringResource

class DeactivateAccountViewModel(private val deactivateAccountUseCase: DeactivateAccountUseCase) : ViewModel() {
    val state: StateFlow<State>
        field = MutableStateFlow(State())

    fun onEvent(event: Event) {
        when (event) {
            is Event.CurrentPasswordChanged -> {
                state.update {
                    it.copy(currentPassword = event.password, errorMessage = null, errorMessageRes = null)
                }
            }

            Event.TogglePasswordVisibility -> {
                state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            }

            Event.Continue -> {
                if (state.value.currentPassword.isNotBlank()) {
                    state.update { it.copy(confirmStep = true, errorMessage = null, errorMessageRes = null) }
                }
            }

            Event.EditPassword -> state.update {
                it.copy(confirmStep = false, errorMessage = null, errorMessageRes = null)
            }

            Event.ConfirmDeactivate -> submit()

            Event.ClearError -> state.update { it.copy(errorMessage = null, errorMessageRes = null) }
        }
    }

    @Immutable
    data class State(
        val currentPassword: String = "",
        val isPasswordVisible: Boolean = false,
        val confirmStep: Boolean = false,
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val errorMessageRes: StringResource? = null,
        val isComplete: Boolean = false,
    ) : AuthRequestState<State> {
        override fun withRequestStatus(isLoading: Boolean, errorMessage: String?, errorMessageRes: StringResource?) =
            copy(isLoading = isLoading, errorMessage = errorMessage, errorMessageRes = errorMessageRes)
    }

    sealed interface Event {
        data class CurrentPasswordChanged(val password: String) : Event
        data object TogglePasswordVisibility : Event
        data object Continue : Event
        data object EditPassword : Event
        data object ConfirmDeactivate : Event
        data object ClearError : Event
    }

    private fun submit() {
        if (state.value.currentPassword.isBlank()) return

        launchAuthRequest(
            state = state,
            request = { deactivateAccountUseCase(state.value.currentPassword) },
            onSuccess = { current, _ -> current.copy(isComplete = true) },
            networkErrorRes = Res.string.auth_error_network,
        )
    }
}
