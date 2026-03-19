package com.group8.comp2300.routes

import com.group8.comp2300.domain.model.medical.Medication
import com.group8.comp2300.domain.model.medical.MedicationCreateRequest
import com.group8.comp2300.domain.model.medical.MedicationFrequency
import com.group8.comp2300.domain.model.medical.MedicationLogLinkMode
import com.group8.comp2300.domain.model.medical.MedicationLog
import com.group8.comp2300.domain.model.medical.MedicationLogRequest
import com.group8.comp2300.domain.model.medical.MedicationLogStatus
import com.group8.comp2300.domain.model.medical.MedicationStatus
import com.group8.comp2300.domain.model.medical.Routine
import com.group8.comp2300.domain.model.medical.RoutineCreateRequest
import com.group8.comp2300.domain.model.medical.RoutineOccurrenceOverride
import com.group8.comp2300.domain.model.medical.RoutineOccurrenceOverrideRequest
import com.group8.comp2300.domain.model.medical.RoutineRepeatType
import com.group8.comp2300.domain.model.medical.RoutineStatus
import com.group8.comp2300.domain.model.medical.buildRoutineDayAgenda
import com.group8.comp2300.domain.repository.MedicationLogRepository
import com.group8.comp2300.domain.repository.MedicationRepository
import com.group8.comp2300.domain.repository.RoutineOccurrenceOverrideRepository
import com.group8.comp2300.domain.repository.RoutineRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime
import org.koin.ktor.ext.inject
import java.util.UUID
import kotlin.time.Instant

fun Route.medicationRoutes() {
    val medicationRepository: MedicationRepository by inject()
    val routineRepository: RoutineRepository by inject()
    val routineOccurrenceOverrideRepository: RoutineOccurrenceOverrideRepository by inject()
    val medicationLogRepository: MedicationLogRepository by inject()

    route("/api/medications") {
        get {
            withUserId { userId ->
                call.respond(HttpStatusCode.OK, medicationRepository.getAllByUserId(userId))
            }
        }

        put("/{id}") {
            withUserId { userId ->
                val id = call.parameters["id"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing medication id"))
                    return@withUserId
                }
                val request = call.receive<MedicationCreateRequest>()
                val medication = call.toMedication(userId = userId, id = id, request = request) ?: return@withUserId
                val existing = medicationRepository.getById(id)
                if (existing != null && existing.userId != userId) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Medication not found"))
                    return@withUserId
                }
                if (existing == null) {
                    medicationRepository.insert(medication)
                    call.respond(HttpStatusCode.Created, medication)
                } else {
                    medicationRepository.update(medication)
                    call.respond(HttpStatusCode.OK, medication)
                }
            }
        }

        delete("/{id}") {
            withUserId { userId ->
                val id = call.parameters["id"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing medication id"))
                    return@withUserId
                }
                val medication = medicationRepository.getById(id)
                if (medication == null || medication.userId != userId) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Medication not found"))
                    return@withUserId
                }
                medicationRepository.delete(id)
                call.respond(HttpStatusCode.OK, mapOf("message" to "Medication deleted"))
            }
        }

        get("/logs") {
            withUserId { userId ->
                call.respond(HttpStatusCode.OK, medicationLogRepository.getHistory(userId))
            }
        }

        post("/logs") {
            withUserId { userId ->
                val request = call.receive<MedicationLogRequest>()
                val medication = medicationRepository.getById(request.medicationId)
                if (medication == null || medication.userId != userId) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Medication not found"))
                    return@withUserId
                }
                val status = runCatching { MedicationLogStatus.valueOf(request.status) }.getOrElse {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid medication log status"))
                    return@withUserId
                }
                val linkMode = request.linkMode ?: if (request.routineId != null && request.occurrenceTimeMs != null) {
                    MedicationLogLinkMode.ATTACH_TO_OCCURRENCE
                } else {
                    MedicationLogLinkMode.EXTRA_DOSE
                }

                if (linkMode == MedicationLogLinkMode.ATTACH_TO_OCCURRENCE && (request.routineId == null || request.occurrenceTimeMs == null)) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "routineId and occurrenceTimeMs are required when attaching to a scheduled dose"),
                    )
                    return@withUserId
                }

                val routineId = request.routineId
                if (linkMode == MedicationLogLinkMode.ATTACH_TO_OCCURRENCE && routineId != null) {
                    val routine = routineRepository.getById(routineId)
                    if (routine == null || routine.userId != userId || request.medicationId !in routine.medicationIds) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Medication is not linked to the routine"))
                        return@withUserId
                    }
                }

                val timestamp = request.timestampMs ?: System.currentTimeMillis()
                val log = MedicationLog(
                    id =
                    if (linkMode == MedicationLogLinkMode.ATTACH_TO_OCCURRENCE && request.routineId != null && request.occurrenceTimeMs != null) {
                        "${request.routineId}:${request.medicationId}:${request.occurrenceTimeMs}"
                    } else {
                        UUID.randomUUID().toString()
                    },
                    medicationId = request.medicationId,
                    medicationTime = timestamp,
                    status = status,
                    routineId = request.routineId,
                    occurrenceTimeMs = request.occurrenceTimeMs,
                    medicationName = medication.name,
                    routineName = request.routineId?.let { routineRepository.getById(it)?.name },
                )
                medicationLogRepository.insert(log)
                call.respond(HttpStatusCode.Created, log)
            }
        }
    }

    route("/api/routines") {
        get {
            withUserId { userId ->
                call.respond(HttpStatusCode.OK, routineRepository.getAllByUserId(userId))
            }
        }

        get("/occurrence-overrides") {
            withUserId { userId ->
                call.respond(HttpStatusCode.OK, routineOccurrenceOverrideRepository.getAllByUserId(userId))
            }
        }

        put("/occurrence-overrides") {
            withUserId { userId ->
                val request = call.receive<RoutineOccurrenceOverrideRequest>()
                val override =
                    call.toRoutineOccurrenceOverride(
                        userId = userId,
                        request = request,
                        routineRepository = routineRepository,
                    ) ?: return@withUserId
                routineOccurrenceOverrideRepository.insert(override)
                call.respond(HttpStatusCode.OK, override)
            }
        }

        put("/{id}") {
            withUserId { userId ->
                val id = call.parameters["id"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing routine id"))
                    return@withUserId
                }
                val request = call.receive<RoutineCreateRequest>()
                val routine = call.toRoutine(userId = userId, id = id, request = request, medicationRepository = medicationRepository)
                    ?: return@withUserId
                val existing = routineRepository.getById(id)
                if (existing != null && existing.userId != userId) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Routine not found"))
                    return@withUserId
                }
                if (existing == null) {
                    routineRepository.insert(routine)
                    call.respond(HttpStatusCode.Created, routine)
                } else {
                    routineRepository.update(routine)
                    call.respond(HttpStatusCode.OK, routine)
                }
            }
        }

        delete("/{id}") {
            withUserId { userId ->
                val id = call.parameters["id"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing routine id"))
                    return@withUserId
                }
                val routine = routineRepository.getById(id)
                if (routine == null || routine.userId != userId) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Routine not found"))
                    return@withUserId
                }
                routineRepository.delete(id)
                call.respond(HttpStatusCode.OK, mapOf("message" to "Routine deleted"))
            }
        }

        get("/agenda") {
            withUserId { userId ->
                val dateString = call.request.queryParameters["date"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "date parameter is required"))
                    return@withUserId
                }
                val date = runCatching { LocalDate.parse(dateString) }.getOrElse {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "date must be in YYYY-MM-DD format"))
                    return@withUserId
                }
                val medications = medicationRepository.getAllByUserId(userId)
                val routines = routineRepository.getAllByUserId(userId)
                val overrides = routineOccurrenceOverrideRepository.getAllByUserId(userId)
                val logs = medicationLogRepository.getDailyAgenda(userId, dateString)
                val agenda = buildRoutineDayAgenda(
                    routines = routines,
                    medications = medications,
                    logs = logs,
                    overrides = overrides,
                    date = date,
                    nowMs = System.currentTimeMillis(),
                    timeZone = TimeZone.currentSystemDefault(),
                )
                call.respond(HttpStatusCode.OK, agenda)
            }
        }
    }
}

private suspend fun io.ktor.server.application.ApplicationCall.toRoutineOccurrenceOverride(
    userId: String,
    request: RoutineOccurrenceOverrideRequest,
    routineRepository: RoutineRepository,
): RoutineOccurrenceOverride? {
    val routine = routineRepository.getById(request.routineId)
    if (routine == null || routine.userId != userId) {
        respond(HttpStatusCode.NotFound, mapOf("error" to "Schedule not found"))
        return null
    }
    if (routine.status != RoutineStatus.ACTIVE) {
        respond(HttpStatusCode.BadRequest, mapOf("error" to "Only active schedules can be moved"))
        return null
    }
    if (request.rescheduledOccurrenceTimeMs <= 0L) {
        respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid rescheduled occurrence time"))
        return null
    }
    val timeZone = TimeZone.currentSystemDefault()
    val originalDateTime = Instant.fromEpochMilliseconds(request.originalOccurrenceTimeMs).toLocalDateTime(timeZone)
    val originalDate = originalDateTime.date
    val originalTimeOfDayMs = originalDateTime.toTimeOfDayMs()
    if (!routine.isDueOn(originalDate) || originalTimeOfDayMs !in routine.timesOfDayMs.sorted().distinct()) {
        respond(HttpStatusCode.BadRequest, mapOf("error" to "Original occurrence does not belong to this schedule"))
        return null
    }
    return RoutineOccurrenceOverride(
        id = "${routine.id}:${request.originalOccurrenceTimeMs}",
        routineId = routine.id,
        originalOccurrenceTimeMs = request.originalOccurrenceTimeMs,
        rescheduledOccurrenceTimeMs = request.rescheduledOccurrenceTimeMs,
    )
}

private suspend fun io.ktor.server.application.ApplicationCall.toMedication(
    userId: String,
    id: String,
    request: MedicationCreateRequest,
): Medication? {
    if (request.name.isBlank()) {
        respond(HttpStatusCode.BadRequest, mapOf("error" to "Medication name is required"))
        return null
    }
    if (request.dosage.isBlank()) {
        respond(HttpStatusCode.BadRequest, mapOf("error" to "Dosage is required"))
        return null
    }
    val frequency = runCatching { MedicationFrequency.valueOf(request.frequency) }.getOrElse {
        respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid medication type"))
        return null
    }
    val status = runCatching { MedicationStatus.valueOf(request.status) }.getOrElse {
        respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid medication status"))
        return null
    }
    return Medication(
        id = id,
        userId = userId,
        name = request.name.trim(),
        dosage = request.dosage.trim(),
        quantity = request.quantity.trim(),
        frequency = frequency,
        instruction = request.instruction?.trim()?.takeIf(String::isNotEmpty),
        colorHex = request.colorHex ?: Medication.PRESET_COLORS.random(),
        status = status,
    )
}

private suspend fun io.ktor.server.application.ApplicationCall.toRoutine(
    userId: String,
    id: String,
    request: RoutineCreateRequest,
    medicationRepository: MedicationRepository,
): Routine? {
    if (request.name.isBlank()) {
        respond(HttpStatusCode.BadRequest, mapOf("error" to "Routine name is required"))
        return null
    }
    if (request.startDate.isBlank()) {
        respond(HttpStatusCode.BadRequest, mapOf("error" to "Start date is required"))
        return null
    }
    val repeatType = runCatching { RoutineRepeatType.valueOf(request.repeatType) }.getOrElse {
        respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid routine repeat type"))
        return null
    }
    val status = runCatching { RoutineStatus.valueOf(request.status) }.getOrElse {
        respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid routine status"))
        return null
    }
    val dateRegex = Regex("^\\d{4}-\\d{2}-\\d{2}$")
    val endDate = request.endDate
    if (!dateRegex.matches(request.startDate) || (endDate != null && !dateRegex.matches(endDate))) {
        respond(HttpStatusCode.BadRequest, mapOf("error" to "Dates must be in YYYY-MM-DD format"))
        return null
    }
    if (repeatType == RoutineRepeatType.WEEKLY && request.daysOfWeek.isEmpty()) {
        respond(HttpStatusCode.BadRequest, mapOf("error" to "Select at least one day of week"))
        return null
    }
    if (repeatType == RoutineRepeatType.DAILY && request.daysOfWeek.isNotEmpty()) {
        respond(HttpStatusCode.BadRequest, mapOf("error" to "Daily routines cannot declare day selections"))
        return null
    }
    val normalizedTimes = request.timesOfDayMs.sorted().distinct()
    if (normalizedTimes.isEmpty()) {
        respond(HttpStatusCode.BadRequest, mapOf("error" to "Add at least one schedule time"))
        return null
    }
    val dayMs = 24L * 60L * 60L * 1000L
    if (normalizedTimes.any { it < 0L || it >= dayMs }) {
        respond(HttpStatusCode.BadRequest, mapOf("error" to "Schedule times must be within the day"))
        return null
    }
    val allowedOffsets = setOf(0, 5, 10, 15, 30, 60)
    if (request.reminderOffsetsMins.any { it !in allowedOffsets }) {
        respond(HttpStatusCode.BadRequest, mapOf("error" to "Unsupported reminder offset"))
        return null
    }
    val linkedMedications = medicationRepository.getAllByUserId(userId).associateBy(Medication::id)
    val invalidLink =
        request.medicationIds.any { medicationId ->
            val medication = linkedMedications[medicationId]
            medication == null || medication.status != MedicationStatus.ACTIVE
        }
    if (invalidLink) {
        respond(HttpStatusCode.BadRequest, mapOf("error" to "Schedules can only link active medications"))
        return null
    }
    return Routine(
        id = id,
        userId = userId,
        name = request.name.trim(),
        timesOfDayMs = normalizedTimes,
        repeatType = repeatType,
        daysOfWeek = request.daysOfWeek.sorted().distinct(),
        startDate = request.startDate,
        endDate = request.endDate?.takeIf(String::isNotBlank),
        hasReminder = request.hasReminder,
        reminderOffsetsMins = if (request.hasReminder) request.reminderOffsetsMins.sorted().distinct() else emptyList(),
        status = status,
        medicationIds = request.medicationIds.distinct(),
    )
}

private fun Routine.isDueOn(date: LocalDate): Boolean {
    val start = LocalDate.parse(startDate)
    val end = endDate?.takeIf(String::isNotBlank)?.let(LocalDate::parse)
    if (date < start || (end != null && date > end)) return false
    return when (repeatType) {
        RoutineRepeatType.DAILY -> true
        RoutineRepeatType.WEEKLY -> {
            val selected = daysOfWeek.toSet()
            selected.isNotEmpty() && date.dayOfWeek.toStorageDayOfWeek() in selected
        }
    }
}

private fun kotlinx.datetime.LocalDateTime.toTimeOfDayMs(): Long =
    ((hour * 60L + minute.toLong()) * 60L * 1000L) + (second * 1000L)

private fun kotlinx.datetime.DayOfWeek.toStorageDayOfWeek(): Int = isoDayNumber % 7
