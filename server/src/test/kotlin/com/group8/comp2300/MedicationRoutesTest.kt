package com.group8.comp2300

import com.group8.comp2300.data.repository.*
import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.domain.model.medical.*
import com.group8.comp2300.domain.model.user.Gender
import com.group8.comp2300.domain.model.user.SexualOrientation
import com.group8.comp2300.domain.repository.*
import com.group8.comp2300.infrastructure.database.createServerDatabase
import com.group8.comp2300.routes.medicationRoutes
import com.group8.comp2300.security.JwtService
import com.group8.comp2300.security.JwtServiceImpl
import io.ktor.client.call.*
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
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

class MedicationRoutesTest {
    @Test
    fun upsertMedicationReturnsCreated() = testApplication {
        val (_, _, _, token, _) = configureMedicationTestModuleWithUser()
        val client = jsonClient()

        val response = client.put("/api/medications/med-1") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.ContentType, "application/json")
            setBody(
                MedicationCreateRequest(
                    name = "Vitamin D",
                    dosage = "1 tablet",
                    quantity = "1000 IU",
                    frequency = MedicationFrequency.DAILY.name,
                ),
            )
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val medication = response.body<Medication>()
        assertEquals("med-1", medication.id)
        assertEquals("Vitamin D", medication.name)
    }

    @Test
    fun routineAgendaReturnsLinkedRoutineOccurrence() = testApplication {
        val (medicationRepository, routineRepository, _, token, userId) = configureMedicationTestModuleWithUser()
        val client = jsonClient()

        medicationRepository.insert(
            Medication(
                id = "med-1",
                userId = userId,
                name = "PrEP",
                dosage = "1 tablet",
                quantity = "30 tablets",
                frequency = MedicationFrequency.DAILY,
                instruction = null,
                colorHex = "#42A5F5",
            ),
        )
        routineRepository.insert(
            Routine(
                id = "routine-1",
                userId = userId,
                name = "Morning meds",
                timesOfDayMs = listOf(9 * 60 * 60 * 1000L),
                repeatType = RoutineRepeatType.DAILY,
                startDate = "2026-03-17",
                hasReminder = true,
                reminderOffsetsMins = listOf(0),
                status = RoutineStatus.ACTIVE,
                medicationIds = listOf("med-1"),
            ),
        )

        val response = client.get("/api/routines/agenda?date=2026-03-17") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val agenda = response.body<List<RoutineDayAgenda>>()
        assertEquals(1, agenda.size)
        assertEquals("Morning meds", agenda.single().routineName)
        assertEquals(listOf("med-1"), agenda.single().medications.map { it.medicationId })
    }

    @Test
    fun movingOccurrenceReturnsRescheduledAgendaOnTargetDay() = testApplication {
        val (medicationRepository, routineRepository, overrideRepository, token, userId) =
            configureMedicationTestModuleWithUser()
        val client = jsonClient()

        medicationRepository.insert(
            Medication(
                id = "med-1",
                userId = userId,
                name = "PrEP",
                dosage = "1 tablet",
                quantity = "30 tablets",
                frequency = MedicationFrequency.DAILY,
                instruction = null,
                colorHex = "#42A5F5",
            ),
        )
        routineRepository.insert(
            Routine(
                id = "routine-1",
                userId = userId,
                name = "Morning meds",
                timesOfDayMs = listOf(9 * 60 * 60 * 1000L),
                repeatType = RoutineRepeatType.DAILY,
                startDate = "2026-03-17",
                hasReminder = true,
                reminderOffsetsMins = listOf(0),
                status = RoutineStatus.ACTIVE,
                medicationIds = listOf("med-1"),
            ),
        )
        val originalTimestamp = LocalDateTime(2026, Month.MARCH, 17, 9, 0, 0, 0)
            .toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()
        val movedTimestamp = LocalDateTime(2026, Month.MARCH, 18, 11, 0, 0, 0)
            .toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()

        val moveResponse = client.put("/api/routines/occurrence-overrides") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.ContentType, "application/json")
            setBody(
                RoutineOccurrenceOverrideRequest(
                    routineId = "routine-1",
                    originalOccurrenceTimeMs = originalTimestamp,
                    rescheduledOccurrenceTimeMs = movedTimestamp,
                ),
            )
        }

        assertEquals(HttpStatusCode.OK, moveResponse.status)
        val override = moveResponse.body<RoutineOccurrenceOverride>()
        assertEquals("routine-1", override.routineId)
        assertEquals(1, overrideRepository.getAllByUserId(userId).size)

        val response = client.get("/api/routines/agenda?date=2026-03-18") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val agenda = response.body<List<RoutineDayAgenda>>()
        assertEquals(2, agenda.size)
        val rescheduledAgenda = agenda.first { it.isRescheduled }
        assertEquals(originalTimestamp, rescheduledAgenda.originalOccurrenceTimeMs)
        assertEquals(movedTimestamp, rescheduledAgenda.occurrenceTimeMs)
    }

    @Test
    fun loggingRoutineMedicationReturnsCreated() = testApplication {
        val (medicationRepository, routineRepository, _, token, userId) = configureMedicationTestModuleWithUser()
        val client = jsonClient()

        medicationRepository.insert(
            Medication(
                id = "med-1",
                userId = userId,
                name = "Vitamin D",
                dosage = "1 tablet",
                quantity = "1000 IU",
                frequency = MedicationFrequency.DAILY,
                instruction = null,
                colorHex = "#42A5F5",
            ),
        )
        routineRepository.insert(
            Routine(
                id = "routine-1",
                userId = userId,
                name = "Morning meds",
                timesOfDayMs = listOf(9 * 60 * 60 * 1000L),
                repeatType = RoutineRepeatType.DAILY,
                startDate = "2026-03-17",
                hasReminder = true,
                reminderOffsetsMins = listOf(0),
                status = RoutineStatus.ACTIVE,
                medicationIds = listOf("med-1"),
            ),
        )

        val response = client.post("/api/medications/logs") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.ContentType, "application/json")
            setBody(
                MedicationLogRequest(
                    medicationId = "med-1",
                    status = MedicationLogStatus.TAKEN.name,
                    timestampMs = 1_710_641_400_000,
                    routineId = "routine-1",
                    occurrenceTimeMs = 1_710_640_800_000,
                ),
            )
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(response.bodyAsText().contains("routine-1"))
    }

    @Test
    fun upsertRoutineAllowsLegacyOnDemandMedicationLinks() = testApplication {
        val (medicationRepository, _, _, token, userId) = configureMedicationTestModuleWithUser()
        val client = jsonClient()

        medicationRepository.insert(
            Medication(
                id = "med-ondemand",
                userId = userId,
                name = "Pain relief",
                dosage = "1 capsule",
                quantity = "20 capsules",
                frequency = MedicationFrequency.ON_DEMAND,
                instruction = null,
                colorHex = "#42A5F5",
            ),
        )

        val response = client.put("/api/routines/routine-1") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.ContentType, "application/json")
            setBody(
                RoutineCreateRequest(
                    name = "Morning meds",
                    timesOfDayMs = listOf(9 * 60 * 60 * 1000L, 21 * 60 * 60 * 1000L),
                    repeatType = RoutineRepeatType.DAILY.name,
                    startDate = "2026-03-17",
                    medicationIds = listOf("med-ondemand"),
                ),
            )
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun loggingExtraDoseDoesNotRequireRoutineLink() = testApplication {
        val (medicationRepository, _, _, token, userId) = configureMedicationTestModuleWithUser()
        val client = jsonClient()

        medicationRepository.insert(
            Medication(
                id = "med-1",
                userId = userId,
                name = "Pain relief",
                dosage = "1 capsule",
                quantity = "20 capsules",
                frequency = MedicationFrequency.ON_DEMAND,
                instruction = null,
                colorHex = "#42A5F5",
            ),
        )

        val response = client.post("/api/medications/logs") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.ContentType, "application/json")
            setBody(
                MedicationLogRequest(
                    medicationId = "med-1",
                    status = MedicationLogStatus.TAKEN.name,
                    timestampMs = 1_710_641_400_000,
                    linkMode = MedicationLogLinkMode.EXTRA_DOSE,
                ),
            )
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val log = response.body<MedicationLog>()
        assertEquals(null, log.routineId)
    }
}

private fun ApplicationTestBuilder.configureMedicationTestModuleWithUser(): Quintuple<
    MedicationRepository,
    RoutineRepository,
    RoutineOccurrenceOverrideRepository,
    String,
    String,
    > {
    val db = createServerDatabase("jdbc:sqlite::memory:")
    val jwtService = JwtServiceImpl("test-secret-key-must-be-long-enough", "test-issuer", "test-audience")
    val userRepo = UserRepositoryImpl(db)
    val refreshRepo = RefreshTokenRepositoryImpl(db, jwtService.refreshTokenExpiration)
    val medicationRepo = MedicationRepositoryImpl(db)
    val routineRepo = RoutineRepositoryImpl(db)
    val routineOccurrenceOverrideRepo = RoutineOccurrenceOverrideRepositoryImpl(db)
    val medicationLogRepo = MedicationLogRepositoryImpl(db)
    val userId = "user-1"
    userRepo.insert(
        id = userId,
        email = "user@example.com",
        passwordHash = "hash",
        firstName = "Test",
        lastName = "User",
        phone = "00000000",
        dateOfBirth = null,
        gender = Gender.PREFER_NOT_TO_SAY.name,
        sexualOrientation = SexualOrientation.PREFER_NOT_TO_SAY.name,
        preferredLanguage = "en",
    )
    userRepo.activateUser(userId)
    val token = jwtService.generateAccessToken(userId)

    application {
        configureTestModule(
            db,
            jwtService,
            userRepo,
            refreshRepo,
            medicationRepo,
            routineRepo,
            routineOccurrenceOverrideRepo,
            medicationLogRepo,
        )
    }
    return Quintuple(medicationRepo, routineRepo, routineOccurrenceOverrideRepo, token, userId)
}

private fun Application.configureTestModule(
    db: ServerDatabase,
    jwtService: JwtService,
    userRepository: UserRepository,
    refreshTokenRepository: RefreshTokenRepository,
    medicationRepository: MedicationRepository,
    routineRepository: RoutineRepository,
    routineOccurrenceOverrideRepository: RoutineOccurrenceOverrideRepository,
    medicationLogRepository: MedicationLogRepository,
) {
    install(Koin) {
        modules(
            module {
                single<ServerDatabase> { db }
                single<JwtService> { jwtService }
                single<UserRepository> { userRepository }
                single<RefreshTokenRepository> { refreshTokenRepository }
                single<MedicationRepository> { medicationRepository }
                single<RoutineRepository> { routineRepository }
                single<RoutineOccurrenceOverrideRepository> { routineOccurrenceOverrideRepository }
                single<MedicationLogRepository> { medicationLogRepository }
            },
        )
    }
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(Authentication) {
        jwt("auth-jwt") {
            verifier(jwtService.verifier)
            validate { credential -> JWTPrincipal(credential.payload) }
        }
    }
    routing {
        authenticate("auth-jwt") {
            medicationRoutes()
        }
    }
}

private fun ApplicationTestBuilder.jsonClient() = createClient {
    install(ClientContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}

@Serializable
private data class Quintuple<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)
