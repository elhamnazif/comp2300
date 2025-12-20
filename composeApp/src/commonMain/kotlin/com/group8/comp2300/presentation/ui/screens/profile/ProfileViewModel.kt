package com.group8.comp2300.presentation.ui.screens.profile

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.group8.comp2300.domain.model.medical.LabResult
import com.group8.comp2300.mock.sampleResults
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ProfileViewModel : ViewModel() {
    val state: StateFlow<State>
        field = MutableStateFlow(State(recentResults = sampleResults))

    @Immutable
    data class State(
        val userInitials: String = "",
        val userName: String = "",
        val memberSince: String = "",
        val recentResults: List<LabResult> = emptyList(),
    )
}
