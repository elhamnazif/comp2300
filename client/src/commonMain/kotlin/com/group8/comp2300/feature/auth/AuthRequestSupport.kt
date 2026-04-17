package com.group8.comp2300.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

internal interface AuthRequestState<T> {
    fun withRequestStatus(
        isLoading: Boolean,
        errorMessage: String? = null,
        errorMessageRes: StringResource? = null,
    ): T
}

internal fun authErrorFromException(
    exception: Throwable?,
    networkErrorRes: StringResource,
    invalidOrExpiredTokenRes: StringResource? = null,
): Pair<String?, StringResource?> {
    if (exception == null) return null to null

    val exceptionMessage = exception.message.orEmpty()
    val errorFlags = parseAuthError(exception)
    val fallbackRes = invalidOrExpiredTokenRes ?: networkErrorRes

    return when {
        errorFlags.isNetworkError -> null to networkErrorRes
        invalidOrExpiredTokenRes != null && errorFlags.isInvalidOrExpiredToken ->
            null to invalidOrExpiredTokenRes

        exceptionMessage.isNotBlank() && !exceptionMessage.contains("Exception") ->
            exceptionMessage to null

        else -> null to fallbackRes
    }
}

internal inline fun <State, ResultT> ViewModel.launchAuthRequest(
    state: MutableStateFlow<State>,
    noinline request: suspend () -> Result<ResultT>,
    crossinline onSuccess: (State, ResultT?) -> State,
    networkErrorRes: StringResource,
    invalidOrExpiredTokenRes: StringResource? = null,
) where State : AuthRequestState<State> {
    state.update { current -> current.withRequestStatus(isLoading = true) }

    viewModelScope.launch {
        val result = request()
        if (result.isSuccess) {
            state.update { current ->
                onSuccess(current, result.getOrNull()).withRequestStatus(isLoading = false)
            }
        } else {
            val (errorMessage, errorMessageRes) = authErrorFromException(
                exception = result.exceptionOrNull(),
                networkErrorRes = networkErrorRes,
                invalidOrExpiredTokenRes = invalidOrExpiredTokenRes,
            )
            state.update { current ->
                current.withRequestStatus(
                    isLoading = false,
                    errorMessage = errorMessage,
                    errorMessageRes = errorMessageRes,
                )
            }
        }
    }
}
