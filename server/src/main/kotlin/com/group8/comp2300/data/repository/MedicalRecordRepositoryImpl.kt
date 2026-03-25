package com.group8.comp2300.data.repository

import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.database.data.MedicalRecordEnt
import com.group8.comp2300.domain.model.medical.MedicalRecord
import com.group8.comp2300.domain.repository.MedicalRecordRepository
import kotlin.time.Clock

class MedicalRecordRepositoryImpl(private val database: ServerDatabase) : MedicalRecordRepository {

    override fun insert(
        id: String,
        userId: String,
        fileName: String,
        storagePath: String,
        fileSize: Long,
        createdAt: Long
    ) {
        database.medicalRecordQueries.insertRecord(
            id = id,
            userId = userId,
            fileName = fileName,
            storagePath = storagePath,
            fileSize = fileSize,
            createdAt = createdAt
        )
    }

    override fun getRecordsByUserId(userId: String): List<MedicalRecord> {
        // pass a 'mapper' lambda directly into the query function
        return database.medicalRecordQueries.getRecordsByUserId(userId) { id, fileName, fileSize, createdAt ->
            MedicalRecord(
                id = id,
                fileName = fileName,
                fileSize = fileSize,
                createdAt = createdAt
            )
        }.executeAsList()
    }

    override fun getFilePath(id: String, userId: String): String? {
        return database.medicalRecordQueries.getFilePathById(id, userId).executeAsOneOrNull()
    }

    override fun updateFileName(id: String, userId: String, newName: String): Boolean {
        database.medicalRecordQueries.updateFileName(newName, id, userId)
        // Check if a row was actually updated
        return database.medicalRecordQueries.changes().executeAsOne() > 0
    }

    override fun delete(id: String, userId: String): Boolean {
        database.medicalRecordQueries.deleteRecordById(id, userId)
        return database.medicalRecordQueries.changes().executeAsOne() > 0
    }
}

/**
 * Extension function to map the SQLDelight Entity to our clean Domain Model.
 * This ensures the storagePath stays hidden from the UI layers.
 */
private fun MedicalRecordEnt.toDomain(): MedicalRecord = MedicalRecord(
    id = id,
    fileName = fileName,
    fileSize = fileSize,
    createdAt = createdAt
)
