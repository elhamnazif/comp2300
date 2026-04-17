package com.group8.comp2300.feature.booking

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group8.comp2300.domain.model.medical.AppointmentRequest
import com.group8.comp2300.domain.model.medical.Clinic
import com.group8.comp2300.domain.model.medical.ClinicFilters
import com.group8.comp2300.domain.repository.ClinicRepository
import com.group8.comp2300.domain.repository.medical.AppointmentDataRepository
import com.group8.comp2300.services.ClinicFilterService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*

class BookingViewModel(
    private val repository: ClinicRepository,
    private val clinicFilterService: ClinicFilterService,
    private val appointmentRepository: AppointmentDataRepository,
) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _filters = MutableStateFlow(ClinicFilters())
    val filters: StateFlow<ClinicFilters> = _filters

    private val _selectedClinic = MutableStateFlow<Clinic?>(null)
    val selectedClinic: StateFlow<Clinic?> = _selectedClinic

    private val _bookingState = MutableStateFlow(BookingState())
    val bookingState: StateFlow<BookingState> = _bookingState

    private val _bookingEvents = MutableSharedFlow<BookingEvent>()
    val bookingEvents = _bookingEvents.asSharedFlow()

    private val clinicsList = MutableStateFlow(repository.getAllClinics())

    val allClinics: List<Clinic> get() = clinicsList.value

    val filteredClinics: StateFlow<List<Clinic>> = combine(
        clinicsList,
        _searchQuery,
        _filters,
    ) { clinics, query, filters ->
        clinicFilterService.filterClinics(
            clinics = clinics,
            filters = filters,
            searchQuery = query,
        )
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.Eagerly,
        initialValue = clinicFilterService.filterClinics(repository.getAllClinics()),
    )

    fun selectClinic(clinic: Clinic?) {
        _selectedClinic.value = clinic
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateFilters(filters: ClinicFilters) {
        _filters.value = filters
    }

    fun clearFilters() {
        _filters.value = ClinicFilters()
    }

    fun getClinicById(id: String): Clinic? = repository.getClinicById(id)

    fun scheduleAppointment(clinic: Clinic, date: LocalDate, timeSlot: String, reason: String) {
        _bookingState.value = BookingState(isSubmitting = true)

        viewModelScope.launch {
            runCatching {
                appointmentRepository.scheduleAppointment(
                    AppointmentRequest(
                        title = "Appointment with ${clinic.name}",
                        appointmentTime = dateTimeFromSlot(date, timeSlot),
                        appointmentType = DEFAULT_APPOINTMENT_TYPE,
                        notes = reason.trim().ifBlank { null },
                        doctorName = clinic.name,
                    ),
                )
            }.onSuccess {
                _bookingState.value = BookingState()
                _bookingEvents.emit(BookingEvent.Submitted)
            }.onFailure { error ->
                _bookingState.value = BookingState(isSubmitting = false, errorMessage = error.message)
            }
        }
    }

    fun clearBookingError() {
        _bookingState.value = _bookingState.value.copy(errorMessage = null)
    }

    @Immutable
    data class State(val clinics: List<Clinic> = emptyList(), val selectedClinic: Clinic? = null)

    @Immutable
    data class BookingState(
        val isSubmitting: Boolean = false,
        val errorMessage: String? = null,
    )

    sealed interface BookingEvent {
        data object Submitted : BookingEvent
    }

    private fun dateTimeFromSlot(date: LocalDate, timeSlot: String): Long {
        val trimmed = timeSlot.trim()
        val timePart = trimmed.substringBefore(' ')
        val meridiem = trimmed.substringAfter(' ', "").uppercase()
        val hourPart = timePart.substringBefore(':').toInt()
        val minutePart = timePart.substringAfter(':').toInt()
        val hour24 = when (meridiem) {
            "AM" -> if (hourPart == 12) 0 else hourPart
            "PM" -> if (hourPart == 12) 12 else hourPart + 12
            else -> hourPart
        }
        return LocalDateTime(date, LocalTime(hour24, minutePart))
            .toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()
    }

    private companion object {
        const val DEFAULT_APPOINTMENT_TYPE = "CONSULTATION"
    }
}
