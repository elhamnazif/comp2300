package com.group8.comp2300.routes

import com.group8.comp2300.domain.model.medical.MedicationCreateRequest
import com.group8.comp2300.domain.model.medical.MedicationLogRequest
import com.group8.comp2300.domain.model.medical.RoutineCreateRequest
import com.group8.comp2300.domain.model.medical.RoutineOccurrenceOverrideRequest
import com.group8.comp2300.service.medication.MedicationResult
import com.group8.comp2300.service.medication.MedicationService
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

fun Route.medicationRoutes() {
    val medicationService: MedicationService by inject()

    route("/api/medications") {
        get {
            withUserId { userId ->
                call.respond(HttpStatusCode.OK, medicationService.getAllMedications(userId))
            }
        }

        put("/{id}") {
            withUserId { userId ->
                val id = call.parameters["id"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing medication id"))
                    return@withUserId
                }
                val request = call.receive<MedicationCreateRequest>()
                when (val result = medicationService.putMedication(userId, id, request)) {
                    is MedicationResult.Success -> call.respond(HttpStatusCode.OK, result.data)
                    is MedicationResult.Error -> call.respond(result.status, mapOf("error" to result.message))
                }
            }
        }

        delete("/{id}") {
            withUserId { userId ->
                val id = call.parameters["id"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing medication id"))
                    return@withUserId
                }
                when (val result = medicationService.deleteMedication(userId, id)) {
                    is MedicationResult.Success -> call.respond(
                        HttpStatusCode.OK,
                        mapOf("message" to "Medication deleted")
                    )
                    is MedicationResult.Error -> call.respond(result.status, mapOf("error" to result.message))
                }
            }
        }

        get("/logs") {
            withUserId { userId ->
                call.respond(HttpStatusCode.OK, medicationService.getMedicationLogs(userId))
            }
        }

        post("/logs") {
            withUserId { userId ->
                val request = call.receive<MedicationLogRequest>()
                when (val result = medicationService.logMedication(userId, request)) {
                    is MedicationResult.Success -> call.respond(HttpStatusCode.Created, result.data)
                    is MedicationResult.Error -> call.respond(result.status, mapOf("error" to result.message))
                }
            }
        }
    }

    route("/api/routines") {
        get {
            withUserId { userId ->
                call.respond(HttpStatusCode.OK, medicationService.getAllRoutines(userId))
            }
        }

        get("/occurrence-overrides") {
            withUserId { userId ->
                call.respond(HttpStatusCode.OK, medicationService.getRoutineOverrides(userId))
            }
        }

        put("/occurrence-overrides") {
            withUserId { userId ->
                val request = call.receive<RoutineOccurrenceOverrideRequest>()
                when (val result = medicationService.putRoutineOverride(userId, request)) {
                    is MedicationResult.Success -> call.respond(HttpStatusCode.OK, result.data)
                    is MedicationResult.Error -> call.respond(result.status, mapOf("error" to result.message))
                }
            }
        }

        put("/{id}") {
            withUserId { userId ->
                val id = call.parameters["id"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing routine id"))
                    return@withUserId
                }
                val request = call.receive<RoutineCreateRequest>()
                when (val result = medicationService.putRoutine(userId, id, request)) {
                    is MedicationResult.Success -> call.respond(HttpStatusCode.OK, result.data)
                    is MedicationResult.Error -> call.respond(result.status, mapOf("error" to result.message))
                }
            }
        }

        delete("/{id}") {
            withUserId { userId ->
                val id = call.parameters["id"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing routine id"))
                    return@withUserId
                }
                when (val result = medicationService.deleteRoutine(userId, id)) {
                    is MedicationResult.Success -> call.respond(
                        HttpStatusCode.OK,
                        mapOf("message" to "Routine deleted")
                    )
                    is MedicationResult.Error -> call.respond(result.status, mapOf("error" to result.message))
                }
            }
        }

        get("/agenda") {
            withUserId { userId ->
                val dateString = call.request.queryParameters["date"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "date parameter is required"))
                    return@withUserId
                }
                when (val result = medicationService.getAgenda(userId, dateString)) {
                    is MedicationResult.Success -> call.respond(HttpStatusCode.OK, result.data)
                    is MedicationResult.Error -> call.respond(result.status, mapOf("error" to result.message))
                }
            }
        }
    }
}
