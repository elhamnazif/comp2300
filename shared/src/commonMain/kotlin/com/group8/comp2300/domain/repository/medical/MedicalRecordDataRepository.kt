package com.group8.comp2300.domain.repository.medical

import com.group8.comp2300.domain.model.medical.MedicalRecordCategory
import com.group8.comp2300.domain.model.medical.MedicalRecordResponse

interface MedicalRecordDataRepository {
    suspend fun getMedicalRecords(sort: String): List<MedicalRecordResponse>

    suspend fun uploadMedicalRecord(fileBytes: ByteArray, fileName: String, category: MedicalRecordCategory): Boolean

    suspend fun downloadMedicalRecord(id: String): ByteArray

    suspend fun deleteMedicalRecord(id: String)
}
