package com.group8.comp2300.feature.auth.emailverification

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group8.comp2300.domain.usecase.auth.ActivateAccountUseCase
import com.group8.comp2300.domain.usecase.auth.ResendVerificationEmailUseCase
import com.group8.comp2300.feature.auth.AuthRequestState
import com.group8.comp2300.feature.auth.launchAuthRequest
import comp2300.i18n.generated.resources.Res
import comp2300.i18n.generated.resources.email_verification_error_invalid_token
import comp2300.i18n.generated.resources.email_verification_error_network
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import kotlin.time.Duration.Companion.seconds

class EmailVerificationViewModel(
    private val activateAccountUseCase: ActivateAccountUseCase,
    private val resendVerificationEmailUseCase: ResendVerificationEmailUseCase,
    initialEmail: String,
) : ViewModel() {
    val state: StateFlow<State>
        field = MutableStateFlow(State(email = initialEmail))

    private var cooldownJob: Job? = null

    fun onEvent(event: Event) {
        when (event) {
            is Event.TokenChanged -> {
                state.update { it.copy(token = event.token, errorMessage = null, errorMessageRes = null) }
            }

            is Event.VerifyToken -> verifyToken()

            is Event.ResendEmail -> resendEmail()

            is Event.ClearError -> state.update { it.copy(errorMessage = null, errorMessageRes = null) }
        }
    }

    @Immutable
    data class State(
        val email: String = "",
        val token: String = "",
        val isLoading: Boolean = false,
        val isVerified: Boolean = false,
        val errorMessage: String? = null,
        val errorMessageRes: StringResource? = null,
        val canResend: Boolean = true,
        val resendCooldown: Int = 0, // seconds remaining
    ) : AuthRequestState<State> {
        override fun withRequestStatus(isLoading: Boolean, errorMessage: String?, errorMessageRes: StringResource?) =
            copy(isLoading = isLoading, errorMessage = errorMessage, errorMessageRes = errorMessageRes)
    }

    sealed interface Event {
        data class TokenChanged(val token: String) : Event
        data object VerifyToken : Event
        data object ResendEmail : Event
        data object ClearError : Event
    }

    private fun verifyToken() {
        val token = state.value.token.trim()
        if (token.isEmpty()) return

        launchAuthRequest(
            state = state,
            request = { activateAccountUseCase(token) },
            onSuccess = { current, _ ->
                current.copy(isVerified = true)
            },
            networkErrorRes = Res.string.email_verification_error_network,
            invalidOrExpiredTokenRes = Res.string.email_verification_error_invalid_token,
        )
    }

    private fun resendEmail() {
        if (!state.value.canResend) return

        val email = state.value.email.trim()
        if (email.isEmpty()) return

        launchAuthRequest(
            state = state,
            request = { resendVerificationEmailUseCase(email) },
            onSuccess = { current, _ ->
                startCooldown()
                current
            },
            networkErrorRes = Res.string.email_verification_error_network,
            invalidOrExpiredTokenRes = Res.string.email_verification_error_invalid_token,
        )
    }

    private fun startCooldown() {
        state.update { it.copy(canResend = false, resendCooldown = 60) }
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            repeat(60) { i ->
                delay(1.seconds)
                state.update { it.copy(resendCooldown = 59 - i) }
            }
            state.update { it.copy(canResend = true, resendCooldown = 0) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        cooldownJob?.cancel()
    }
}
