package com.group8.comp2300.service

import com.group8.comp2300.domain.model.medical.MedicalRecord
import com.group8.comp2300.domain.repository.MedicalRecordRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MedicalRecordServiceTest {

    // 1. Mock the Repository (The database)
    private val repository = mockk<MedicalRecordRepository>()

    // 2. Instantiate the Service using a special test folder
    private val testUploadDir = "test_uploads"
    private val service = MedicalRecordService(repository, uploadDir = testUploadDir)

    @BeforeTest
    fun setup() {
        // Ensure the test folder exists before each test
        File(testUploadDir).mkdirs()
    }

    @AfterTest
    fun teardown() {
        // CRITICAL: Clean up the physical files so they don't clutter your PC
        File(testUploadDir).deleteRecursively()
    }

    @Test
    fun `renameRecord appends pdf extension if it is missing`() {
        // Arrange
        val id = "file-123"
        val userId = "user-1"
        // We expect the service to add ".pdf" and call the repository
        every { repository.updateFileName(id, userId, "Xray_Results.pdf") } returns true

        // Act
        val result = service.renameRecord(id, userId, "Xray_Results")

        // Assert
        assertTrue(result)
        verify(exactly = 1) { repository.updateFileName(id, userId, "Xray_Results.pdf") }
    }

    @Test
    fun `deleteRecord removes physical file when database delete succeeds`() {
        // Arrange
        val id = "file-456"
        val userId = "user-1"
        val mockFilePath = "$testUploadDir/test_file.pdf"

        // Create a real physical file in the test folder
        val physicalFile = File(mockFilePath)
        physicalFile.writeText("Dummy PDF content")
        assertTrue(physicalFile.exists()) // Confirm it's there

        every { repository.getFilePath(id, userId) } returns mockFilePath
        every { repository.delete(id, userId) } returns true // Simulate successful DB delete

        // Act
        val result = service.deleteRecord(id, userId)

        // Assert
        assertTrue(result)
        assertFalse(physicalFile.exists(), "The physical file should have been deleted from the disk")
    }
}
