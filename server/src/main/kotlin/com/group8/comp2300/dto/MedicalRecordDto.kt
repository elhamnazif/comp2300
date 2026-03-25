package com.group8.comp2300.dto

import com.group8.comp2300.domain.model.medical.MedicalRecord
import kotlinx.serialization.Serializable

/**
 * Output DTO: Sent to the client when they request their medical records.
 */
@Serializable
data class MedicalRecordResponse(
    val id: String,
    val fileName: String,
    val fileSize: Long,   // Size in bytes
    val createdAt: Long   // Unix timestamp
)

/**
 * Input DTO: Expected from the client when they want to rename a file.
 * (This replaces sending the new name in the URL query parameters).
 */
@Serializable
data class RenameRecordRequest(
    val newName: String
)

/**
 * Standardized Response DTO: Useful for consistent success/error messages.
 */
@Serializable
data class ApiResponse(
    val success: Boolean,
    val message: String
)

@Serializable
data class RenameRequest(
    val newName: String
)
