package com.group8.comp2300.presentation.screens.medical.medication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group8.comp2300.domain.model.medical.Medication
import com.group8.comp2300.domain.model.medical.MedicationCreateRequest
import com.group8.comp2300.domain.model.medical.Routine
import com.group8.comp2300.domain.model.medical.RoutineCreateRequest
import com.group8.comp2300.domain.model.medical.RoutineStatus
import com.group8.comp2300.domain.repository.medical.MedicationDataRepository
import com.group8.comp2300.domain.repository.medical.RoutineDataRepository
import com.group8.comp2300.domain.repository.medical.SyncCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MedicationUiState(
    val isLoading: Boolean = false,
    val medications: List<Medication> = emptyList(),
    val routines: List<Routine> = emptyList(),
    val linkedRoutineCounts: Map<String, Int> = emptyMap(),
    val error: String? = null,
)

class MedicationViewModel(
    private val medicationRepository: MedicationDataRepository,
    private val routineRepository: RoutineDataRepository,
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
                refreshState()
            }.onSuccess {
                _state.update { current -> current.copy(isLoading = false) }
            }.onFailure { error ->
                _state.update { it.copy(isLoading = false, error = error.message ?: "Failed to load medications") }
            }
        }
    }

    fun saveMedication(request: MedicationCreateRequest, id: String? = null, onSuccess: (Medication) -> Unit = {}) {
        viewModelScope.launch {
            _state.update { it.copy(error = null) }
            runCatching {
                val savedMedication = medicationRepository.saveMedication(request = request, id = id)
                refreshState()
                savedMedication
            }.onSuccess {
                _state.update { current -> current.copy(error = null) }
                onSuccess(it)
            }.onFailure { error ->
                _state.update { it.copy(error = error.message ?: "Failed to save medication") }
            }
        }
    }

    fun deleteMedication(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(error = null) }
            runCatching {
                removeMedicationFromAllRoutines(id)
                medicationRepository.deleteMedication(id)
                refreshState()
            }.onSuccess {
                _state.update { current -> current.copy(error = null) }
            }.onFailure { error ->
                _state.update { it.copy(error = error.message ?: "Failed to delete medication") }
            }
        }
    }

    fun updateRoutineLinks(medicationId: String, selectedRoutineIds: Set<String>) {
        viewModelScope.launch {
            _state.update { it.copy(error = null) }
            runCatching {
                val routines = routineRepository.getRoutines()
                routines.forEach { routine ->
                    val currentlyLinked = medicationId in routine.medicationIds
                    val shouldBeLinked = routine.id in selectedRoutineIds
                    if (currentlyLinked != shouldBeLinked) {
                        routineRepository.saveRoutine(
                            request = routine.toRequest(
                                medicationIds =
                                if (shouldBeLinked) {
                                    routine.medicationIds + medicationId
                                } else {
                                    routine.medicationIds.filterNot { it == medicationId }
                                },
                            ),
                            id = routine.id,
                        )
                    }
                }
                refreshState()
            }.onFailure { error ->
                _state.update { it.copy(error = error.message ?: "Failed to update routine links") }
            }
        }
    }

    fun saveRoutine(request: RoutineCreateRequest, id: String? = null, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _state.update { it.copy(error = null) }
            runCatching {
                routineRepository.saveRoutine(request = request, id = id)
                refreshState()
                onSuccess()
            }.onFailure { error ->
                _state.update { it.copy(error = error.message ?: "Failed to create routine") }
            }
        }
    }

    fun unlinkMedicationFromRoutine(medicationId: String, routineId: String) {
        viewModelScope.launch {
            _state.update { it.copy(error = null) }
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
            }.onFailure { error ->
                _state.update { it.copy(error = error.message ?: "Failed to remove schedule link") }
            }
        }
    }

    fun deleteRoutine(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(error = null) }
            runCatching {
                routineRepository.deleteRoutine(id)
                refreshState()
            }.onFailure { error ->
                _state.update { it.copy(error = error.message ?: "Failed to delete schedule") }
            }
        }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    private suspend fun refreshState() {
        val medications = medicationRepository.getMedications()
        val routines = routineRepository.getRoutines()
        _state.update {
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
