package com.group8.comp2300.presentation.screens.medical

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group8.comp2300.domain.model.medical.Medication
import com.group8.comp2300.domain.model.medical.MedicationCreateRequest
import com.group8.comp2300.domain.repository.medical.MedicationDataRepository
import com.group8.comp2300.domain.repository.medical.SyncCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MedicationUiState(
    val isLoading: Boolean = false,
    val medications: List<Medication> = emptyList(),
    val error: String? = null,
)

class MedicationViewModel(
    private val medicationRepository: MedicationDataRepository,
    private val syncCoordinator: SyncCoordinator,
) : ViewModel() {
    private val _state = MutableStateFlow(MedicationUiState(isLoading = true))
    val state: StateFlow<MedicationUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching {
                syncCoordinator.refreshAuthenticatedData()
                medicationRepository.getMedications()
            }.onSuccess { medications ->
                _state.update { it.copy(isLoading = false, medications = medications) }
            }.onFailure { error ->
                _state.update { it.copy(isLoading = false, error = error.message ?: "Failed to load medications") }
            }
        }
    }

    fun saveMedication(request: MedicationCreateRequest, id: String? = null) {
        viewModelScope.launch {
            _state.update { it.copy(error = null) }
            runCatching {
                medicationRepository.saveMedication(request = request, id = id)
                medicationRepository.getMedications()
            }.onSuccess { medications ->
                _state.update { it.copy(medications = medications) }
            }.onFailure { error ->
                _state.update { it.copy(error = error.message ?: "Failed to save medication") }
            }
        }
    }

    fun deleteMedication(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(error = null) }
            runCatching {
                medicationRepository.deleteMedication(id)
                medicationRepository.getMedications()
            }.onSuccess { medications ->
                _state.update { it.copy(medications = medications) }
            }.onFailure { error ->
                _state.update { it.copy(error = error.message ?: "Failed to delete medication") }
            }
        }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }
}
