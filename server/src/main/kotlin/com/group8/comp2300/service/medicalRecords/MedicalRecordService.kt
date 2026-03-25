package com.group8.comp2300.service.medicalRecords

import com.group8.comp2300.domain.model.medical.MedicalRecord
import com.group8.comp2300.domain.repository.MedicalRecordRepository
import com.group8.comp2300.dto.MedicalRecordResponse
import io.ktor.http.content.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyTo
import java.io.File
import java.util.*

class MedicalRecordService(
    private val repository: MedicalRecordRepository,
    private val uploadDir: String = "uploads" // Directory where PDFs are stored
) {

    init {
        // Create uploads folder if it doesn't exist
        val dir = File(uploadDir)
        if (!dir.exists()) dir.mkdirs()
    }

    /**
     * Handles the multipart file upload from Ktor.
     * Generates a unique ID and saves the physical file before updating the DB.
     */
    suspend fun uploadMedicalRecord(userId: String, part: PartData.FileItem): MedicalRecord? = withContext(Dispatchers.IO) {
        val fileId = UUID.randomUUID().toString()
        val originalName = part.originalFileName ?: "unknown.pdf"
        val extension = originalName.substringAfterLast(".", "pdf")

        // Only allow PDFs
        if (extension.lowercase() != "pdf") return@withContext null

        val physicalFile = File(uploadDir, "$fileId.$extension")

        try {
            val readChannel = part.provider()
            readChannel.copyTo(physicalFile.writeChannel())

            // Check file size
            val actualSize = physicalFile.length()
            val maxFileSize = 10 * 1024 * 1024L // 10 MB

            if (actualSize > maxFileSize) {
                physicalFile.delete() // File too big
                return@withContext null
            }

            val now = System.currentTimeMillis()

            repository.insert(
                id = fileId,
                userId = userId,
                fileName = originalName,
                storagePath = physicalFile.path,
                fileSize = actualSize,
                createdAt = now
            )

            MedicalRecord(fileId, originalName, actualSize, now)
        } catch (e: Exception) {
            if (physicalFile.exists()) physicalFile.delete()
            null
        }
    }

    /**
     * Fetches all records for a user.
     */
    fun getRecordsForUser(userId: String): List<MedicalRecord> {
        return repository.getRecordsByUserId(userId)
    }

    /**
     * Safely deletes the database record and the physical file.
     */
    fun deleteRecord(id: String, userId: String): Boolean {
        val path = repository.getFilePath(id, userId) ?: return false

        val wasDbDeleted = repository.delete(id, userId)

        if (wasDbDeleted) {
            val file = File(path)
            if (file.exists()) file.delete()
        }

        return wasDbDeleted
    }

    /**
     * Renames the file in the database.
     */
    fun renameRecord(id: String, userId: String, newName: String): Boolean {
        // Basic validation: Ensure it ends with .pdf
        val sanitizedName = if (newName.lowercase().endsWith(".pdf")) newName else "$newName.pdf"
        return repository.updateFileName(id, userId, sanitizedName)
    }

    /**
     * Returns the physical file object for streaming/downloading.
     */
    fun getPhysicalFile(id: String, userId: String): File? {
        val path = repository.getFilePath(id, userId) ?: return null
        val file = File(path)
        return if (file.exists()) file else null
    }
}

/**
 * Helper to convert the internal MedicalRecord domain model into a DTO for JSON responses.
 */
fun MedicalRecord.toDto(): MedicalRecordResponse {
    return MedicalRecordResponse(
        id = this.id,
        fileName = this.fileName,
        fileSize = this.fileSize,
        createdAt = this.createdAt
    )
}
