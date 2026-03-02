package com.group8.comp2300

import com.group8.comp2300.data.repository.MedicationLogRepositoryImpl
import com.group8.comp2300.data.repository.MedicationRepositoryImpl
import com.group8.comp2300.data.repository.RefreshTokenRepositoryImpl
import com.group8.comp2300.data.repository.UserRepositoryImpl
import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.domain.model.medical.Medication
import com.group8.comp2300.domain.model.medical.MedicationCreateRequest
import com.group8.comp2300.domain.model.medical.MedicationFrequency
import com.group8.comp2300.domain.model.medical.MedicationLog
import com.group8.comp2300.domain.model.medical.MedicationLogRequest
import com.group8.comp2300.domain.model.medical.MedicationLogStatus
import com.group8.comp2300.domain.model.medical.MedicationStatus
import com.group8.comp2300.domain.repository.MedicationLogRepository
import com.group8.comp2300.domain.repository.MedicationRepository
import com.group8.comp2300.domain.repository.UserRepository
import com.group8.comp2300.infrastructure.database.createServerDatabase
import com.group8.comp2300.routes.medicationRoutes
import com.group8.comp2300.security.JwtService
import com.group8.comp2300.security.JwtServiceImpl
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MedicationRoutesTest {
    // ============== List Medications Tests ==============

    @Test
    fun listMedicationsRequiresAuthentication() = testApplication {
        configureMedicationTestModule()
        val client = jsonClient()

        val response = client.get("/api/medications")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun listMedicationsReturnsEmptyListForNewUser() = testApplication {
        val (_, accessToken) = configureMedicationTestModuleWithUser()
        val client = jsonClient()

        val response = client.get("/api/medications") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val medications = response.body<List<Medication>>()
        assertTrue(medications.isEmpty())
    }

    // ============== Create Medication Tests ==============

    @Test
    fun createMedicationRequiresAuthentication() = testApplication {
        configureMedicationTestModule()
        val client = jsonClient()

        val response = client.post("/api/medications") {
            header(HttpHeaders.ContentType, "application/json")
            setBody(
                mapOf(
                    "name" to "Test Med",
                    "dosage" to "1 pill",
                    "startDate" to "2026-01-01",
                    "endDate" to "2026-12-31",
                ),
            )
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun createMedicationWithValidDataReturnsCreated() = testApplication {
        val (_, accessToken) = configureMedicationTestModuleWithUser()
        val client = jsonClient()

        val response = client.post("/api/medications") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            header(HttpHeaders.ContentType, "application/json")
            setBody(
                MedicationCreateRequest(
                    name = "Test Medication",
                    dosage = "2 pills",
                    quantity = "100mg",
                    frequency = "DAILY",
                    startDate = "2026-01-01",
                    endDate = "2026-12-31",
                    hasReminder = true,
                ),
            )
        }

        assertEquals(HttpStatusCode.Created, response.status, response.bodyAsText())
        val medication = response.body<Medication>()
        assertEquals("Test Medication", medication.name)
        assertEquals("2 pills", medication.dosage)
        assertEquals(MedicationFrequency.DAILY, medication.frequency)
        assertEquals(MedicationStatus.ACTIVE, medication.status)
    }

    @Test
    fun createMedicationWithBlankNameReturnsBadRequest() = testApplication {
        val (_, accessToken) = configureMedicationTestModuleWithUser()
        val client = jsonClient()

        val response = client.post("/api/medications") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            header(HttpHeaders.ContentType, "application/json")
            setBody(
                MedicationCreateRequest(
                    name = "",
                    dosage = "1 pill",
                    startDate = "2026-01-01",
                    endDate = "2026-12-31",
                ),
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("Medication name is required", response.body<ErrorResponse>().error)
    }

    @Test
    fun createMedicationWithBlankDosageReturnsBadRequest() = testApplication {
        val (_, accessToken) = configureMedicationTestModuleWithUser()
        val client = jsonClient()

        val response = client.post("/api/medications") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            header(HttpHeaders.ContentType, "application/json")
            setBody(
                MedicationCreateRequest(
                    name = "Test Med",
                    dosage = "",
                    startDate = "2026-01-01",
                    endDate = "2026-12-31",
                ),
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("Dosage is required", response.body<ErrorResponse>().error)
    }

    @Test
    fun createMedicationWithMissingStartDateReturnsBadRequest() = testApplication {
        val (_, accessToken) = configureMedicationTestModuleWithUser()
        val client = jsonClient()

        val response = client.post("/api/medications") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            header(HttpHeaders.ContentType, "application/json")
            setBody(
                MedicationCreateRequest(
                    name = "Test Med",
                    dosage = "1 pill",
                    startDate = "",
                    endDate = "2026-12-31",
                ),
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("Start date is required", response.body<ErrorResponse>().error)
    }

    @Test
    fun createMedicationWithInvalidDateFormatReturnsBadRequest() = testApplication {
        val (_, accessToken) = configureMedicationTestModuleWithUser()
        val client = jsonClient()

        val response = client.post("/api/medications") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            header(HttpHeaders.ContentType, "application/json")
            setBody(
                MedicationCreateRequest(
                    name = "Test Med",
                    dosage = "1 pill",
                    startDate = "01-01-2026",
                    endDate = "2026-12-31",
                ),
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("Start date must be in YYYY-MM-DD format", response.body<ErrorResponse>().error)
    }

    @Test
    fun createMedicationWithInvalidFrequencyReturnsBadRequest() = testApplication {
        val (_, accessToken) = configureMedicationTestModuleWithUser()
        val client = jsonClient()

        val response = client.post("/api/medications") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            header(HttpHeaders.ContentType, "application/json")
            setBody(
                MedicationCreateRequest(
                    name = "Test Med",
                    dosage = "1 pill",
                    frequency = "INVALID",
                    startDate = "2026-01-01",
                    endDate = "2026-12-31",
                ),
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.body<ErrorResponse>().error.contains("Invalid frequency"))
    }

    // ============== Medication Agenda Tests ==============

    @Test
    fun getMedicationAgendaRequiresAuthentication() = testApplication {
        configureMedicationTestModule()
        val client = jsonClient()

        val response = client.get("/api/medications/agenda?date=2026-01-15")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun getMedicationAgendaWithMissingDateReturnsBadRequest() = testApplication {
        val (_, accessToken) = configureMedicationTestModuleWithUser()
        val client = jsonClient()

        val response = client.get("/api/medications/agenda") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.body<ErrorResponse>().error.contains("date parameter is required"))
    }

    @Test
    fun getMedicationAgendaWithValidDateReturnsOk() = testApplication {
        val (_, accessToken) = configureMedicationTestModuleWithUser()
        val client = jsonClient()

        val response = client.get("/api/medications/agenda?date=2026-01-15") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val agenda = response.body<List<MedicationLog>>()
        assertTrue(agenda.isEmpty())
    }

    // ============== Medication Log Tests ==============

    @Test
    fun logMedicationRequiresAuthentication() = testApplication {
        configureMedicationTestModule()
        val client = jsonClient()

        val response = client.post("/api/medications/logs") {
            header(HttpHeaders.ContentType, "application/json")
            setBody(
                MedicationLogRequest(
                    medicationId = "med-123",
                    status = "TAKEN",
                ),
            )
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun logMedicationWithInvalidStatusReturnsBadRequest() = testApplication {
        val (_, accessToken) = configureMedicationTestModuleWithUser()
        val client = jsonClient()

        val response = client.post("/api/medications/logs") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            header(HttpHeaders.ContentType, "application/json")
            setBody(
                MedicationLogRequest(
                    medicationId = "med-123",
                    status = "INVALID",
                ),
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("Invalid status", response.body<ErrorResponse>().error)
    }

    @Test
    fun logMedicationWithValidDataReturnsCreated() = testApplication {
        val (medRepo, accessToken) = configureMedicationTestModuleWithUser()
        val client = jsonClient()

        // First create a medication
        val createResponse = client.post("/api/medications") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            header(HttpHeaders.ContentType, "application/json")
            setBody(
                MedicationCreateRequest(
                    name = "Test Med",
                    dosage = "1 pill",
                    startDate = "2026-01-01",
                    endDate = "2026-12-31",
                ),
            )
        }
        val medication = createResponse.body<Medication>()

        // Then log it
        val logResponse = client.post("/api/medications/logs") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            header(HttpHeaders.ContentType, "application/json")
            setBody(
                MedicationLogRequest(
                    medicationId = medication.id,
                    status = "TAKEN",
                ),
            )
        }

        assertEquals(HttpStatusCode.Created, logResponse.status, logResponse.bodyAsText())
        val log = logResponse.body<MedicationLog>()
        assertEquals(medication.id, log.medicationId)
        assertEquals(MedicationLogStatus.TAKEN, log.status)
    }

    @Serializable
    private data class ErrorResponse(val error: String)
}

// ============== Test Helpers ==============

private data class MedicationTestSetup(
    val medicationRepository: MedicationRepository,
    val accessToken: String,
)

private fun ApplicationTestBuilder.configureMedicationTestModule() {
    val database = createServerDatabase("jdbc:sqlite::memory:")
    val testModule = module {
        single<ServerDatabase> { database }
        single<MedicationRepository> { MedicationRepositoryImpl(get()) }
        single<MedicationLogRepository> { MedicationLogRepositoryImpl(get()) }
    }

    application {
        install(Koin) {
            slf4jLogger()
            modules(testModule)
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        routing {
            medicationRoutes()
        }
    }
}

private fun ApplicationTestBuilder.configureMedicationTestModuleWithUser(): MedicationTestSetup {
    val database = createServerDatabase("jdbc:sqlite::memory:")
    val jwtService = testJwtService()
    val userRepository = UserRepositoryImpl(database)
    val medicationRepository = MedicationRepositoryImpl(database)

    val testModule = module {
        single<ServerDatabase> { database }
        single<MedicationRepository> { medicationRepository }
        single<MedicationLogRepository> { MedicationLogRepositoryImpl(get()) }
    }

    // Create a test user and get tokens
    val userId = "user_test-${java.util.UUID.randomUUID()}"
    userRepository.insert(
        id = userId,
        email = "med.test@example.com",
        passwordHash = "hash",
        firstName = "Med",
        lastName = "Test",
        phone = null,
        dateOfBirth = null,
        gender = null,
        sexualOrientation = null,
        preferredLanguage = "en",
    )
    userRepository.activateUser(userId)

    val accessToken = jwtService.generateAccessToken(userId)

    application {
        install(Koin) {
            slf4jLogger()
            modules(testModule)
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }

        install(Authentication) {
            jwt("auth-jwt") {
                realm = "comp2300-test"
                verifier(jwtService.verifier)
                validate { credential ->
                    val userId = credential.payload.subject
                    if (userId != null) {
                        JWTPrincipal(credential.payload)
                    } else {
                        null
                    }
                }
            }
        }

        routing {
            authenticate("auth-jwt") {
                medicationRoutes()
            }
        }
    }

    return MedicationTestSetup(medicationRepository, accessToken)
}

private fun ApplicationTestBuilder.jsonClient() = createClient {
    install(ClientContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
            },
        )
    }
}

private fun testJwtService(): JwtService = JwtServiceImpl(
    secret = "test-secret-for-medication-routes",
    issuer = "http://localhost/test",
    audience = "http://localhost/test",
)
