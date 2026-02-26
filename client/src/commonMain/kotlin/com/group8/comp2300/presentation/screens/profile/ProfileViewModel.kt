package com.group8.comp2300.presentation.screens.profile

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group8.comp2300.domain.model.medical.LabResult
import com.group8.comp2300.domain.repository.AuthRepository
import com.group8.comp2300.domain.usecase.medical.GetRecentLabResultsUseCase
import com.group8.comp2300.presentation.util.DateFormatter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    authRepository: AuthRepository,
    private val getRecentLabResultsUseCase: GetRecentLabResultsUseCase
) : ViewModel() {

    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1)

    val state: StateFlow<State> = combine(
        authRepository.currentUser,
        refreshTrigger.onStart { emit(Unit) } // Ensure it runs at least once on start
    ) { user, _ ->
        user
    }.flatMapLatest { user ->
        if (user == null) {
            flowOf(State(isLoading = false))
        } else {
            flow {
                val firstName = user.firstName
                val lastName = user.lastName
                val initials = listOfNotNull(firstName.firstOrNull(), lastName.firstOrNull())
                    .joinToString("") { it.uppercase() }

                val baseState = State(
                    isLoading = true,
                    userInitials = initials,
                    userName = "$firstName $lastName".trim(),
                    memberSince = user.createdAt.let { DateFormatter.formatMonthDayYearSuspend(it) }
                )
                emit(baseState)

                try {
                    val results = getRecentLabResultsUseCase()
                    emit(baseState.copy(isLoading = false, recentResults = results))
                } catch (_: Exception) {
                    emit(baseState.copy(isLoading = false, isError = true))
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = State(isLoading = true)
    )

    fun refresh() {
        viewModelScope.launch {
            refreshTrigger.emit(Unit)
        }
    }

    @Immutable
    data class State(
        val isLoading: Boolean = false,
        val isError: Boolean = false,
        val userInitials: String = "",
        val userName: String = "",
        val memberSince: String = "",
        val recentResults: List<LabResult> = emptyList()
    )
}
