package com.group8.comp2300.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.group8.comp2300.data.repository.MedicalRecordRepositoryImpl
import com.group8.comp2300.domain.model.medical.MedicalRecordCategory
import com.group8.comp2300.domain.model.medical.MedicalRecordSortOrder
import com.group8.comp2300.dto.RenameRequest
import com.group8.comp2300.infrastructure.database.createServerDatabase
import com.group8.comp2300.security.AesGcmMedicalRecordCipher
import com.group8.comp2300.service.medicalRecords.MedicalRecordService
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import java.io.File
import kotlin.test.*

class MedicalRecordRoutesTest {

    private val testUploadDir = "test_uploads_api"
    private val testSecret = "test-key"
    private val cipher = AesGcmMedicalRecordCipher(ByteArray(32) { (it + 2).toByte() })
    private val testToken = JWT.create()
        .withSubject("user-1")
        .sign(Algorithm.HMAC256(testSecret))

    @BeforeTest
    fun setup() {
        File(testUploadDir).mkdirs()
    }

    @AfterTest
    fun cleanup() {
        File(testUploadDir).deleteRecursively()
    }

    private fun Application.configureTestEnv(
        database: com.group8.comp2300.database.ServerDatabase,
        repository: MedicalRecordRepositoryImpl,
        service: MedicalRecordService,
    ) {
        install(ContentNegotiation) { json() }

        install(Authentication) {
            jwt("auth-jwt") {
                verifier(JWT.require(Algorithm.HMAC256(testSecret)).build())
                validate { credential -> JWTPrincipal(credential.payload) }
            }
        }

        install(Koin) {
            modules(
                module {
                    single { database }
                    single { repository }
                    single { service }
                },
            )
        }

        routing {
            authenticate("auth-jwt") {
                medicalRecordRoutes()
            }
        }
    }

    @Test
    fun `full api - rename updates database`() = testApplication {
        val database = createServerDatabase("jdbc:sqlite::memory:")
        val repository = MedicalRecordRepositoryImpl(database)
        val service = MedicalRecordService(repository, uploadDir = testUploadDir, medicalRecordCipher = cipher)

        val client = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json()
            }
        }

        database.userQueries.insertUser(
            id = "user-1", email = "test@vita.com", passwordHash = "hash",
            firstName = "Test", lastName = "User", phone = null, dateOfBirth = null,
            gender = null, sexualOrientation = null, profileImageUrl = null,
            createdAt = 1700000000L, preferredLanguage = "en", isActivated = 1L, deactivatedAt = null,
        )

        repository.insert("file-1", "user-1", "Old.pdf", "path", 100, 100, MedicalRecordCategory.GENERAL)

        application { configureTestEnv(database, repository, service) }

        val requestedName = "Updated_Report"

        val response = client.patch("/api/medical-records/rename/file-1") {
            header(HttpHeaders.Authorization, "Bearer $testToken")
            contentType(ContentType.Application.Json)
            setBody(RenameRequest(newName = requestedName))
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val updatedRecord = repository.getRecordsByUserId("user-1", MedicalRecordSortOrder.DATE_DESC).first()
        assertEquals("$requestedName.pdf", updatedRecord.fileName)
    }

    @Test
    fun `full api - delete removes record`() = testApplication {
        val database = createServerDatabase("jdbc:sqlite::memory:")
        val repository = MedicalRecordRepositoryImpl(database)
        val service = MedicalRecordService(repository, uploadDir = testUploadDir, medicalRecordCipher = cipher)

        database.userQueries.insertUser(
            id = "user-1", email = "delete@vita.com", passwordHash = "hash",
            firstName = "Del", lastName = "User", phone = null, dateOfBirth = null,
            gender = null, sexualOrientation = null, profileImageUrl = null,
            createdAt = 1700000000L, preferredLanguage = "en", isActivated = 1L, deactivatedAt = null,
        )

        repository.insert("del-1", "user-1", "Bye.pdf", "path", 100, 100, MedicalRecordCategory.GENERAL)

        application { configureTestEnv(database, repository, service) }

        val response = client.delete("/api/medical-records/del-1") {
            header(HttpHeaders.Authorization, "Bearer $testToken")
        }

        assertEquals(HttpStatusCode.NoContent, response.status)

        val records = repository.getRecordsByUserId("user-1", MedicalRecordSortOrder.DATE_DESC)
        assertTrue(records.isEmpty(), "Record should be deleted from DB")
    }

    @Test
    fun `download - returns correct content types for pdf and images`() = testApplication {
        val database = createServerDatabase("jdbc:sqlite::memory:")
        val repository = MedicalRecordRepositoryImpl(database)
        val service = MedicalRecordService(repository, uploadDir = testUploadDir, medicalRecordCipher = cipher)

        application { configureTestEnv(database, repository, service) }

        database.userQueries.insertUser(
            id = "user-1", email = "down@vita.com", passwordHash = "hash",
            firstName = "Test", lastName = "User", phone = null, dateOfBirth = null,
            gender = null, sexualOrientation = null, profileImageUrl = null,
            createdAt = 1700000000L, preferredLanguage = "en", isActivated = 1L, deactivatedAt = null,
        )

        val filesToTest = listOf(
            Quadruple("pdf-1", "report.pdf", "Dummy content for report.pdf".toByteArray(), ContentType.Application.Pdf),
            Quadruple("img-1", "rash.png", "Dummy content for rash.png".toByteArray(), ContentType.Image.PNG),
            Quadruple("img-2", "scan.jpg", "Dummy content for scan.jpg".toByteArray(), ContentType.Image.JPEG),
        )

        filesToTest.forEach { (id, fileName, plainBytes, _) ->
            val physicalFile = File(testUploadDir, "$id.${fileName.substringAfterLast(".")}.enc")
            physicalFile.writeBytes(cipher.encrypt(plainBytes))

            repository.insert(
                id = id,
                userId = "user-1",
                fileName = fileName,
                storagePath = physicalFile.path,
                fileSize = plainBytes.size.toLong(),
                createdAt = System.currentTimeMillis(),
                category = MedicalRecordCategory.IMAGING,
            )
        }

        filesToTest.forEach { (id, fileName, plainBytes, expectedType) ->
            val response = client.get("/api/medical-records/download/$id") {
                header(HttpHeaders.Authorization, "Bearer $testToken")
            }

            assertEquals(HttpStatusCode.OK, response.status, "Failed for $fileName")

            val actualContentType = response.contentType()?.withoutParameters()
            assertEquals(expectedType, actualContentType, "Wrong MIME type for $fileName")
            assertContentEquals(
                plainBytes,
                response.bodyAsBytes(),
                "Download should return decrypted bytes for $fileName",
            )

            val disposition = response.headers[HttpHeaders.ContentDisposition]
            assertNotNull(disposition, "Content-Disposition header is missing")

            assertTrue(
                disposition.contains(fileName),
                "Header '$disposition' should contain filename '$fileName'",
            )
        }
    }

    @Test
    fun `unauthenticated request returns 401`() = testApplication {
        val database = createServerDatabase("jdbc:sqlite::memory:")
        val repository = MedicalRecordRepositoryImpl(database)
        val service = MedicalRecordService(repository, uploadDir = testUploadDir, medicalRecordCipher = cipher)

        application { configureTestEnv(database, repository, service) }

        val response = client.get("/api/medical-records/user")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
