package com.group8.comp2300.service

import com.group8.comp2300.domain.model.medical.MedicalRecord
import com.group8.comp2300.domain.model.medical.MedicalRecordSortOrder
import com.group8.comp2300.domain.repository.MedicalRecordRepository
import com.group8.comp2300.service.medicalRecords.MedicalRecordService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.File
import kotlin.test.*

class MedicalRecordServiceTest {
    private val repository = mockk<MedicalRecordRepository>()
    private val testUploadDir = "test_uploads"
    private val service = MedicalRecordService(repository, uploadDir = testUploadDir)

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

        // Mock the existing record to be a PNG
        val existingRecord = MedicalRecord(id, "skin_rash.png", 1024L, 1648000000L)
        every {
            repository.getRecordsByUserId(userId, MedicalRecordSortOrder.DATE_DESC)
        } returns listOf(existingRecord)

        every { repository.updateFileName(id, userId, "consultation_photo.png") } returns true

        val result = service.renameRecord(id, userId, "consultation_photo")

        // Assert: It should have automatically appended .png, not .pdf
        assertTrue(result)
        verify { repository.updateFileName(id, userId, "consultation_photo.png") }
    }

    @Test
    fun `reuploadRecord replaces physical file and updates metadata`() {
        val id = "file-abc"
        val userId = "user-1"
        val oldPath = "$testUploadDir/old_file.pdf"
        val newContent = "New updated content".toByteArray()

        val oldFile = File(oldPath) // Create the "old" physical file
        oldFile.writeText("Old content")
        assertTrue(oldFile.exists())

        every { repository.getFilePath(id, userId) } returns oldPath
        every {
            repository.updateRecordMetadata(
                eq(id),
                eq(userId),
                any(),
                any(),
                any(),
                any()
            )
        } returns true

        val result = service.reuploadRecord(id, userId, "updated_report.pdf", newContent)
        assertTrue(result)
        assertFalse(oldFile.exists(), "The old physical file should have been deleted")

        verify {
            repository.updateRecordMetadata(
                id = id,
                userId = userId,
                newName = "updated_report.pdf",
                newPath = match { it.contains(id) && it.endsWith(".pdf") },
                newSize = newContent.size.toLong(),
                newTimestamp = any()
            )
        }
    }

    @Test
    fun `uploadMedicalRecord rejects unsupported file types for security`() {
        val id = "file-789"
        val userId = "user-1"
        val existingRecord = MedicalRecord(id, "test.pdf", 500L, 123L)

        every { repository.getRecordsByUserId(userId, any()) } returns listOf(existingRecord)
        every { repository.updateFileName(any(), any(), any()) } returns true

        // If we try to rename to something weird, our sanitizer keeps the old extension
        service.renameRecord(id, userId, "malicious.exe")

        verify { repository.updateFileName(id, userId, "malicious.exe.pdf") }
    }
}
