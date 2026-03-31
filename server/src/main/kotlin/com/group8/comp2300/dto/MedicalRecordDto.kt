package com.group8.comp2300.dto

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
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * Standardized Response DTO: Useful for consistent success/error messages.
 */
@Serializable
data class ApiResponse(val success: Boolean, val message: String)

@Serializable
data class RenameRequest(val newName: String)
