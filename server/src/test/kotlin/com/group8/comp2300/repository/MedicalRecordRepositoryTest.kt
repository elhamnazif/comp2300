package com.group8.comp2300.repository

import com.group8.comp2300.data.repository.MedicalRecordRepositoryImpl
import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.infrastructure.database.createServerDatabase
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MedicalRecordRepositoryTest {

    private lateinit var database: ServerDatabase
    private lateinit var repository: MedicalRecordRepositoryImpl

    @BeforeTest
    fun setup() {
        database = createServerDatabase("jdbc:sqlite::memory:")
        repository = MedicalRecordRepositoryImpl(database)
    }

    @Test
    fun `insert and getRecordsByUserId successfully saves and maps data`() {
        val userId = "user_99"
        val fileId = "file_abc123"

        // Run SQL INSERT query
        repository.insert(
            id = fileId,
            userId = userId,
            fileName = "Blood_Test.pdf",
            storagePath = "uploads/file_abc123.pdf",
            fileSize = 1024L,
            createdAt = 1700000000L,
        )

        // Assert: run SQL SELECT query
        val records = repository.getRecordsByUserId(userId)

        assertEquals(1, records.size, "There should be exactly one record for this user")

        val savedRecord = records.first()
        assertEquals(fileId, savedRecord.id)
        assertEquals("Blood_Test.pdf", savedRecord.fileName)
        assertEquals(1024L, savedRecord.fileSize)
    }

    @Test
    fun `updateFileName successfully changes the name in the database`() {
        // Insert a baseline record
        val userId = "user_99"
        val fileId = "file_abc123"
        repository.insert(fileId, userId, "Old_Name.pdf", "path", 100L, 100L)

        // Update it
        val wasUpdated = repository.updateFileName(fileId, userId, "New_Name.pdf")

        assertTrue(wasUpdated, "updateFileName should return true when a row is modified")

        // Verify the state actually changed by fetching it again
        val records = repository.getRecordsByUserId(userId)
        assertEquals("New_Name.pdf", records.first().fileName, "The database row should reflect the new name")
    }

    @Test
    fun `delete removes the record completely`() {
        val userId = "user_99"
        val fileId = "file_abc123"
        repository.insert(fileId, userId, "To_Delete.pdf", "path", 100L, 100L)

        val wasDeleted = repository.delete(fileId, userId)

        assertTrue(wasDeleted, "Delete should return true on success")
        val records = repository.getRecordsByUserId(userId)
        assertTrue(records.isEmpty(), "The database should have no records for this user after deletion")
    }

    @Test
    fun `delete returns false if the record does not exist`() {
        // Act: Try to delete a ghost record
        val wasDeleted = repository.delete("ghost_id", "user_99")
        assertFalse(wasDeleted, "Delete should return false because no rows were affected")
    }
}
