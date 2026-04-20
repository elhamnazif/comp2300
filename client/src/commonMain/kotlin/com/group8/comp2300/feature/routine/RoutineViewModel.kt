package com.group8.comp2300.feature.routine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group8.comp2300.domain.model.medical.Medication
import com.group8.comp2300.domain.model.medical.MedicationStatus
import com.group8.comp2300.domain.model.medical.Routine
import com.group8.comp2300.domain.model.medical.RoutineCreateRequest
import com.group8.comp2300.domain.repository.medical.MedicationDataRepository
import com.group8.comp2300.domain.repository.medical.RoutineDataRepository
import com.group8.comp2300.domain.repository.medical.SyncCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RoutineUiState(
    val isLoading: Boolean = false,
    val isMutating: Boolean = false,
    val routines: List<Routine> = emptyList(),
    val medications: List<Medication> = emptyList(),
    val error: String? = null,
)

class RoutineViewModel(
    private val routineRepository: RoutineDataRepository,
    private val medicationRepository: MedicationDataRepository,
    private val syncCoordinator: SyncCoordinator,
) : ViewModel() {
    val state: StateFlow<RoutineUiState>
        field: MutableStateFlow<RoutineUiState> = MutableStateFlow(RoutineUiState(isLoading = true))

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            state.update { it.copy(isLoading = true, error = null) }
            runCatching {
                syncCoordinator.refreshAuthenticatedData()
                routineRepository.getRoutines() to medicationRepository.getMedications()
            }.onSuccess { (routines, medications) ->
                state.update {
                    it.copy(
                        isLoading = false,
                        routines = routines,
                        medications = medications.filter { med -> med.status == MedicationStatus.ACTIVE },
                    )
                }
            }.onFailure { error ->
                state.update { it.copy(isLoading = false, error = error.message ?: "Failed to load routines") }
            }
        }
    }

    fun saveRoutine(request: RoutineCreateRequest, id: String? = null, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            state.update { it.copy(error = null, isMutating = true) }
            runCatching {
                routineRepository.saveRoutine(request, id)
                routineRepository.getRoutines()
            }.onSuccess { routines ->
                state.update { it.copy(routines = routines, isMutating = false) }
                onSuccess()
            }.onFailure { error ->
                state.update { it.copy(isMutating = false, error = error.message ?: "Failed to save routine") }
            }
        }
    }

    fun deleteRoutine(id: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            state.update { it.copy(error = null, isMutating = true) }
            runCatching {
                routineRepository.deleteRoutine(id)
                routineRepository.getRoutines()
            }.onSuccess { routines ->
                state.update { it.copy(routines = routines, isMutating = false) }
                onSuccess()
            }.onFailure { error ->
                state.update { it.copy(isMutating = false, error = error.message ?: "Failed to delete routine") }
            }
        }
    }

    fun dismissError() {
        state.update { it.copy(error = null) }
    }
}
