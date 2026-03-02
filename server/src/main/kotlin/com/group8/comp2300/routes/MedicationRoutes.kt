package com.group8.comp2300.routes

import com.group8.comp2300.domain.model.medical.MedicationLog
import com.group8.comp2300.domain.model.medical.MedicationLogRequest
import com.group8.comp2300.domain.model.medical.MedicationLogStatus
import com.group8.comp2300.domain.repository.MedicationLogRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject
import java.util.UUID

fun Route.medicationRoutes() {
    val medicationLogRepository: MedicationLogRepository by inject()

    route("/api/medications") {
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

                // Check if status is a valid enum value
                val status = try {
                    MedicationLogStatus.valueOf(request.status)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid status"))
                    return@withUserId
                }

                // A proper implementation would link to user through medication,
                // but log repository takes MedicationLog object directly
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
