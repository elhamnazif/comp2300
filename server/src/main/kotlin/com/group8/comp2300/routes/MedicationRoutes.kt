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
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject
import java.util.UUID

fun Route.medicationRoutes() {
    val medicationLogRepository: MedicationLogRepository by inject()
    val medicationRepository: MedicationRepository by inject()

    route("/api/medications") {
        // List user's active medications
        get {
            withUserId { userId ->
                val medications = medicationRepository.getActiveByUserId(userId)
                call.respond(HttpStatusCode.OK, medications)
            }
        }

        // Create a new medication for the user
        post {
            withUserId { userId ->
                val request = call.receive<MedicationCreateRequest>()

                // Validate required fields
                if (request.name.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Medication name is required"))
                    return@withUserId
                }
                if (request.dosage.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Dosage is required"))
                    return@withUserId
                }
                if (request.startDate.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Start date is required"))
                    return@withUserId
                }
                if (request.endDate.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "End date is required"))
                    return@withUserId
                }

                // Validate date format (YYYY-MM-DD)
                val dateRegex = Regex("^\\d{4}-\\d{2}-\\d{2}$")
                if (!dateRegex.matches(request.startDate)) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Start date must be in YYYY-MM-DD format"))
                    return@withUserId
                }
                if (!dateRegex.matches(request.endDate)) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "End date must be in YYYY-MM-DD format"))
                    return@withUserId
                }

                val frequency = try {
                    MedicationFrequency.valueOf(request.frequency)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid frequency: ${request.frequency}"))
                    return@withUserId
                }

                val medication = Medication(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    name = request.name,
                    dosage = request.dosage,
                    quantity = request.quantity,
                    frequency = frequency,
                    instruction = request.instruction,
                    colorHex = request.colorHex ?: Medication.PRESET_COLORS.random(),
                    startDate = request.startDate,
                    endDate = request.endDate,
                    hasReminder = request.hasReminder,
                    status = MedicationStatus.ACTIVE,
                )

                medicationRepository.insert(medication)
                call.respond(HttpStatusCode.Created, medication)
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
