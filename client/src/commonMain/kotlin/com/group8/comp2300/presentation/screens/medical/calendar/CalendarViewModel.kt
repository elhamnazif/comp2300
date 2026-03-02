package com.group8.comp2300.presentation.screens.medical.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group8.comp2300.domain.model.medical.Appointment
import com.group8.comp2300.domain.model.medical.AppointmentRequest
import com.group8.comp2300.domain.model.medical.CalendarOverviewResponse
import com.group8.comp2300.domain.model.medical.Medication
import com.group8.comp2300.domain.model.medical.MedicationCreateRequest
import com.group8.comp2300.domain.model.medical.MedicationLogRequest
import com.group8.comp2300.domain.model.medical.MedicationLogStatus
import com.group8.comp2300.domain.model.medical.MoodEntryRequest
import com.group8.comp2300.domain.repository.MedicalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

data class CalendarUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val appointments: List<Appointment> = emptyList(),
    val isMedicationTakenToday: Boolean = false,
    val overview: List<CalendarOverviewResponse> = emptyList(),
    val medications: List<Medication> = emptyList(),
)

class CalendarViewModel(private val medicalRepository: MedicalRepository) : ViewModel() {

    private val _state = MutableStateFlow(CalendarUiState())
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                val year = now.year
                val month = now.month.number
                val dateString = "${now.year}-${
                    now.month.number.toString().padStart(2, '0')
                }-${now.day.toString().padStart(2, '0')}"

                val overview = medicalRepository.getCalendarOverview(year, month)
                val domainAppointments = medicalRepository.getAppointments()
                val medicationAgenda = medicalRepository.getMedicationAgenda(dateString)
                val medications = try {
                    medicalRepository.getUserMedications()
                } catch (e: Exception) {
                    co.touchlab.kermit.Logger.w(e) { "Failed to load medications" }
                    emptyList()
                }

                val takenToday = medicationAgenda.any { it.status.name == "TAKEN" }

                _state.update {
                    it.copy(
                        isLoading = false,
                        overview = overview,
                        appointments = domainAppointments,
                        isMedicationTakenToday = takenToday,
                        medications = medications,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load calendar data")
                }
            }
        }
    }

    fun loadOverviewForMonth(year: Int, month: Int) {
        viewModelScope.launch {
            try {
                val overview = medicalRepository.getCalendarOverview(year, month)
                _state.update { it.copy(overview = overview) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun createMedication(request: MedicationCreateRequest) {
        viewModelScope.launch {
            _state.update { it.copy(error = null) }
            try {
                val medication = medicalRepository.createMedication(request)
                _state.update { it.copy(medications = it.medications + medication) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun logMedication(medicationId: String) {
        viewModelScope.launch {
            _state.update { it.copy(error = null) }
            try {
                val request = MedicationLogRequest(
                    medicationId = medicationId,
                    status = MedicationLogStatus.TAKEN.name,
                )
                medicalRepository.logMedication(request)
                _state.update { it.copy(isMedicationTakenToday = true) }
                loadInitialData()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun scheduleAppointment(doctorName: String, appointmentType: String, appointmentTimeMs: Long) {
        viewModelScope.launch {
            _state.update { it.copy(error = null) }
            try {
                val serverType = mapAppointmentType(appointmentType)
                val request = AppointmentRequest(
                    title = "Appointment with $doctorName",
                    appointmentTime = appointmentTimeMs,
                    appointmentType = serverType,
                    doctorName = doctorName,
                )
                medicalRepository.scheduleAppointment(request)
                loadInitialData()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun logMood(score: Int, tags: List<String>, symptoms: List<String>, notes: String) {
        viewModelScope.launch {
            _state.update { it.copy(error = null) }
            try {
                val request = MoodEntryRequest(
                    moodScore = score,
                    tags = tags,
                    symptoms = symptoms,
                    notes = notes,
                )
                medicalRepository.logMood(request)
                loadInitialData()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    companion object {
        fun mapAppointmentType(displayType: String): String = when (displayType.lowercase()) {
            "consultation" -> "CONSULTATION"
            "lab work", "labwork" -> "LAB_TEST"
            "follow-up", "follow up", "followup" -> "FOLLOW_UP"
            "checkup", "check-up" -> "CHECKUP"
            "screening" -> "SCREENING"
            "vaccination" -> "VACCINATION"
            "emergency" -> "EMERGENCY"
            else -> "OTHER"
        }
    }
}
