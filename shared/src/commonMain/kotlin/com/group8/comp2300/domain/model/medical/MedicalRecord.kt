package com.group8.comp2300.domain.model.medical

import kotlinx.serialization.Serializable

@Serializable
enum class MedicalRecordCategory(val displayName: String) {
    GENERAL("General"),
    LAB_RESULT("Lab Result"),
    PRESCRIPTION("Prescription"),
    IMAGING("Imaging"),
    OTHER("Other"),
}

@Serializable
data class MedicalRecord(
    val id: String,
    val fileName: String,
    val fileSize: Long,
    val createdAt: Long = 0L,
    val category: MedicalRecordCategory = MedicalRecordCategory.GENERAL,
)

typealias MedicalRecordResponse = MedicalRecord
