package com.group8.comp2300.domain.repository

import com.group8.comp2300.domain.model.medical.MedicalRecord

interface MedicalRecordRepository {
    fun insert(
        id: String,
        userId: String,
        fileName: String,
        storagePath: String,
        fileSize: Long,
        createdAt: Long
    )
    fun getRecordsByUserId(userId: String): List<MedicalRecord>  //Does not include the storagePath
    fun getFilePath(id: String, userId: String): String?
    fun updateFileName(id: String, userId: String, newName: String): Boolean
    fun delete(id: String, userId: String): Boolean
}
