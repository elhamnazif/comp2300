package com.group8.comp2300.routes

import com.group8.comp2300.domain.model.medical.Medication
import com.group8.comp2300.domain.model.medical.MedicationCreateRequest
import com.group8.comp2300.domain.model.medical.MedicationFrequency
import com.group8.comp2300.domain.model.medical.MedicationLog
import com.group8.comp2300.domain.model.medical.MedicationLogRequest
import com.group8.comp2300.domain.model.medical.MedicationLogStatus
import com.group8.comp2300.domain.model.medical.MedicationStatus
import com.group8.comp2300.domain.repository.MedicationLogRepository
import com.group8.comp2300.domain.repository.MedicationRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject
import java.util.UUID

fun Route.medicationRoutes() {
    val medicationLogRepository: MedicationLogRepository by inject()
    val medicationRepository: MedicationRepository by inject()

    route("/api/medications") {
        // List user's medications, including archived entries.
        get {
            withUserId { userId ->
                val medications = medicationRepository.getAllByUserId(userId)
                call.respond(HttpStatusCode.OK, medications)
            }
        }

        put("/{id}") {
            withUserId { userId ->
                val id = call.parameters["id"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing medication id"))
                    return@withUserId
                }
                val request = call.receive<MedicationCreateRequest>()
                val validated = call.validateMedicationRequest(request) ?: return@withUserId

                val existing = medicationRepository.getById(id)
                if (existing != null && existing.userId != userId) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Medication not found"))
                    return@withUserId
                }

                val medication = Medication(
                    id = id,
                    userId = userId,
                    name = request.name,
                    dosage = request.dosage,
                    quantity = request.quantity,
                    frequency = validated.frequency,
                    instruction = request.instruction,
                    colorHex = request.colorHex ?: existing?.colorHex ?: Medication.PRESET_COLORS.random(),
                    startDate = request.startDate,
                    endDate = request.endDate,
                    hasReminder = request.hasReminder,
                    status = validated.status,
                )

                if (existing == null) {
                    medicationRepository.insert(medication)
                    call.respond(HttpStatusCode.Created, medication)
                } else {
                    medicationRepository.update(medication)
                    call.respond(HttpStatusCode.OK, medication)
                }
            }
        }

        // Delete a medication by ID
        delete("/{id}") {
            withUserId { userId ->
                val id = call.parameters["id"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing medication id"))
                    return@withUserId
                }

                // Verify ownership
                val medication = medicationRepository.getById(id)
                if (medication == null || medication.userId != userId) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Medication not found"))
                    return@withUserId
                }

                medicationRepository.delete(id)
                call.respond(HttpStatusCode.OK, mapOf("message" to "Medication deleted"))
            }
        }

        get("/agenda") {
            withUserId { userId ->
                val dateString = call.request.queryParameters["date"] ?: ""
                if (dateString.isEmpty()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "date parameter is required in format YYYY-MM-DD"),
                    )
                    return@withUserId
                }

                val agenda = medicationLogRepository.getDailyAgenda(userId, dateString)
                call.respond(HttpStatusCode.OK, agenda)
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

                val status = try {
                    MedicationLogStatus.valueOf(request.status)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid status"))
                    return@withUserId
                }

                val timestamp = request.timestampMs ?: System.currentTimeMillis()
                val medication = medicationRepository.getById(request.medicationId)
                if (medication == null || medication.userId != userId) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Medication not found"))
                    return@withUserId
                }

                val log = MedicationLog(
                    id = UUID.randomUUID().toString(),
                    medicationId = request.medicationId,
                    medicationTime = timestamp,
                    status = status,
                    medicationName = null,
                )

                medicationLogRepository.insert(log)
                call.respond(HttpStatusCode.Created, log)
            }
        }
    }
}

private data class ValidMedicationRequest(
    val frequency: MedicationFrequency,
    val status: MedicationStatus,
)

private suspend fun io.ktor.server.application.ApplicationCall.validateMedicationRequest(
    request: MedicationCreateRequest,
): ValidMedicationRequest? {
    if (request.name.isBlank()) {
        respond(HttpStatusCode.BadRequest, mapOf("error" to "Medication name is required"))
        return null
    }
    if (request.dosage.isBlank()) {
        respond(HttpStatusCode.BadRequest, mapOf("error" to "Dosage is required"))
        return null
    }
    if (request.startDate.isBlank()) {
        respond(HttpStatusCode.BadRequest, mapOf("error" to "Start date is required"))
        return null
    }
    if (request.endDate.isBlank()) {
        respond(HttpStatusCode.BadRequest, mapOf("error" to "End date is required"))
        return null
    }

    val dateRegex = Regex("^\\d{4}-\\d{2}-\\d{2}$")
    if (!dateRegex.matches(request.startDate)) {
        respond(HttpStatusCode.BadRequest, mapOf("error" to "Start date must be in YYYY-MM-DD format"))
        return null
    }
    if (!dateRegex.matches(request.endDate)) {
        respond(HttpStatusCode.BadRequest, mapOf("error" to "End date must be in YYYY-MM-DD format"))
        return null
    }

    val frequency = try {
        MedicationFrequency.valueOf(request.frequency)
    } catch (_: Exception) {
        respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid frequency: ${request.frequency}"))
        return null
    }

    val status = try {
        MedicationStatus.valueOf(request.status)
    } catch (_: Exception) {
        respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid status: ${request.status}"))
        return null
    }

    return ValidMedicationRequest(
        frequency = frequency,
        status = status,
    )
}
