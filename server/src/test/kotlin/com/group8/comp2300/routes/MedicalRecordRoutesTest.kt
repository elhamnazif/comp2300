package com.group8.comp2300.routes

import com.group8.comp2300.domain.model.medical.MedicalRecord
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MedicalRecordRoutesIntegrationTest {

    @Test
    fun `full flow - rename record actually updates the database`() = testApplication {
        // 1. Setup REAL database in RAM
        val database = createServerDatabase("jdbc:sqlite::memory:")

        // 2. Setup REAL repository and REAL service
        val repository = MedicalRecordRepositoryImpl(database)
        val service = MedicalRecordService(repository, uploadDir = "test_uploads")

        // 3. Seed some "real" data into the RAM database
        repository.insert("file-123", "user-1", "OldName.pdf", "path", 100L, 100L)

        application {
            install(ContentNegotiation) { json() }
            install(Koin) {
                // Inject the REAL service, not a mock!
                modules(module { single { service } })
            }
            routing { medicalRecordRoutes() }
        }

        // 4. Act: Hit the real endpoint
        val response = client.patch("/api/medical-records/rename/file-123") {
            contentType(ContentType.Application.Json)
            setBody(RenameRequest("NewName"))
        }

        // 5. Assert: Check the DB to see if it ACTUALLY changed
        assertEquals(HttpStatusCode.OK, response.status)
        val updatedRecord = repository.getRecordsByUserId("user-1").first()
        assertEquals("NewName.pdf", updatedRecord.fileName)
    }
}
