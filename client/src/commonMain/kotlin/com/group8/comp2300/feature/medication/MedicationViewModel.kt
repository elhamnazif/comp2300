package com.group8.comp2300.feature.medication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group8.comp2300.domain.model.medical.*
import com.group8.comp2300.domain.repository.medical.MedicationDataRepository
import com.group8.comp2300.domain.repository.medical.OfflineSyncCoordinator
import com.group8.comp2300.domain.repository.medical.RoutineDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MedicationUiState(
    val isLoading: Boolean = false,
    val isMutating: Boolean = false,
    val medications: List<Medication> = emptyList(),
    val routines: List<Routine> = emptyList(),
    val linkedRoutineCounts: Map<String, Int> = emptyMap(),
    val error: String? = null,
)

class MedicationViewModel(
    private val medicationRepository: MedicationDataRepository,
    private val routineRepository: RoutineDataRepository,
    private val syncCoordinator: OfflineSyncCoordinator,
) : ViewModel() {
    val state: StateFlow<MedicationUiState>
        field: MutableStateFlow<MedicationUiState> = MutableStateFlow(MedicationUiState(isLoading = true))

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            state.update { it.copy(isLoading = true, error = null) }
            runCatching {
                syncCoordinator.refreshCaches()
                refreshState()
            }.onSuccess {
                state.update { current -> current.copy(isLoading = false) }
            }.onFailure { error ->
                state.update { it.copy(isLoading = false, error = error.message ?: "Failed to load medications") }
            }
        }
    }

    fun saveMedication(request: MedicationCreateRequest, id: String? = null, onSuccess: (Medication) -> Unit = {}) {
        viewModelScope.launch {
            state.update { it.copy(error = null, isMutating = true) }
            runCatching {
                val savedMedication = medicationRepository.saveMedication(request = request, id = id)
                refreshState()
                savedMedication
            }.onSuccess {
                state.update { current -> current.copy(error = null, isMutating = false) }
                onSuccess(it)
            }.onFailure { error ->
                state.update { it.copy(isMutating = false, error = error.message ?: "Failed to save medication") }
            }
        }
    }

    fun deleteMedication(id: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            state.update { it.copy(error = null, isMutating = true) }
            runCatching {
                removeMedicationFromAllRoutines(id)
                medicationRepository.deleteMedication(id)
                refreshState()
            }.onSuccess {
                state.update { current -> current.copy(error = null, isMutating = false) }
                onSuccess()
            }.onFailure { error ->
                state.update { it.copy(isMutating = false, error = error.message ?: "Failed to delete medication") }
            }
        }
    }

    fun saveRoutine(request: RoutineCreateRequest, id: String? = null, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            state.update { it.copy(error = null, isMutating = true) }
            runCatching {
                routineRepository.saveRoutine(request = request, id = id)
                refreshState()
            }.onFailure { error ->
                state.update { it.copy(isMutating = false, error = error.message ?: "Failed to create routine") }
            }.onSuccess {
                state.update { current -> current.copy(error = null, isMutating = false) }
                onSuccess()
            }
        }
    }

    fun unlinkMedicationFromRoutine(medicationId: String, routineId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            state.update { it.copy(error = null, isMutating = true) }
            runCatching {
                val routine = routineRepository.getRoutines().firstOrNull { it.id == routineId }
                    ?: error("Schedule not found")
                routineRepository.saveRoutine(
                    request = routine.toRequest(
                        medicationIds = routine.medicationIds.filterNot { it == medicationId },
                    ),
                    id = routine.id,
                )
                refreshState()
            }.onSuccess {
                state.update { current -> current.copy(error = null, isMutating = false) }
                onSuccess()
            }.onFailure { error ->
                state.update { it.copy(isMutating = false, error = error.message ?: "Failed to remove schedule link") }
            }
        }
    }

    fun deleteRoutine(id: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            state.update { it.copy(error = null, isMutating = true) }
            runCatching {
                routineRepository.deleteRoutine(id)
                refreshState()
            }.onSuccess {
                state.update { current -> current.copy(error = null, isMutating = false) }
                onSuccess()
            }.onFailure { error ->
                state.update { it.copy(isMutating = false, error = error.message ?: "Failed to delete schedule") }
            }
        }
    }

    fun dismissError() {
        state.update { it.copy(error = null) }
    }

    private suspend fun refreshState() {
        val medications = medicationRepository.getMedications()
        val routines = routineRepository.getRoutines()
        state.update {
            it.copy(
                medications = medications,
                routines = routines,
                linkedRoutineCounts = routines.filter { routine -> routine.status == RoutineStatus.ACTIVE }
                    .flatMap(Routine::medicationIds)
                    .groupingBy { medicationId -> medicationId }
                    .eachCount(),
            )
        }
    }

    private suspend fun removeMedicationFromAllRoutines(medicationId: String) {
        val routines = routineRepository.getRoutines()
        routines.filter { medicationId in it.medicationIds }.forEach { routine ->
            routineRepository.saveRoutine(
                request = routine.toRequest(medicationIds = routine.medicationIds.filterNot { it == medicationId }),
                id = routine.id,
            )
        }
    }
}

private fun Routine.toRequest(medicationIds: List<String> = this.medicationIds) = RoutineCreateRequest(
    name = name,
    timesOfDayMs = timesOfDayMs,
    repeatType = repeatType.name,
    daysOfWeek = daysOfWeek,
    startDate = startDate,
    endDate = endDate,
    hasReminder = hasReminder,
    reminderOffsetsMins = reminderOffsetsMins,
    status = status.name,
    medicationIds = medicationIds,
)
