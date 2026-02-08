package com.group8.comp2300.presentation.ui.screens.profile

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group8.comp2300.domain.model.medical.LabResult
import com.group8.comp2300.domain.repository.AuthRepository
import com.group8.comp2300.domain.usecase.medical.GetRecentLabResultsUseCase
import com.group8.comp2300.presentation.util.DateFormatter
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val getRecentLabResultsUseCase: GetRecentLabResultsUseCase
) : ViewModel() {

    // INPUT: Explicit trigger for Pull-to-Refresh
    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1)

    // OUTPUT: The single source of truth
    val state: StateFlow<State> = combine(
        authRepository.currentUser,
        refreshTrigger.onStart { emit(Unit) } // Ensure it runs at least once on start
    ) { user, _ ->
        user
    }.flatMapLatest { user ->
        // flatMapLatest cancels the previous block if the User changes OR we Refresh
        if (user == null) {
            // Handle Logged Out State immediately
            flowOf(State(isLoading = false))
        } else {
            flow {
                // 1. Immediately emit user details (usually available locally/cached)
                val firstName = user.firstName
                val lastName = user.lastName
                val initials = listOfNotNull(firstName.firstOrNull(), lastName.firstOrNull())
                    .joinToString("") { it.uppercase() }

                val baseState = State(
                    isLoading = true, // Start loading the async parts (Lab Results)
                    userInitials = initials,
                    userName = "$firstName $lastName".trim(),
                    memberSince = user.createdAt.let { DateFormatter.formatMonthDayYearSuspend(it) }
                )
                emit(baseState)

                // 2. Fetch Async Data (Lab Results)
                try {
                    // This suspend call is now safe and won't block the UI
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
