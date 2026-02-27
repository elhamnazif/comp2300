package com.group8.comp2300.presentation.screens.auth

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group8.comp2300.domain.usecase.auth.ActivateAccountUseCase
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

abstract class EmailVerificationViewModel : ViewModel() {
    abstract val state: StateFlow<State>
    abstract fun onEvent(event: Event)

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
    )

    sealed interface Event {
        data class TokenChanged(val token: String) : Event
        data object VerifyToken : Event
        data object ResendEmail : Event
        data object ClearError : Event
    }
}

class RealEmailVerificationViewModel(
    private val activateAccountUseCase: ActivateAccountUseCase,
    initialEmail: String,
) : EmailVerificationViewModel() {
    override val state: MutableStateFlow<State> = MutableStateFlow(State(email = initialEmail))

    private var cooldownJob: Job? = null

    override fun onEvent(event: Event) {
        when (event) {
            is Event.TokenChanged -> {
                state.update { it.copy(token = event.token, errorMessage = null, errorMessageRes = null) }
            }

            is Event.VerifyToken -> verifyToken()

            is Event.ResendEmail -> resendEmail()

            is Event.ClearError -> state.update { it.copy(errorMessage = null, errorMessageRes = null) }
        }
    }

    private fun verifyToken() {
        val token = state.value.token.trim()
        if (token.isEmpty()) return

        state.update { it.copy(isLoading = true, errorMessage = null, errorMessageRes = null) }

        viewModelScope.launch {
            val result = activateAccountUseCase(token)
            if (result.isSuccess) {
                state.update { it.copy(isLoading = false, isVerified = true) }
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

    private fun resendEmail() {
        if (!state.value.canResend) return

        // Start cooldown timer
        startCooldown()

        // In a real app, you would call a resend API here
        // For now, we just start the cooldown
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
            isNetworkError -> null to Res.string.email_verification_error_network

            exceptionMessage.contains("invalid", ignoreCase = true) ||
                exceptionMessage.contains("expired", ignoreCase = true) ->
                null to Res.string.email_verification_error_invalid_token

            exceptionMessage.isNotBlank() && !exceptionMessage.contains("Exception") ->
                exceptionMessage to null

            else -> null to Res.string.email_verification_error_invalid_token
        }
    }

    override fun onCleared() {
        super.onCleared()
        cooldownJob?.cancel()
    }
}
