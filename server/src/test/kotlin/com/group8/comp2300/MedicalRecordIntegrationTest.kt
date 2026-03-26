package com.group8.comp2300

import com.group8.comp2300.data.repository.MedicalRecordRepositoryImpl
import com.group8.comp2300.infrastructure.database.createServerDatabase
import com.group8.comp2300.service.medicalRecords.MedicalRecordService
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MedicalRecordIntegrationTest {

    private val testUploadDir = "test_uploads_temp"

    @AfterTest
    fun cleanup() {
        // Remove physical files created during tests
        File(testUploadDir).deleteRecursively()
    }

    @Test
    fun `full integration - rename record updates in-memory database`() = testApplication {
        // 1. GIVEN: A real In-Memory Database and Service
        val database = createServerDatabase("jdbc:sqlite::memory:")
        val repository = MedicalRecordRepositoryImpl(database)
        val service = MedicalRecordService(repository, uploadDir = testUploadDir)

        // Seed the database with a record
        val fileId = "test-uuid-123"
        val userId = "user-test-001"
        repository.insert(fileId, userId, "Original.pdf", "path/to/file", 1024L, 123456L)

        // 2. SETUP: Configure the Ktor Test Module
        application {
            install(ContentNegotiation) { json() }
            install(Koin) {
                modules(module {
                    single { service } // Inject the real service
                })
            }
            routing {
                medicalRecordRoutes() // Your real routes
            }
        }

        // 3. WHEN: We call the PATCH rename endpoint
        val response = client.patch("/api/medical-records/rename/$fileId") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("newName" to "NewHealthReport"))
            // In a real test, you'd add your auth header here if needed
        }

        // 4. THEN: Check HTTP status AND verify the database state
        assertEquals(HttpStatusCode.OK, response.status)

        val updatedRecords = repository.getRecordsByUserId(userId)
        assertEquals("NewHealthReport.pdf", updatedRecords.first().fileName)
    }

    @Test
    fun `full integration - delete record removes from in-memory db`() = testApplication {
        val database = createServerDatabase("jdbc:sqlite::memory:")
        val repository = MedicalRecordRepositoryImpl(database)
        val service = MedicalRecordService(repository, uploadDir = testUploadDir)

        val fileId = "del-123"
        val userId = "user-001"
        repository.insert(fileId, userId, "DeleteMe.pdf", "path", 100L, 100L)

        application {
            install(ContentNegotiation) { json() }
            install(Koin) { modules(module { single { service } }) }
            routing { medicalRecordRoutes() }
        }

        // Act
        val response = client.delete("/api/medical-records/$fileId")

        // Assert
        assertEquals(HttpStatusCode.NoContent, response.status)
        assertTrue(repository.getRecordsByUserId(userId).isEmpty())
    }
}
