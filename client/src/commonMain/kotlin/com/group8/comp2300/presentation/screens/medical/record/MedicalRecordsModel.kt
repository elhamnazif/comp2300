package com.group8.comp2300.presentation.screens.medical.record

import kotlinx.serialization.Serializable

@Serializable
data class MedicalRecordResponse(
    val id: String,
    val fileName: String,
    val fileSize: Long,
    val createdAt: Long
)
@Serializable
data class RenameRequest(
    val newName: String
)

@Serializable
data class ApiResponse(
    val sucess: Boolean,
    val message: String
)

enum class RecordSortOrder(val apiValue: String) {
    RECENT("DATE_DESC"),
    OLDEST("DATE_ASC"),
    NAME_AZ("NAME_ASC"),
    NAME_ZA("NAME_DESC")
}