package com.group8.comp2300.feature.booking

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group8.comp2300.domain.model.medical.Appointment
import com.group8.comp2300.domain.model.medical.AppointmentSlot
import com.group8.comp2300.domain.model.medical.Clinic
import com.group8.comp2300.domain.model.medical.ClinicBookingRequest
import com.group8.comp2300.domain.repository.ClinicRepository
import com.group8.comp2300.domain.repository.medical.AppointmentDataRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

class BookingViewModel(
    private val clinicRepository: ClinicRepository,
    private val appointmentRepository: AppointmentDataRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(State(isLoadingClinics = true))
    val state: StateFlow<State> = _state

    val filteredClinics: StateFlow<List<Clinic>> = state
        .map(::filterClinics)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList(),
        )

    private val _bookingEvents = MutableSharedFlow<BookingEvent>()
    val bookingEvents = _bookingEvents.asSharedFlow()

    init {
        loadClinics()
    }

    fun loadClinics() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingClinics = true, errorMessage = null) }
            runCatching { clinicRepository.getAllClinics() }
                .onSuccess { clinics ->
                    _state.update {
                        it.copy(
                            clinics = clinics.sortedBy(Clinic::distanceKm),
                            isLoadingClinics = false,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoadingClinics = false,
                            errorMessage = error.message ?: "Failed to load clinics",
                        )
                    }
                }
        }
    }

    fun loadClinicDetails(clinicId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingClinic = true, isLoadingSlots = true, errorMessage = null) }
            runCatching {
                val clinic = clinicRepository.getClinicById(clinicId)
                    ?: error("Clinic not found")
                val slots = clinicRepository.getAvailableSlots(clinicId)
                    .filter { it.startTime > Clock.System.now().toEpochMilliseconds() }
                clinic to slots
            }.onSuccess { (clinic, slots) ->
                _state.update {
                    it.copy(
                        selectedClinic = clinic,
                        availableSlots = slots.sortedBy(AppointmentSlot::startTime),
                        isLoadingClinic = false,
                        isLoadingSlots = false,
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isLoadingClinic = false,
                        isLoadingSlots = false,
                        errorMessage = error.message ?: "Failed to load clinic availability",
                    )
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun toggleTag(tag: String?) {
        _state.update { current ->
            current.copy(selectedTag = if (current.selectedTag == tag) null else tag)
        }
    }

    fun selectClinic(clinic: Clinic) {
        _state.update { it.copy(selectedClinic = clinic) }
    }

    fun ensureBookingDraft(clinicId: String, slotId: String) {
        val currentDraft = _state.value.bookingDraft
        if (currentDraft?.clinicId == clinicId && currentDraft.slotId == slotId) return
        selectSlot(clinicId, slotId)
    }

    fun selectSlot(clinicId: String, slotId: String) {
        _state.update { current ->
            val nextDraft = if (current.bookingDraft?.clinicId == clinicId) {
                current.bookingDraft.copy(slotId = slotId)
            } else {
                BookingDraft(
                    clinicId = clinicId,
                    slotId = slotId,
                )
            }
            current.copy(bookingDraft = nextDraft, lastBookedAppointment = null)
        }
    }

    fun updateBookingDraft(
        appointmentType: String? = null,
        reason: String? = null,
        hasReminder: Boolean? = null,
    ) {
        _state.update { current ->
            val draft = current.bookingDraft
            if (draft == null) {
                current
            } else {
                current.copy(
                    bookingDraft = draft.copy(
                        appointmentType = appointmentType ?: draft.appointmentType,
                        reason = reason ?: draft.reason,
                        hasReminder = hasReminder ?: draft.hasReminder,
                    ),
                )
            }
        }
    }

    fun setMapMode(enabled: Boolean) {
        _state.update { it.copy(isMapMode = enabled) }
    }

    fun clearBookingError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun clearBookingFlow() {
        _state.update {
            it.copy(
                bookingDraft = null,
                lastBookedAppointment = null,
                errorMessage = null,
            )
        }
    }

    fun bookClinicAppointment(
        clinicId: String,
        slotId: String,
        appointmentType: String,
        reason: String,
        hasReminder: Boolean,
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, errorMessage = null) }
            runCatching {
                val draft = _state.value.bookingDraft
                appointmentRepository.bookClinicAppointment(
                    ClinicBookingRequest(
                        clinicId = clinicId,
                        slotId = slotId,
                        appointmentType = draft?.appointmentType ?: appointmentType,
                        reason = (draft?.reason ?: reason).trim().ifBlank { null },
                        hasReminder = draft?.hasReminder ?: hasReminder,
                    ),
                )
            }.onSuccess { appointment ->
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        lastBookedAppointment = appointment,
                    )
                }
                _bookingEvents.emit(BookingEvent.Submitted(appointment))
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = error.message ?: "Failed to book clinic appointment",
                    )
                }
            }
        }
    }

    fun getClinic(clinicId: String): Clinic? {
        val current = _state.value.selectedClinic
        if (current?.id == clinicId) return current
        return _state.value.clinics.firstOrNull { it.id == clinicId }
    }

    fun getSlot(slotId: String): AppointmentSlot? = _state.value.availableSlots.firstOrNull { it.id == slotId }

    fun getLastBookedAppointment(appointmentId: String): Appointment? =
        _state.value.lastBookedAppointment?.takeIf { it.id == appointmentId }

    private fun filterClinics(state: State): List<Clinic> {
        val query = state.searchQuery.trim()
        return state.clinics.filter { clinic ->
            val matchesQuery = query.isBlank() ||
                clinic.name.contains(query, ignoreCase = true) ||
                clinic.address?.contains(query, ignoreCase = true) == true ||
                clinic.tags.any { it.contains(query, ignoreCase = true) }
            val matchesTag = state.selectedTag == null || clinic.tags.any { it.equals(state.selectedTag, ignoreCase = true) }
            matchesQuery && matchesTag
        }.sortedBy(Clinic::distanceKm)
    }

    @Immutable
    data class State(
        val clinics: List<Clinic> = emptyList(),
        val selectedClinic: Clinic? = null,
        val availableSlots: List<AppointmentSlot> = emptyList(),
        val isLoadingClinics: Boolean = false,
        val isLoadingClinic: Boolean = false,
        val isLoadingSlots: Boolean = false,
        val isSubmitting: Boolean = false,
        val searchQuery: String = "",
        val selectedTag: String? = null,
        val isMapMode: Boolean = false,
        val bookingDraft: BookingDraft? = null,
        val lastBookedAppointment: Appointment? = null,
        val errorMessage: String? = null,
    )

    @Immutable
    data class BookingDraft(
        val clinicId: String,
        val slotId: String,
        val appointmentType: String = "STI_TESTING",
        val reason: String = "",
        val hasReminder: Boolean = true,
    )

    sealed interface BookingEvent {
        data class Submitted(val appointment: Appointment) : BookingEvent
    }
}
