package com.group8.comp2300.presentation.screens.medical.booking

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group8.comp2300.domain.model.medical.Clinic
import com.group8.comp2300.domain.model.medical.ClinicFilters
import com.group8.comp2300.domain.repository.ClinicRepository
import com.group8.comp2300.services.ClinicFilterService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class BookingViewModel(
    private val repository: ClinicRepository,
    private val clinicFilterService: ClinicFilterService,
) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _filters = MutableStateFlow(ClinicFilters())
    val filters: StateFlow<ClinicFilters> = _filters

    private val _selectedClinic = MutableStateFlow<Clinic?>(null)
    val selectedClinic: StateFlow<Clinic?> = _selectedClinic

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

    @Immutable
    data class State(val clinics: List<Clinic> = emptyList(), val selectedClinic: Clinic? = null)
}
