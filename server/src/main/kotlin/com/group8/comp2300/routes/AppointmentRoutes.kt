package com.group8.comp2300.routes

import com.group8.comp2300.domain.model.medical.Appointment
import com.group8.comp2300.domain.model.medical.AppointmentRequest
import com.group8.comp2300.domain.repository.AppointmentRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import java.util.*

fun Route.appointmentRoutes() {
    val appointmentRepository: AppointmentRepository by inject()

    route("/api/appointments") {
        get {
            withUserId { userId ->
                // Using getConfirmedByUserId - might want to get all instead, but confirmed is fine for now
                val appointments = appointmentRepository.getConfirmedByUserId(userId)
                call.respond(HttpStatusCode.OK, appointments)
            }
        }

        post {
            withUserId { userId ->
                val request = call.receive<AppointmentRequest>()

                val newAppointment = Appointment(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    title = request.title,
                    appointmentTime = request.appointmentTime,
                    appointmentType = request.appointmentType,
                    clinicId = null,
                    bookingId = null,
                    status = "CONFIRMED",
                    notes = request.notes,
                    hasReminder = true, // default or based on request
                    paymentStatus = "PENDING",
                )

                appointmentRepository.insertAppointment(newAppointment)
                call.respond(HttpStatusCode.Created, newAppointment)
            }
        }
    }
}
