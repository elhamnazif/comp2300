package com.group8.comp2300.presentation.screens.medical

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group8.comp2300.domain.model.medical.Clinic
import com.group8.comp2300.domain.repository.ClinicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class BookingViewModel(private val repository: ClinicRepository) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedClinic = MutableStateFlow<Clinic?>(null)
    val selectedClinic: StateFlow<Clinic?> = _selectedClinic

    private val clinicsList = MutableStateFlow(repository.getAllClinics())

    val allClinics: List<Clinic> get() = clinicsList.value

    val filteredClinics: StateFlow<List<Clinic>> = combine(
        clinicsList,
        _searchQuery,
    ) { clinics, query ->
        if (query.isBlank()) {
            clinics
        } else {
            clinics.filter { clinic ->
                clinic.name.contains(query, ignoreCase = true) ||
                    clinic.address?.contains(query, ignoreCase = true) == true ||
                    clinic.tags.any { it.contains(query, ignoreCase = true) }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.Eagerly,
        initialValue = repository.getAllClinics(),
    )

    fun selectClinic(clinic: Clinic?) {
        _selectedClinic.value = clinic
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun getClinicById(id: String): Clinic? = repository.getClinicById(id)

    @Immutable
    data class State(val clinics: List<Clinic> = emptyList(), val selectedClinic: Clinic? = null)
}
