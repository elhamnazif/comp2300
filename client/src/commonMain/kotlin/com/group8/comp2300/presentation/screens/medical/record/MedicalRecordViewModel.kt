package com.group8.comp2300.presentation.screens.medical.record

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group8.comp2300.domain.model.medical.MedicalRecordCategory
import com.group8.comp2300.domain.model.medical.MedicalRecordResponse
import com.group8.comp2300.domain.model.medical.RecordSortOrder
import com.group8.comp2300.domain.repository.MedicalRepository
import kotlinx.coroutines.launch

class MedicalRecordViewModel(
    private val repository: MedicalRepository,
    private val fileOpener: MedicalRecordFileOpener,
) : ViewModel() {

    private var pendingUploadBytes: ByteArray? = null

    var uiState by mutableStateOf(MedicalRecordsUiState())
        private set

    init {
        loadRecords()
    }

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

    fun prepareUpload(fileBytes: ByteArray, fileName: String) {
        if (fileBytes.size > 10 * 1024 * 1024) {
            uiState = uiState.copy(errorMessage = "File exceeds 10MB limit")
            return
        }

        pendingUploadBytes = fileBytes
        uiState = uiState.copy(
            pendingUploadFileName = fileName,
            pendingUploadCategory = MedicalRecordCategory.GENERAL,
        )
    }

    fun selectPendingCategory(category: MedicalRecordCategory) {
        uiState = uiState.copy(pendingUploadCategory = category)
    }

    fun dismissPendingUpload() {
        pendingUploadBytes = null
        uiState = uiState.copy(pendingUploadFileName = null)
    }

    fun confirmUpload() {
        val fileBytes = pendingUploadBytes ?: return
        val fileName = uiState.pendingUploadFileName ?: return
        val category = uiState.pendingUploadCategory

        pendingUploadBytes = null

        viewModelScope.launch {
            uiState = uiState.copy(
                isLoading = true,
                pendingUploadFileName = null,
            )
            val success = repository.uploadMedicalRecord(fileBytes, fileName, category)
            if (success) {
                loadRecords()
            } else {
                uiState =
                    uiState.copy(errorMessage = "Failed to upload records", isLoading = false)
            }
        }
    }

    fun openRecord(record: MedicalRecordResponse) {
        viewModelScope.launch {
            uiState = uiState.copy(openingRecordId = record.id)
            try {
                val fileBytes = repository.downloadMedicalRecord(record.id)
                val result = fileOpener.open(record.fileName, fileBytes)
                if (result.isFailure) {
                    uiState = uiState.copy(errorMessage = "Failed to open record")
                }
            } catch (_: Exception) {
                uiState = uiState.copy(errorMessage = "Failed to open record")
            } finally {
                uiState = uiState.copy(openingRecordId = null)
            }
        }
    }

    fun deleteRecord(id: String) {
        viewModelScope.launch {
            repository.deleteMedicalRecord(id)
            loadRecords()
        }
    }

    fun dismissError() {
        uiState = uiState.copy(errorMessage = null)
    }
}

data class MedicalRecordsUiState(
    val records: List<MedicalRecordResponse> = emptyList(),
    val isLoading: Boolean = false,
    val openingRecordId: String? = null,
    val selectedSort: RecordSortOrder = RecordSortOrder.RECENT,
    val pendingUploadFileName: String? = null,
    val pendingUploadCategory: MedicalRecordCategory = MedicalRecordCategory.GENERAL,
    val errorMessage: String? = null,
)
