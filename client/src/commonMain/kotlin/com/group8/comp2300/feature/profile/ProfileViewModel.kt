package com.group8.comp2300.feature.profile

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group8.comp2300.core.format.DateFormatter
import com.group8.comp2300.domain.model.session.userOrNull
import com.group8.comp2300.domain.repository.AuthRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProfileViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1)

    val state: StateFlow<State> = combine(
        authRepository.session,
        refreshTrigger.onStart { emit(Unit) }, // Ensure it runs at least once on start
    ) { session, _ ->
        session.userOrNull
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
                    isSignedIn = true,
                    userInitials = initials,
                    userName = "$firstName $lastName".trim(),
                    memberSince = user.createdAt.let { DateFormatter.formatMonthDayYearSuspend(it) },
                    profileImageUrl = user.profileImageUrl,
                )
                emit(baseState)

                emit(baseState.copy(isLoading = false))
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = State(isLoading = true),
    )

    fun refresh() {
        viewModelScope.launch {
            refreshTrigger.emit(Unit)
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    @Immutable
    data class State(
        val isLoading: Boolean = false,
        val isSignedIn: Boolean = false,
        val userInitials: String = "",
        val userName: String = "",
        val memberSince: String = "",
        val profileImageUrl: String? = null,
    )
}
