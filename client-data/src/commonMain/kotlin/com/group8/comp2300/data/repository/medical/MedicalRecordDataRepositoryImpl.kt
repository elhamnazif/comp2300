package com.group8.comp2300.data.repository.medical

import com.group8.comp2300.data.remote.ApiService
import com.group8.comp2300.domain.model.medical.MedicalRecordCategory
import com.group8.comp2300.domain.model.medical.MedicalRecordResponse
import com.group8.comp2300.domain.repository.medical.MedicalRecordDataRepository

class MedicalRecordDataRepositoryImpl(
    private val apiService: ApiService,
) : MedicalRecordDataRepository {
    override suspend fun getMedicalRecords(sort: String): List<MedicalRecordResponse> = apiService.getMedicalRecords(sort)

    override suspend fun uploadMedicalRecord(
        fileBytes: ByteArray,
        fileName: String,
        category: MedicalRecordCategory,
    ): Boolean = try {
        apiService.uploadMedicalRecord(fileBytes, fileName, category)
        true
    } catch (_: Exception) {
        false
    }

    override suspend fun downloadMedicalRecord(id: String): ByteArray = apiService.downloadMedicalRecord(id)

    override suspend fun deleteMedicalRecord(id: String) {
        apiService.deleteMedicalRecord(id)
    }
}
