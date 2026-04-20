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
import com.group8.comp2300.domain.repository.medical.SyncCoordinator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.time.Clock

class BookingViewModel(
    private val clinicRepository: ClinicRepository,
    private val appointmentRepository: AppointmentDataRepository,
    private val syncCoordinator: SyncCoordinator,
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

    fun ensureBookingDraft(clinicId: String, slotId: String, rescheduleAppointment: Appointment? = null) {
        val currentDraft = _state.value.bookingDraft
        if (
            currentDraft?.clinicId == clinicId &&
            currentDraft.slotId == slotId &&
            currentDraft.rescheduleAppointmentId == rescheduleAppointment?.id
        ) {
            return
        }

        _state.update { current ->
            current.copy(
                bookingDraft = rescheduleAppointment?.let {
                    BookingDraft(
                        clinicId = clinicId,
                        slotId = slotId,
                        appointmentType = it.appointmentType,
                        reason = it.notes.orEmpty(),
                        hasReminder = it.hasReminder,
                        rescheduleAppointmentId = it.id,
                    )
                } ?: current.bookingDraft?.takeIf { it.clinicId == clinicId }?.copy(slotId = slotId) ?: BookingDraft(
                    clinicId = clinicId,
                    slotId = slotId,
                ),
                lastBookedAppointment = null,
            )
        }
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

    fun updateBookingDraft(appointmentType: String? = null, reason: String? = null, hasReminder: Boolean? = null) {
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
                selectedManagedAppointmentId = null,
                errorMessage = null,
            )
        }
    }

    fun loadBookingHistory(refreshFromServer: Boolean = true) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingAppointments = true, errorMessage = null) }
            runCatching {
                if (refreshFromServer) {
                    syncCoordinator.refreshAuthenticatedData()
                }
                appointmentRepository.getBookingHistory()
            }.onSuccess { appointments ->
                _state.update {
                    it.copy(
                        managedAppointments = appointments.sortedWith(bookingHistoryComparator()),
                        isLoadingAppointments = false,
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isLoadingAppointments = false,
                        errorMessage = error.message ?: "Failed to load bookings",
                    )
                }
            }
        }
    }

    fun showManagedAppointment(appointmentId: String) {
        _state.update { it.copy(selectedManagedAppointmentId = appointmentId) }
    }

    fun hideManagedAppointment() {
        _state.update { it.copy(selectedManagedAppointmentId = null) }
    }

    fun prepareReschedule(appointment: Appointment) {
        val clinicId = appointment.clinicId
        val bookingId = appointment.bookingId
        if (clinicId.isNullOrBlank() || bookingId.isNullOrBlank()) {
            _state.update { it.copy(errorMessage = "This booking can’t be rescheduled") }
            return
        }

        _state.update {
            it.copy(
                bookingDraft = BookingDraft(
                    clinicId = clinicId,
                    slotId = bookingId,
                    appointmentType = appointment.appointmentType,
                    reason = appointment.notes.orEmpty(),
                    hasReminder = appointment.hasReminder,
                    rescheduleAppointmentId = appointment.id,
                ),
            )
        }
    }

    fun cancelAppointment(appointmentId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isMutatingAppointment = true, errorMessage = null) }
            runCatching { appointmentRepository.cancelAppointment(appointmentId) }
                .onSuccess { appointment ->
                    _state.update { current ->
                        current.copy(
                            isMutatingAppointment = false,
                            managedAppointments = current.managedAppointments
                                .replaceAppointment(appointment)
                                .sortedWith(bookingHistoryComparator()),
                            lastBookedAppointment = current.lastBookedAppointment
                                ?.takeIf { it.id == appointment.id }
                                ?.let { appointment } ?: current.lastBookedAppointment,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isMutatingAppointment = false,
                            errorMessage = error.message ?: "Failed to cancel booking",
                        )
                    }
                }
        }
    }

    fun loadPersistedAppointment(appointmentId: String) {
        if (getLastBookedAppointment(appointmentId) != null) return

        viewModelScope.launch {
            runCatching { appointmentRepository.getAppointment(appointmentId) }
                .onSuccess { appointment ->
                    if (appointment != null) {
                        _state.update { current ->
                            current.copy(
                                lastBookedAppointment = appointment,
                                managedAppointments = current.managedAppointments
                                    .replaceAppointment(appointment)
                                    .sortedWith(bookingHistoryComparator()),
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(errorMessage = error.message ?: "Failed to load booking") }
                }
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
                val request = ClinicBookingRequest(
                    clinicId = clinicId,
                    slotId = slotId,
                    appointmentType = draft?.appointmentType ?: appointmentType,
                    reason = (draft?.reason ?: reason).trim().ifBlank { null },
                    hasReminder = draft?.hasReminder ?: hasReminder,
                )
                val rescheduleAppointmentId = draft?.rescheduleAppointmentId
                val appointment = if (rescheduleAppointmentId == null) {
                    appointmentRepository.bookClinicAppointment(request)
                } else {
                    appointmentRepository.rescheduleAppointment(rescheduleAppointmentId, request)
                }
                appointment to (rescheduleAppointmentId != null)
            }.onSuccess { (appointment, wasRescheduled) ->
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        lastBookedAppointment = appointment,
                        managedAppointments = it.managedAppointments
                            .replaceAppointment(appointment)
                            .sortedWith(bookingHistoryComparator()),
                    )
                }
                _bookingEvents.emit(BookingEvent.Submitted(appointment, wasRescheduled))
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = error.message ?: "Failed to save booking",
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
            ?: _state.value.managedAppointments.firstOrNull { it.id == appointmentId }

    fun getManagedAppointment(appointmentId: String): Appointment? =
        _state.value.managedAppointments.firstOrNull { it.id == appointmentId }

    private fun filterClinics(state: State): List<Clinic> {
        val query = state.searchQuery.trim()
        return state.clinics.filter { clinic ->
            val matchesQuery = query.isBlank() ||
                clinic.name.contains(query, ignoreCase = true) ||
                clinic.address?.contains(query, ignoreCase = true) == true ||
                clinic.tags.any { it.contains(query, ignoreCase = true) }
            val matchesTag =
                state.selectedTag == null || clinic.tags.any { it.equals(state.selectedTag, ignoreCase = true) }
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
        val isLoadingAppointments: Boolean = false,
        val isMutatingAppointment: Boolean = false,
        val searchQuery: String = "",
        val selectedTag: String? = null,
        val isMapMode: Boolean = false,
        val bookingDraft: BookingDraft? = null,
        val lastBookedAppointment: Appointment? = null,
        val managedAppointments: List<Appointment> = emptyList(),
        val selectedManagedAppointmentId: String? = null,
        val errorMessage: String? = null,
    )

    @Immutable
    data class BookingDraft(
        val clinicId: String,
        val slotId: String,
        val appointmentType: String = "STI_TESTING",
        val reason: String = "",
        val hasReminder: Boolean = true,
        val rescheduleAppointmentId: String? = null,
    )

    sealed interface BookingEvent {
        data class Submitted(val appointment: Appointment, val wasRescheduled: Boolean) : BookingEvent
    }
}

private fun bookingHistoryComparator(): Comparator<Appointment> {
    val now = Clock.System.now().toEpochMilliseconds()
    return compareBy<Appointment> {
        when {
            it.status == "CANCELLED" -> 2
            it.appointmentTime < now -> 1
            else -> 0
        }
    }.thenBy { it.appointmentTime }
}

private fun List<Appointment>.replaceAppointment(updated: Appointment): List<Appointment> {
    val existingIndex = indexOfFirst { it.id == updated.id }
    if (existingIndex == -1) return this + updated
    return map { if (it.id == updated.id) updated else it }
}
