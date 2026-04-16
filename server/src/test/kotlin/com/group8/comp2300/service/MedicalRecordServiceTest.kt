package com.group8.comp2300.service

import com.group8.comp2300.domain.model.medical.MedicalRecord
import com.group8.comp2300.domain.model.medical.MedicalRecordCategory
import com.group8.comp2300.domain.repository.MedicalRecordRepository
import com.group8.comp2300.security.AesGcmMedicalRecordCipher
import com.group8.comp2300.service.medicalRecords.MedicalRecordService
import com.group8.comp2300.service.medicalRecords.UploadResult
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.*

class MedicalRecordServiceTest {
    private val repository = mockk<MedicalRecordRepository>()
    private val testUploadDir = "test_uploads"
    private val cipher = AesGcmMedicalRecordCipher(ByteArray(32) { (it + 1).toByte() })
    private val service = MedicalRecordService(repository, uploadDir = testUploadDir, medicalRecordCipher = cipher)

    @BeforeTest
    fun setup() {
        File(testUploadDir).mkdirs()
    }

    @AfterTest
    fun teardown() {
        File(testUploadDir).deleteRecursively()
    }

    @Test
    fun `renameRecord preserves the original image extension`() {
        val id = "img-123"
        val userId = "user-1"

        val existingRecord = MedicalRecord(id, "skin_rash.png", 1024L, 1648000000L)
        every { repository.getRecordById(id, userId) } returns existingRecord
        every { repository.updateFileName(id, userId, "consultation_photo.png") } returns true

        val result = service.renameRecord(id, userId, "consultation_photo")

        assertTrue(result)
        verify { repository.updateFileName(id, userId, "consultation_photo.png") }
    }

    @Test
    fun `reuploadRecord replaces physical file and updates metadata`() {
        val id = "file-abc"
        val userId = "user-1"
        val oldPath = "$testUploadDir/old_file.pdf"
        val newContent = "New updated content".toByteArray()

        val oldFile = File(oldPath)
        oldFile.writeText("Old content")
        assertTrue(oldFile.exists())

        every { repository.getRecordById(id, userId) } returns MedicalRecord(
            id = id,
            fileName = "old_file.pdf",
            fileSize = oldFile.length(),
            createdAt = 123L,
            category = MedicalRecordCategory.GENERAL,
        )
        every { repository.getFilePath(id, userId) } returns oldPath
        every {
            repository.updateRecordMetadata(
                eq(id),
                eq(userId),
                any(),
                any(),
                any(),
                any(),
            )
        } returns true

        val result = service.reuploadRecord(id, userId, "updated_report.pdf", newContent)
        assertTrue(result is UploadResult.Success)
        assertFalse(oldFile.exists(), "The old physical file should have been deleted")

        verify {
            repository.updateRecordMetadata(
                id = id,
                userId = userId,
                newName = "updated_report.pdf",
                newPath = match { it.contains(id) && it.endsWith(".pdf.enc") },
                newSize = newContent.size.toLong(),
                newTimestamp = any(),
            )
        }
    }

    @Test
    fun `renameRecord appends original extension when renamed file has different extension`() {
        val id = "file-789"
        val userId = "user-1"
        val existingRecord = MedicalRecord(id, "test.pdf", 500L, 123L)

        every { repository.getRecordById(id, userId) } returns existingRecord
        every { repository.updateFileName(any(), any(), any()) } returns true

        service.renameRecord(id, userId, "malicious.exe")

        verify { repository.updateFileName(id, userId, "malicious.exe.pdf") }
    }

    @Test
    fun `renameRecord strips path traversal characters`() {
        val id = "file-123"
        val userId = "user-1"
        val existingRecord = MedicalRecord(id, "doc.pdf", 100L, 123L)

        every { repository.getRecordById(id, userId) } returns existingRecord
        every { repository.updateFileName(any(), any(), any()) } returns true

        service.renameRecord(id, userId, "../../../etc/passwd")

        verify {
            repository.updateFileName(
                id,
                userId,
                match { name ->
                    // Assert safety: no path separators, no parent-dir references, no null bytes
                    "/" !in name && "\\" !in name && ".." !in name && "\u0000" !in name
                },
            )
        }
    }

    @Test
    fun `renameRecord rejects blank names after sanitization`() {
        val id = "file-123"
        val userId = "user-1"
        val existingRecord = MedicalRecord(id, "doc.pdf", 100L, 123L)

        every { repository.getRecordById(id, userId) } returns existingRecord

        val result = service.renameRecord(id, userId, "../..")
        assertFalse(result)
    }

    @Test
    fun `reuploadRecord rejects unsupported file types`() {
        val id = "file-abc"
        val userId = "user-1"

        val result = service.reuploadRecord(id, userId, "malicious.html", "<script>alert(1)</script>".toByteArray())

        assertTrue(result is UploadResult.Failed)
        assertTrue(result.reason.contains("not allowed", ignoreCase = true))
    }

    @Test
    fun `reuploadRecord rejects oversized files`() {
        val id = "file-abc"
        val userId = "user-1"
        val oversizedBytes = ByteArray(11 * 1024 * 1024) // 11 MB

        val result = service.reuploadRecord(id, userId, "big.pdf", oversizedBytes)

        assertTrue(result is UploadResult.Failed)
        assertTrue(result.reason.contains("10 MB", ignoreCase = true))
    }

    @Test
    fun `reuploadRecord returns failed when record not found`() {
        val id = "ghost-id"
        val userId = "user-1"

        every { repository.getRecordById(id, userId) } returns null

        val result = service.reuploadRecord(id, userId, "report.pdf", "content".toByteArray())

        assertTrue(result is UploadResult.Failed)
        assertTrue(result.reason.contains("not found", ignoreCase = true))
    }

    @Test
    fun `deleteRecord returns false when record not found`() {
        every { repository.getFilePath("ghost", "user-1") } returns null

        assertFalse(service.deleteRecord("ghost", "user-1"))
    }

    @Test
    fun `uploadMedicalRecord rejects unsupported file types`() = runBlocking {
        val result = service.uploadMedicalRecord(
            userId = "user-1",
            fileName = "malicious.exe",
            fileBytes = "bad".toByteArray(),
            category = MedicalRecordCategory.OTHER,
        )

        assertTrue(result is UploadResult.Failed)
        assertTrue(result.reason.contains("not allowed", ignoreCase = true))
    }

    @Test
    fun `uploadMedicalRecord encrypts file contents at rest`() = runBlocking {
        val plainBytes = "Highly sensitive lab data".toByteArray()
        every {
            repository.insert(
                id = any(),
                userId = "user-1",
                fileName = "lab-report.pdf",
                storagePath = any(),
                fileSize = plainBytes.size.toLong(),
                createdAt = any(),
                category = MedicalRecordCategory.LAB_RESULT,
            )
        } just runs

        val result = service.uploadMedicalRecord(
            userId = "user-1",
            fileName = "lab-report.pdf",
            fileBytes = plainBytes,
            category = MedicalRecordCategory.LAB_RESULT,
        )

        assertTrue(result is UploadResult.Success)

        val encryptedFile = File(testUploadDir).listFiles()?.singleOrNull()
        assertNotNull(encryptedFile, "Encrypted upload should be written to disk")

        val encryptedBytes = encryptedFile.readBytes()
        assertFalse(encryptedBytes.contentEquals(plainBytes), "Stored bytes must not remain plaintext")
        assertContentEquals(plainBytes, cipher.decrypt(encryptedBytes))
    }

    @Test
    fun `getDownloadableRecord decrypts stored file bytes`() {
        val id = "file-1"
        val userId = "user-1"
        val plainBytes = "MRI scan".toByteArray()
        val encryptedFile = File(testUploadDir, "file-1.png.enc")
        encryptedFile.writeBytes(cipher.encrypt(plainBytes))

        every { repository.getRecordById(id, userId) } returns MedicalRecord(
            id = id,
            fileName = "scan.png",
            fileSize = plainBytes.size.toLong(),
            createdAt = 123L,
            category = MedicalRecordCategory.IMAGING,
        )
        every { repository.getFilePath(id, userId) } returns encryptedFile.path

        val result = service.getDownloadableRecord(id, userId)

        assertNotNull(result)
        assertContentEquals(plainBytes, result.fileBytes)
        assertEquals(io.ktor.http.ContentType.Image.PNG, result.contentType)
    }

    @Test
    fun `reuploadRecord cleans up temp file when DB update fails`() {
        val id = "file-abc"
        val userId = "user-1"
        val oldPath = "$testUploadDir/old_file.pdf"
        File(oldPath).writeText("Old content")

        every { repository.getRecordById(id, userId) } returns MedicalRecord(
            id = id,
            fileName = "old_file.pdf",
            fileSize = File(oldPath).length(),
            createdAt = 123L,
            category = MedicalRecordCategory.GENERAL,
        )
        every { repository.getFilePath(id, userId) } returns oldPath
        every {
            repository.updateRecordMetadata(any(), any(), any(), any(), any(), any())
        } returns false

        val result = service.reuploadRecord(id, userId, "report.pdf", "content".toByteArray())

        assertTrue(result is UploadResult.Failed)
        // Old file should still exist since DB update failed
        assertTrue(File(oldPath).exists(), "Old file should be preserved when DB update fails")
        // No temp files should be left behind
        val tempFiles = File(testUploadDir).listFiles()?.filter { it.name.endsWith(".tmp") }
        assertTrue(tempFiles.isNullOrEmpty(), "No temp files should remain after failure")
    }
}
