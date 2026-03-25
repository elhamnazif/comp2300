package com.group8.comp2300.domain.model.medical

import kotlinx.serialization.Serializable

@Serializable
data class MedicalRecord(
    val id: String,
    val fileName: String,
    val fileSize: Long,
    val createdAt: Long = 0L
)

@Serializable
data class RenameRequest(
    val newName: String
)