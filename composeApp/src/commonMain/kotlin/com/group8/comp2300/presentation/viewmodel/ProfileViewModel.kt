package com.group8.comp2300.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.group8.comp2300.domain.model.medical.LabResult
import com.group8.comp2300.mock.sampleResults
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ProfileUiState(
    val userInitials: String = "JP",
    val userName: String = "Vita User",
    val memberSince: String = "Member since 2024",
    val recentResults: List<LabResult> = emptyList()
)

class ProfileViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState(recentResults = sampleResults))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
}
