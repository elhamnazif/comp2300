package com.group8.comp2300.presentation.ui.screens.medical

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.group8.comp2300.domain.model.medical.Clinic
import com.group8.comp2300.domain.repository.ClinicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BookingViewModel(private val repository: ClinicRepository) : ViewModel() {
    val state: StateFlow<State>
        field = MutableStateFlow(State(clinics = repository.getAllClinics()))

    fun selectClinic(clinic: Clinic?) {
        state.value = state.value.copy(selectedClinic = clinic)
    }

    fun getClinicById(id: String): Clinic? = repository.getClinicById(id)

    @Immutable
    data class State(val clinics: List<Clinic> = emptyList(), val selectedClinic: Clinic? = null)
}
