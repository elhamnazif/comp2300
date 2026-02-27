package com.group8.comp2300.presentation.screens.medical.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group8.comp2300.domain.model.medical.Appointment
import com.group8.comp2300.domain.model.medical.AppointmentRequest
import com.group8.comp2300.domain.model.medical.CalendarOverviewResponse
import com.group8.comp2300.domain.model.medical.MedicationLogRequest
import com.group8.comp2300.domain.model.medical.MoodEntryRequest
import com.group8.comp2300.domain.repository.MedicalRepository
import com.group8.comp2300.mock.sampleMedications
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

data class CalendarUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val appointments: List<Appointment> = emptyList(),
    val isMedicationTakenToday: Boolean = false,
    val overview: List<CalendarOverviewResponse> = emptyList(),
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
                // Determine current year/month
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                val year = now.year
                val month = now.month.number
                val dateString = "${now.year}-${
                    now.month.number.toString().padStart(
                    2,
                        '0',
                    )
                }-${now.day.toString().padStart(2, '0')}"

                val overviewDeferred = medicalRepository.getCalendarOverview(year, month)
                val domainAppointments = medicalRepository.getAppointments()
                val medicationAgendaDeferred = medicalRepository.getMedicationAgenda(dateString)

                val takenToday = medicationAgendaDeferred.any { it.status.name == "TAKEN" }

                _state.update {
                    it.copy(
                        isLoading = false,
                        overview = overviewDeferred,
                        appointments = domainAppointments,
                        isMedicationTakenToday = takenToday,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load calendar data")
                }
            }
        }
    }

    fun logMedication(medicationName: String, dosage: Int) {
        viewModelScope.launch {
            try {
                // Resolve medication ID from the medication name
                val medication = sampleMedications.find { it.name == medicationName }
                val medicationId = medication?.id ?: sampleMedications.firstOrNull()?.id
                    ?: throw IllegalStateException("No medications available")

                val request = MedicationLogRequest(
                    medicationId = medicationId,
                    status = "TAKEN",
                )
                medicalRepository.logMedication(request)
                // Update local state to reflect UI changes immediately
                _state.update { it.copy(isMedicationTakenToday = true) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun scheduleAppointment(doctorName: String, appointmentType: String, appointmentTimeMs: Long) {
        viewModelScope.launch {
            try {
                val request = AppointmentRequest(
                    title = "Appointment with $doctorName",
                    appointmentTime = appointmentTimeMs,
                    appointmentType = appointmentType,
                    doctorName = doctorName,
                )
                val newAppointment = medicalRepository.scheduleAppointment(request)

                // Update local state
                _state.update {
                    it.copy(appointments = it.appointments + newAppointment)
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun logMood(score: Int, tags: List<String>, symptoms: List<String>, notes: String) {
        viewModelScope.launch {
            try {
                val request = MoodEntryRequest(
                    moodScore = score,
                    tags = tags,
                    symptoms = symptoms,
                    notes = notes,
                )
                medicalRepository.logMood(request)
                // You could optionally refetch the calendar overview here
                loadInitialData()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }
}
