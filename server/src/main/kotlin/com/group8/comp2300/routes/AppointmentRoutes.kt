package com.group8.comp2300.routes

import com.group8.comp2300.domain.model.medical.ClinicBookingRequest
import com.group8.comp2300.service.appointment.AppointmentResult
import com.group8.comp2300.service.appointment.AppointmentService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.appointmentRoutes() {
    val appointmentService: AppointmentService by inject()

    route("/api/appointments") {
        get {
            withUserId { userId ->
                val appointments = appointmentService.getAppointmentsForUser(userId)
                call.respond(HttpStatusCode.OK, appointments)
            }
        }

        post {
            withUserId { userId ->
                val request = call.receive<ClinicBookingRequest>()
                when (val result = appointmentService.bookAppointment(userId, request)) {
                    is AppointmentResult.Success -> call.respond(HttpStatusCode.Created, result.appointment)
                    is AppointmentResult.Error -> call.respond(result.status, mapOf("error" to result.message))
                }
            }
        }

        post("/{appointmentId}/cancel") {
            withUserId { userId ->
                val appointmentId = call.parameters["appointmentId"]
                if (appointmentId.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Appointment id is required"))
                    return@withUserId
                }

                when (val result = appointmentService.cancelAppointment(userId, appointmentId)) {
                    is AppointmentResult.Success -> call.respond(HttpStatusCode.OK, result.appointment)
                    is AppointmentResult.Error -> call.respond(result.status, mapOf("error" to result.message))
                }
            }
        }

        post("/{appointmentId}/reschedule") {
            withUserId { userId ->
                val appointmentId = call.parameters["appointmentId"]
                if (appointmentId.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Appointment id is required"))
                    return@withUserId
                }

                val request = call.receive<ClinicBookingRequest>()
                when (val result = appointmentService.rescheduleAppointment(userId, appointmentId, request)) {
                    is AppointmentResult.Success -> call.respond(HttpStatusCode.OK, result.appointment)
                    is AppointmentResult.Error -> call.respond(result.status, mapOf("error" to result.message))
                }
            }
        }
    }
}
