package com.group8.comp2300.dto

import com.group8.comp2300.domain.model.medical.MedicalRecord
import com.group8.comp2300.domain.model.medical.MedicalRecordCategory
import kotlinx.serialization.Serializable

/**
 * Note: fileName should include extensions like .jpg, .jpeg, .png, .doc, .docx
 * depending on what the user uploaded.
 */
@Serializable
data class MedicalRecordResponse(
    val id: String,
    val fileName: String,
    val fileSize: Long,
    val createdAt: Long,
    val category: MedicalRecordCategory,
)

@Serializable
data class RenameRequest(val newName: String)

@Serializable
data class ErrorResponse(val error: String)

fun MedicalRecord.toDto(): MedicalRecordResponse = MedicalRecordResponse(
    id = this.id,
    fileName = this.fileName,
    fileSize = this.fileSize,
    createdAt = this.createdAt,
    category = this.category,
)
