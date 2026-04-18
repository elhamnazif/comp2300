package com.group8.comp2300

import com.group8.comp2300.data.repository.AppointmentRepositoryImpl
import com.group8.comp2300.data.repository.AppointmentSlotRepositoryImpl
import com.group8.comp2300.data.repository.ClinicRepositoryImpl
import com.group8.comp2300.data.repository.ClinicTagRepositoryImpl
import com.group8.comp2300.data.repository.UserRepositoryImpl
import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.domain.model.medical.Appointment
import com.group8.comp2300.domain.model.medical.AppointmentSlot
import com.group8.comp2300.domain.model.medical.ClinicBookingRequest
import com.group8.comp2300.domain.repository.AppointmentRepository
import com.group8.comp2300.domain.repository.AppointmentSlotRepository
import com.group8.comp2300.domain.repository.ClinicRepository
import com.group8.comp2300.domain.repository.ClinicTagRepository
import com.group8.comp2300.domain.repository.UserRepository
import com.group8.comp2300.infrastructure.database.createServerDatabase
import com.group8.comp2300.routes.appointmentRoutes
import com.group8.comp2300.security.JwtService
import com.group8.comp2300.security.JwtServiceImpl
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

class AppointmentRoutesTest {
    @Test
    fun bookingSupportsClinicFlowAndCancellationReleasesSlot() = testApplication {
        val fixture = configureAppointmentTestModuleWithUser()
        val client = jsonClient()

        val createResponse = client.post("/api/appointments") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                ClinicBookingRequest(
                    clinicId = fixture.clinicId,
                    slotId = fixture.initialSlotId,
                    appointmentType = "STI_TESTING",
                    reason = "Screening",
                    hasReminder = true,
                ),
            )
        }

        assertEquals(HttpStatusCode.Created, createResponse.status, createResponse.bodyAsText())
        val created = createResponse.body<Appointment>()
        assertEquals("STI_TESTING", created.appointmentType)
        assertTrue(fixture.slotRepository.getSlotById(fixture.initialSlotId)?.isBooked == true)

        val cancelResponse = client.post("/api/appointments/${created.id}/cancel") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
        }

        assertEquals(HttpStatusCode.OK, cancelResponse.status, cancelResponse.bodyAsText())
        val cancelled = cancelResponse.body<Appointment>()
        assertEquals("CANCELLED", cancelled.status)
        assertEquals(null, cancelled.bookingId)
        assertFalse(fixture.slotRepository.getSlotById(fixture.initialSlotId)?.isBooked == true)

        val historyResponse = client.get("/api/appointments") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
        }

        assertEquals(HttpStatusCode.OK, historyResponse.status)
        val history = historyResponse.body<List<Appointment>>()
        assertEquals(listOf("CANCELLED"), history.map(Appointment::status))
    }

    @Test
    fun reschedulingMovesAppointmentToNewSlotAndFreesOriginalSlot() = testApplication {
        val fixture = configureAppointmentTestModuleWithUser()
        val client = jsonClient()

        val created = client.post("/api/appointments") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                ClinicBookingRequest(
                    clinicId = fixture.clinicId,
                    slotId = fixture.initialSlotId,
                    appointmentType = "STI_TESTING",
                    reason = "Screening",
                    hasReminder = true,
                ),
            )
        }.body<Appointment>()

        val rescheduleResponse = client.post("/api/appointments/${created.id}/reschedule") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                ClinicBookingRequest(
                    clinicId = fixture.clinicId,
                    slotId = fixture.rescheduleSlotId,
                    appointmentType = "FOLLOW_UP",
                    reason = "Bring results",
                    hasReminder = false,
                ),
            )
        }

        assertEquals(HttpStatusCode.OK, rescheduleResponse.status, rescheduleResponse.bodyAsText())
        val rescheduled = rescheduleResponse.body<Appointment>()
        assertEquals(fixture.rescheduleSlotId, rescheduled.bookingId)
        assertEquals("FOLLOW_UP", rescheduled.appointmentType)
        assertEquals("Bring results", rescheduled.notes)
        assertFalse(rescheduled.hasReminder)
        assertFalse(fixture.slotRepository.getSlotById(fixture.initialSlotId)?.isBooked == true)
        assertTrue(fixture.slotRepository.getSlotById(fixture.rescheduleSlotId)?.isBooked == true)
    }

    @Test
    fun reschedulingToBookedSlotReturnsConflict() = testApplication {
        val fixture = configureAppointmentTestModuleWithUser()
        val client = jsonClient()

        val created = client.post("/api/appointments") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                ClinicBookingRequest(
                    clinicId = fixture.clinicId,
                    slotId = fixture.initialSlotId,
                    appointmentType = "STI_TESTING",
                    reason = "Screening",
                    hasReminder = true,
                ),
            )
        }.body<Appointment>()

        val rescheduleResponse = client.post("/api/appointments/${created.id}/reschedule") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                ClinicBookingRequest(
                    clinicId = fixture.clinicId,
                    slotId = fixture.bookedSlotId,
                    appointmentType = "FOLLOW_UP",
                    reason = "Bring results",
                    hasReminder = true,
                ),
            )
        }

        assertEquals(HttpStatusCode.Conflict, rescheduleResponse.status)
        assertTrue(fixture.slotRepository.getSlotById(fixture.initialSlotId)?.isBooked == true)
        assertTrue(fixture.slotRepository.getSlotById(fixture.bookedSlotId)?.isBooked == true)
    }
}

private data class AppointmentRouteFixture(
    val accessToken: String,
    val clinicId: String,
    val initialSlotId: String,
    val rescheduleSlotId: String,
    val bookedSlotId: String,
    val slotRepository: AppointmentSlotRepository,
)

private fun ApplicationTestBuilder.configureAppointmentTestModuleWithUser(): AppointmentRouteFixture {
    val database = createServerDatabase("jdbc:sqlite::memory:")
    val jwtService = testAppointmentJwtService()
    val userRepository = UserRepositoryImpl(database)
    val clinicTagRepository = ClinicTagRepositoryImpl(database)
    val slotRepository = AppointmentSlotRepositoryImpl(database)
    val clinicRepository = ClinicRepositoryImpl(database, clinicTagRepository, slotRepository)
    val appointmentRepository = AppointmentRepositoryImpl(database)

    val testModule = module {
        single<ServerDatabase> { database }
        single<AppointmentRepository> { appointmentRepository }
        single<AppointmentSlotRepository> { slotRepository }
        single<ClinicTagRepository> { clinicTagRepository }
        single<ClinicRepository> { clinicRepository }
        single<UserRepository> { userRepository }
    }

    val userId = "user_appt_${java.util.UUID.randomUUID()}"
    userRepository.insert(
        id = userId,
        email = "appointment.test@example.com",
        passwordHash = "hash",
        firstName = "Appointment",
        lastName = "Test",
        phone = null,
        dateOfBirth = null,
        gender = null,
        sexualOrientation = null,
        preferredLanguage = "en",
    )
    userRepository.activateUser(userId)

    val clinicId = "clinic-001"
    database.clinicQueries.insertClinic(
        id = clinicId,
        name = "City Sexual Health",
        address = "123 Test St",
        phone = "123456",
        latitude = 3.139,
        longitude = 101.6869,
    )

    val now = System.currentTimeMillis()
    val initialSlot = AppointmentSlot(
        id = "slot-initial",
        clinicId = clinicId,
        startTime = now + 86_400_000L,
        endTime = now + 87_000_000L,
        isBooked = false,
    )
    val rescheduleSlot = AppointmentSlot(
        id = "slot-reschedule",
        clinicId = clinicId,
        startTime = now + 172_800_000L,
        endTime = now + 173_400_000L,
        isBooked = false,
    )
    val bookedSlot = AppointmentSlot(
        id = "slot-booked",
        clinicId = clinicId,
        startTime = now + 259_200_000L,
        endTime = now + 259_800_000L,
        isBooked = true,
    )
    slotRepository.createSlot(initialSlot)
    slotRepository.createSlot(rescheduleSlot)
    slotRepository.createSlot(bookedSlot)

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
                    credential.payload.subject?.let { JWTPrincipal(credential.payload) }
                }
            }
        }
        routing {
            authenticate("auth-jwt") {
                appointmentRoutes()
            }
        }
    }

    return AppointmentRouteFixture(
        accessToken = jwtService.generateAccessToken(userId),
        clinicId = clinicId,
        initialSlotId = initialSlot.id,
        rescheduleSlotId = rescheduleSlot.id,
        bookedSlotId = bookedSlot.id,
        slotRepository = slotRepository,
    )
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

private fun testAppointmentJwtService(): JwtService = JwtServiceImpl(
    secret = "test-secret-for-appointment-routes",
    issuer = "http://localhost/test",
    audience = "http://localhost/test",
)
