package com.group8.comp2300.service.medicalRecords

import com.group8.comp2300.domain.model.medical.MedicalRecord
import com.group8.comp2300.domain.model.medical.MedicalRecordSortOrder
import com.group8.comp2300.domain.repository.MedicalRecordRepository
import io.ktor.http.content.*
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

sealed class UploadResult {
    data class Success(val record: MedicalRecord) : UploadResult()
    data class Failed(val reason: String) : UploadResult()
}

class MedicalRecordService(
    private val repository: MedicalRecordRepository,
    private val uploadDir: String = "uploads",
) {

    companion object {
        private val ALLOWED_EXTENSIONS = setOf("pdf", "jpg", "jpeg", "png", "docx", "doc")
        private const val MAX_FILE_SIZE = 10 * 1024 * 1024L // 10 MB
    }

    init {
        val dir = File(uploadDir)
        if (!dir.exists()) dir.mkdirs()
    }

    suspend fun uploadMedicalRecord(userId: String, part: PartData.FileItem): UploadResult =
        withContext(Dispatchers.IO) {
            val fileId = UUID.randomUUID().toString()
            val originalName = part.originalFileName ?: "upload_${System.currentTimeMillis()}"

            val extension = originalName.substringAfterLast(".", "").lowercase()

            if (extension.isNotEmpty() && extension !in ALLOWED_EXTENSIONS) {
                return@withContext UploadResult.Failed("File type '.$extension' is not allowed")
            }

            val storageFileName = if (extension.isNotEmpty()) "$fileId.$extension" else fileId
            val physicalFile = File(uploadDir, storageFileName)

            try {
                val readChannel = part.provider()
                readChannel.copyTo(physicalFile.writeChannel())

                val actualSize = physicalFile.length()

                if (actualSize > MAX_FILE_SIZE) {
                    physicalFile.delete()
                    return@withContext UploadResult.Failed("File size exceeds the 10 MB limit")
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

                UploadResult.Success(MedicalRecord(fileId, originalName, actualSize, now))
            } catch (e: Exception) {
                if (physicalFile.exists()) physicalFile.delete()
                UploadResult.Failed("Upload failed: ${e.message}")
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

        // Delete file first, then DB — if file delete fails the DB record remains for retry
        val file = File(path)
        val fileExisted = file.exists()
        val fileDeleted = !fileExisted || file.delete()

        if (!fileDeleted) return false

        return repository.delete(id, userId)
    }

    fun renameRecord(id: String, userId: String, newName: String): Boolean {
        val currentRecord = repository.getRecordById(id, userId) ?: return false

        val sanitizedName = newName
            .replace("/", "")
            .replace("\\", "")
            .replace("\u0000", "")
            .let { name ->
                // Iteratively remove ".." until none remain (handles ".../", "..../" etc.)
                var cleaned = name
                do {
                    val prev = cleaned
                    cleaned = cleaned.replace("..", "")
                } while (cleaned != prev)
                cleaned
            }

        if (sanitizedName.isBlank()) return false

        val originalExtension = currentRecord.fileName.substringAfterLast(".", "")
        val newExtension = sanitizedName.substringAfterLast(".", "").lowercase()

        val finalName = if (originalExtension.isNotEmpty() && newExtension != originalExtension) {
            "$sanitizedName.$originalExtension"
        } else {
            sanitizedName
        }

        return repository.updateFileName(id, userId, finalName)
    }

    fun getPhysicalFile(id: String, userId: String): File? {
        val path = repository.getFilePath(id, userId) ?: return null
        val file = File(path)
        return if (file.exists()) file else null
    }

    fun reuploadRecord(id: String, userId: String, newFileName: String, fileBytes: ByteArray): UploadResult {
        val extension = newFileName.substringAfterLast(".", "").lowercase()

        if (extension.isNotEmpty() && extension !in ALLOWED_EXTENSIONS) {
            return UploadResult.Failed("File type '.$extension' is not allowed")
        }

        if (fileBytes.size > MAX_FILE_SIZE) {
            return UploadResult.Failed("File size exceeds the 10 MB limit")
        }

        val oldPath = repository.getFilePath(id, userId) ?: return UploadResult.Failed("Record not found")

        val safeName = if (extension.isNotEmpty() && !newFileName.lowercase().endsWith(".$extension")) {
            "$newFileName.$extension"
        } else {
            newFileName
        }

        val newTimestamp = System.currentTimeMillis()
        val storageFileName = if (extension.isNotEmpty()) "$id-$newTimestamp.$extension" else "$id-$newTimestamp"
        val newPath = "$uploadDir/$storageFileName"

        // Write to a temp file first — only promote to final path after DB update succeeds
        val tempFile = File("$newPath.tmp")
        return try {
            tempFile.writeBytes(fileBytes)

            val updated = repository.updateRecordMetadata(
                id = id,
                userId = userId,
                newName = safeName,
                newPath = newPath,
                newSize = fileBytes.size.toLong(),
                newTimestamp = newTimestamp,
            )

            if (updated) {
                tempFile.renameTo(File(newPath))
                // Clean up the old physical file after successful DB update
                val oldFile = File(oldPath)
                if (oldFile.exists()) oldFile.delete()
                UploadResult.Success(MedicalRecord(id, safeName, fileBytes.size.toLong(), newTimestamp))
            } else {
                tempFile.delete()
                UploadResult.Failed("Failed to update record metadata")
            }
        } catch (e: Exception) {
            tempFile.delete()
            UploadResult.Failed("Reupload failed: ${e.message}")
        }
    }
}
