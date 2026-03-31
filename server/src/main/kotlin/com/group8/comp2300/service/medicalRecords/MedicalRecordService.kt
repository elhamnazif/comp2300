package com.group8.comp2300.service.medicalRecords

import com.group8.comp2300.domain.model.medical.MedicalRecord
import com.group8.comp2300.domain.model.medical.MedicalRecordSortOrder
import com.group8.comp2300.domain.repository.MedicalRecordRepository
import com.group8.comp2300.dto.MedicalRecordResponse
import io.ktor.http.content.*
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

class MedicalRecordService(private val repository: MedicalRecordRepository, private val uploadDir: String = "uploads") {

    // Define safe file types for the app (Documents + Images)
    private val allowedExtensions = setOf("pdf", "jpg", "jpeg", "png", "docx", "doc")

    init {
        val dir = File(uploadDir)
        if (!dir.exists()) dir.mkdirs()
    }

    suspend fun uploadMedicalRecord(userId: String, part: PartData.FileItem): MedicalRecord? =
        withContext(Dispatchers.IO) {
            val fileId = UUID.randomUUID().toString()
            val originalName = part.originalFileName ?: "upload_${System.currentTimeMillis()}"

            // Safely extract the extension
            val extension = originalName.substringAfterLast(".", "").lowercase()

            // Security check: reject unsupported or potentially dangerous files
            if (extension.isNotEmpty() && extension !in allowedExtensions) {
                return@withContext null
            }

            // Construct the physical file name preserving the real extension
            val storageFileName = if (extension.isNotEmpty()) "$fileId.$extension" else fileId
            val physicalFile = File(uploadDir, storageFileName)

            try {
                val readChannel = part.provider()
                readChannel.copyTo(physicalFile.writeChannel())

                val actualSize = physicalFile.length()
                val maxFileSize = 10 * 1024 * 1024L // 10 MB

                if (actualSize > maxFileSize) {
                    physicalFile.delete()
                    return@withContext null
                }

                val now = System.currentTimeMillis()

                repository.insert(
                    id = fileId,
                    userId = userId,
                    fileName = originalName,
                    storagePath = physicalFile.path,
                    fileSize = actualSize,
                    createdAt = now,
                )

                MedicalRecord(fileId, originalName, actualSize, now)
            } catch (e: Exception) {
                if (physicalFile.exists()) physicalFile.delete()
                null
            }
        }

    fun getRecordsForUser(userId: String, sortQuery: String?): List<MedicalRecord> {
        val sortOrder = when (sortQuery?.uppercase()) {
            "DATE_ASC" -> MedicalRecordSortOrder.DATE_ASC
            "NAME_ASC" -> MedicalRecordSortOrder.NAME_ASC
            "NAME_DESC" -> MedicalRecordSortOrder.NAME_DESC
            else -> MedicalRecordSortOrder.DATE_DESC
        }
        return repository.getRecordsByUserId(userId, sortOrder)
    }

    fun deleteRecord(id: String, userId: String): Boolean {
        val path = repository.getFilePath(id, userId) ?: return false
        val wasDbDeleted = repository.delete(id, userId)

        if (wasDbDeleted) {
            val file = File(path)
            if (file.exists()) file.delete()
        }
        return wasDbDeleted
    }

    fun renameRecord(id: String, userId: String, newName: String): Boolean {
        // Default sort order: DATE_DESC
        val currentRecord = repository.getRecordsByUserId(userId, MedicalRecordSortOrder.DATE_DESC)
            .find { it.id == id } ?: return false

        val originalExtension = currentRecord.fileName.substringAfterLast(".", "")
        val newExtension = newName.substringAfterLast(".", "").lowercase()

        val sanitizedName = if (originalExtension.isNotEmpty() && newExtension != originalExtension) {
            "$newName.$originalExtension"
        } else {
            newName
        }

        return repository.updateFileName(id, userId, sanitizedName)
    }

    fun getPhysicalFile(id: String, userId: String): File? {
        val path = repository.getFilePath(id, userId) ?: return null
        val file = File(path)
        return if (file.exists()) file else null
    }

    fun reuploadRecord(id: String, userId: String, newFileName: String, fileBytes: ByteArray): Boolean {
        val oldPath = repository.getFilePath(id, userId) ?: return false

        val oldFile = File(oldPath)
        if (oldFile.exists()) oldFile.delete()

        // Handle the extension dynamically for the re-uploaded file
        val extension = newFileName.substringAfterLast(".", "").lowercase()
        val safeName = if (extension.isNotEmpty() && !newFileName.lowercase().endsWith(".$extension")) {
            "$newFileName.$extension"
        } else {
            newFileName
        }

        val newTimestamp = System.currentTimeMillis()
        val storageFileName = if (extension.isNotEmpty()) "$id-$newTimestamp.$extension" else "$id-$newTimestamp"
        val newPath = "$uploadDir/$storageFileName"

        File(newPath).writeBytes(fileBytes)

        return repository.updateRecordMetadata(
            id = id,
            userId = userId,
            newName = safeName,
            newPath = newPath,
            newSize = fileBytes.size.toLong(),
            newTimestamp = newTimestamp,
        )
    }
}

fun MedicalRecord.toDto(): MedicalRecordResponse = MedicalRecordResponse(
    id = this.id,
    fileName = this.fileName,
    fileSize = this.fileSize,
    createdAt = this.createdAt,
)
