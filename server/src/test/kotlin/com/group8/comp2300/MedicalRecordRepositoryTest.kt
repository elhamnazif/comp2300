package com.group8.comp2300

import com.group8.comp2300.infrastructure.database.createServerDatabase
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class MedicalRecordRepositoryTest {

    // 1. Declare the variables we will use in our tests
    // We use 'Any' or your specific Database class type here
    private lateinit var database: Any // Replace 'Any' with your actual ServerDatabase type
    private lateinit var repository: MedicalRecordRepositoryImpl

    @BeforeTest
    fun setup() {
        // 2. The Magic Trick: A fresh database before EVERY single test!
        // This ensures Test A doesn't leave garbage data that ruins Test B.
        database = createServerDatabase("jdbc:sqlite::memory:")
        repository = MedicalRecordRepositoryImpl(database) // Pass the real, in-memory DB
    }

    @Test
    fun `insert and getRecordsByUserId successfully saves and maps data`() {
        // Arrange
        val userId = "user_99"
        val fileId = "file_abc123"

        // Act: Run the real SQL INSERT query
        repository.insert(
            id = fileId,
            userId = userId,
            fileName = "Blood_Test.pdf",
            storagePath = "uploads/file_abc123.pdf",
            fileSize = 1024L,
            createdAt = 1700000000L
        )

        // Assert: Run the real SQL SELECT query
        val records = repository.getRecordsByUserId(userId)

        assertEquals(1, records.size, "There should be exactly one record for this user")

        val savedRecord = records.first()
        assertEquals(fileId, savedRecord.id)
        assertEquals("Blood_Test.pdf", savedRecord.fileName)
        assertEquals(1024L, savedRecord.fileSize)
    }

    @Test
    fun `updateFileName successfully changes the name in the database`() {
        // Arrange: Insert a baseline record
        val userId = "user_99"
        val fileId = "file_abc123"
        repository.insert(fileId, userId, "Old_Name.pdf", "path", 100L, 100L)

        // Act: Try to update it
        val wasUpdated = repository.updateFileName(fileId, userId, "New_Name.pdf")

        // Assert
        assertTrue(wasUpdated, "updateFileName should return true when a row is modified")

        // Verify the state actually changed by fetching it again
        val records = repository.getRecordsByUserId(userId)
        assertEquals("New_Name.pdf", records.first().fileName, "The database row should reflect the new name")
    }

    @Test
    fun `delete removes the record completely`() {
        // Arrange
        val userId = "user_99"
        val fileId = "file_abc123"
        repository.insert(fileId, userId, "To_Delete.pdf", "path", 100L, 100L)

        // Act
        val wasDeleted = repository.delete(fileId, userId)

        // Assert
        assertTrue(wasDeleted, "Delete should return true on success")

        val records = repository.getRecordsByUserId(userId)
        assertTrue(records.isEmpty(), "The database should have no records for this user after deletion")
    }

    @Test
    fun `delete returns false if the record does not exist`() {
        // Arrange: Empty database (because @BeforeTest runs first)

        // Act: Try to delete a ghost record
        val wasDeleted = repository.delete("ghost_id", "user_99")

        // Assert
        assertFalse(wasDeleted, "Delete should return false because no rows were affected")
    }
}
