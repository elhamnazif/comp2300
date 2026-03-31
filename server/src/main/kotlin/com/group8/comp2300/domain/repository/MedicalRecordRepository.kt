package com.group8.comp2300.domain.repository

import com.group8.comp2300.domain.model.medical.MedicalRecord
import com.group8.comp2300.domain.model.medical.MedicalRecordSortOrder

interface MedicalRecordRepository {
    fun insert(id: String, userId: String, fileName: String, storagePath: String, fileSize: Long, createdAt: Long)

    // Does not include the storagePath
    fun getRecordById(id: String, userId: String): MedicalRecord?

    // Does not include the storagePath
    fun getRecordsByUserId(
        userId: String,
        sortOrder: MedicalRecordSortOrder = MedicalRecordSortOrder.DATE_DESC,
    ): List<MedicalRecord>
    fun updateFileName(id: String, userId: String, newName: String): Boolean
    fun delete(id: String, userId: String): Boolean
    fun updateRecordMetadata(
        id: String,
        userId: String,
        newName: String,
        newPath: String,
        newSize: Long,
        newTimestamp: Long,
    ): Boolean
    fun getFilePath(id: String, userId: String): String?
}
