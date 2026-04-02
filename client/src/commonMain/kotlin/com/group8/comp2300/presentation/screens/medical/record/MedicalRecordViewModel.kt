package com.group8.comp2300.presentation.screens.medical.record

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group8.comp2300.domain.model.medical.MedicalRecordResponse
import com.group8.comp2300.domain.model.medical.RecordSortOrder
import com.group8.comp2300.domain.repository.MedicalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MedicalRecordViewModel(private val repository: MedicalRepository) : ViewModel() {

    var uiState by mutableStateOf(MedicalRecordsUiState())
        private set

    fun loadRecords(sort: RecordSortOrder = uiState.selectedSort) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)
            try {
                val records = repository.getMedicalRecords(sort.apiValue)
                uiState = uiState.copy(records = records, selectedSort = sort, isLoading = false)
            } catch (e: Exception) {
                uiState = uiState.copy(errorMessage = "Failed to load records", isLoading = false)
            }
        }
    }

    fun uploadFile(fileBytes: ByteArray, fileName: String) {
        if (fileBytes.size > 10 * 1024 * 1024) {
            uiState = uiState.copy(errorMessage = "File exceeds 10MB limit")
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)
            val success = repository.uploadMedicalRecord(fileBytes, fileName)
            if (success) {
                loadRecords()
            } else {
                uiState =
                    uiState.copy(errorMessage = "Failed to upload records", isLoading = false)
            }
        }
    }

    fun deleteRecord(id: String) {
        viewModelScope.launch {
            repository.deleteMedicalRecord(id)
            loadRecords()
        }
    }
}

data class MedicalRecordsUiState(
    val records: List<MedicalRecordResponse> = emptyList(),
    val isLoading: Boolean = false,
    val selectedSort: RecordSortOrder = RecordSortOrder.RECENT,
    val errorMessage: String? = null,
)
