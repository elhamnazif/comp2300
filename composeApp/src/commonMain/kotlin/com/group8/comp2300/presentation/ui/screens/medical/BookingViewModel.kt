package com.group8.comp2300.presentation.ui.screens.medical

import androidx.lifecycle.ViewModel
import com.group8.comp2300.domain.model.medical.Clinic
import com.group8.comp2300.domain.repository.ClinicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BookingUiState(val clinics: List<Clinic> = emptyList(), val selectedClinic: Clinic? = null)

class BookingViewModel(private val repository: ClinicRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(BookingUiState(clinics = repository.getAllClinics()))
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()

    fun selectClinic(clinic: Clinic?) {
        _uiState.value = _uiState.value.copy(selectedClinic = clinic)
    }

    fun getClinicById(id: String): Clinic? = repository.getClinicById(id)
}
