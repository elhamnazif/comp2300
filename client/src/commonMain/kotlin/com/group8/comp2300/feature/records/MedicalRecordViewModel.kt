package com.group8.comp2300.feature.records

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.group8.comp2300.domain.model.medical.MedicalRecordCategory
import com.group8.comp2300.domain.model.medical.MedicalRecordResponse
import com.group8.comp2300.domain.model.medical.RecordSortOrder
import com.group8.comp2300.domain.repository.medical.MedicalRecordDataRepository
import com.group8.comp2300.platform.files.MedicalRecordFileOpener
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MedicalRecordViewModel(
    private val repository: MedicalRecordDataRepository,
    private val fileOpener: MedicalRecordFileOpener,
) : ViewModel() {
    private val logger = Logger.withTag("MedicalRecordViewModel")
    private var allRecords: List<MedicalRecordResponse> = emptyList()
    private var pendingUploadFile: PlatformFile? = null

    var uiState by mutableStateOf(MedicalRecordsUiState())
        private set

    init {
        refresh()
    }

    fun refresh() {
        loadRecords(uiState.selectedSort)
    }

    fun loadRecords(sort: RecordSortOrder = uiState.selectedSort) {
        viewModelScope.launch {
            refreshRecords(sort)
        }
    }

    fun selectCategoryFilter(category: MedicalRecordCategory?) {
        uiState = uiState.copy(
            selectedCategory = category,
            records = allRecords.filteredBy(category),
        )
    }

    fun prepareUpload(file: PlatformFile) {
        pendingUploadFile = file
        uiState = uiState.copy(
            pendingUploadFileName = file.name,
            pendingUploadCategory = MedicalRecordCategory.GENERAL,
        )
    }

    fun selectPendingCategory(category: MedicalRecordCategory) {
        uiState = uiState.copy(pendingUploadCategory = category)
    }

    fun dismissPendingUpload() {
        pendingUploadFile = null
        uiState = uiState.copy(pendingUploadFileName = null)
    }

    fun confirmUpload() {
        val file = pendingUploadFile ?: return
        val fileName = uiState.pendingUploadFileName ?: return
        val category = uiState.pendingUploadCategory

        pendingUploadFile = null

        viewModelScope.launch {
            uiState = uiState.copy(
                isUploading = true,
                pendingUploadFileName = null,
            )
            try {
                val fileBytes = withContext(Dispatchers.Default) { file.readBytes() }
                if (fileBytes.size > MAX_UPLOAD_BYTES) {
                    uiState = uiState.copy(
                        errorMessage = "File exceeds 10MB limit",
                        isUploading = false,
                    )
                    return@launch
                }

                val success = repository.uploadMedicalRecord(fileBytes, fileName, category)
                if (success) {
                    uiState = uiState.copy(isUploading = false)
                    refreshRecords()
                } else {
                    uiState = uiState.copy(
                        errorMessage = "Failed to upload record",
                        isUploading = false,
                    )
                }
            } catch (exception: Exception) {
                logger.e(exception) { "Failed to upload medical record" }
                uiState = uiState.copy(
                    errorMessage = "Failed to upload record",
                    isUploading = false,
                )
            }
        }
    }

    fun openRecord(record: MedicalRecordResponse) {
        if (uiState.openingRecordId == record.id || uiState.deletingRecordId == record.id) return

        viewModelScope.launch {
            uiState = uiState.copy(openingRecordId = record.id)
            try {
                val fileBytes = repository.downloadMedicalRecord(record.id)
                val result = fileOpener.open(record.fileName, fileBytes)
                if (result.isFailure) {
                    logger.e(result.exceptionOrNull()) { "Failed to open medical record ${record.id}" }
                    uiState = uiState.copy(errorMessage = "Failed to open record")
                }
            } catch (exception: Exception) {
                logger.e(exception) { "Failed to open medical record ${record.id}" }
                uiState = uiState.copy(errorMessage = "Failed to open record")
            } finally {
                uiState = uiState.copy(openingRecordId = null)
            }
        }
    }

    fun deleteRecord(id: String) {
        if (uiState.deletingRecordId == id) return

        viewModelScope.launch {
            uiState = uiState.copy(deletingRecordId = id)
            try {
                repository.deleteMedicalRecord(id)
                allRecords = allRecords.filterNot { it.id == id }
                uiState = uiState.copy(
                    records = allRecords.filteredBy(uiState.selectedCategory),
                    deletingRecordId = null,
                )
            } catch (exception: Exception) {
                logger.e(exception) { "Failed to delete medical record $id" }
                uiState = uiState.copy(
                    errorMessage = "Failed to delete record",
                    deletingRecordId = null,
                )
            }
        }
    }

    fun dismissError() {
        uiState = uiState.copy(errorMessage = null)
    }

    private suspend fun refreshRecords(sort: RecordSortOrder = uiState.selectedSort) {
        uiState = uiState.copy(
            isRefreshing = true,
            selectedSort = sort,
            loadErrorMessage = null,
        )

        try {
            val records = repository.getMedicalRecords(sort.apiValue)
            allRecords = records
            uiState = uiState.copy(
                records = records.filteredBy(uiState.selectedCategory),
                isRefreshing = false,
                loadErrorMessage = null,
            )
        } catch (exception: Exception) {
            logger.e(exception) { "Failed to load medical records" }
            val hasExistingRecords = allRecords.isNotEmpty()
            uiState = uiState.copy(
                isRefreshing = false,
                loadErrorMessage = "Failed to load records",
                errorMessage = if (hasExistingRecords) "Failed to load records" else uiState.errorMessage,
            )
        }
    }

    private fun List<MedicalRecordResponse>.filteredBy(
        category: MedicalRecordCategory?,
    ): List<MedicalRecordResponse> = if (category == null) this else filter { it.category == category }

    private companion object {
        const val MAX_UPLOAD_BYTES = 10 * 1024 * 1024
    }
}

data class MedicalRecordsUiState(
    val records: List<MedicalRecordResponse> = emptyList(),
    val isRefreshing: Boolean = false,
    val isUploading: Boolean = false,
    val openingRecordId: String? = null,
    val deletingRecordId: String? = null,
    val selectedSort: RecordSortOrder = RecordSortOrder.RECENT,
    val selectedCategory: MedicalRecordCategory? = null,
    val pendingUploadFileName: String? = null,
    val pendingUploadCategory: MedicalRecordCategory = MedicalRecordCategory.GENERAL,
    val loadErrorMessage: String? = null,
    val errorMessage: String? = null,
)
