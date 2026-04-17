package com.group8.comp2300.routes

import com.group8.comp2300.domain.model.medical.Appointment
import com.group8.comp2300.domain.model.medical.ClinicBookingRequest
import com.group8.comp2300.domain.repository.AppointmentSlotRepository
import com.group8.comp2300.domain.repository.AppointmentRepository
import com.group8.comp2300.domain.repository.ClinicRepository
import com.group8.comp2300.database.ServerDatabase
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import java.util.*

fun Route.appointmentRoutes() {
    val appointmentRepository: AppointmentRepository by inject()
    val appointmentSlotRepository: AppointmentSlotRepository by inject()
    val clinicRepository: ClinicRepository by inject()
    val database: ServerDatabase by inject()

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
                val request = call.receive<ClinicBookingRequest>()
                val clinic = clinicRepository.getById(request.clinicId)
                if (clinic == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Clinic not found"))
                    return@withUserId
                }

                val slot = appointmentSlotRepository.getSlotById(request.slotId)
                if (slot == null || slot.clinicId != clinic.id) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Slot not found"))
                    return@withUserId
                }

                if (slot.isBooked || slot.startTime <= System.currentTimeMillis()) {
                    call.respond(HttpStatusCode.Conflict, mapOf("error" to "Slot is no longer available"))
                    return@withUserId
                }

                val newAppointment = Appointment(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    title = "Appointment at ${clinic.name}",
                    appointmentTime = slot.startTime,
                    appointmentType = request.appointmentType,
                    clinicId = clinic.id,
                    bookingId = slot.id,
                    status = "CONFIRMED",
                    notes = request.reason?.trim()?.ifBlank { null },
                    hasReminder = request.hasReminder,
                    paymentStatus = "PENDING",
                )

                database.appointmentQueries.transaction {
                    val latestSlot = appointmentSlotRepository.getSlotById(slot.id)
                    if (latestSlot == null || latestSlot.isBooked) {
                        rollback()
                        return@transaction
                    }
                    appointmentSlotRepository.updateSlotBookingStatus(slot.id, true)
                    appointmentRepository.insertAppointment(newAppointment)
                }

                val persistedAppointment = appointmentRepository.getAppointmentById(newAppointment.id)
                if (persistedAppointment == null) {
                    call.respond(HttpStatusCode.Conflict, mapOf("error" to "Slot is no longer available"))
                    return@withUserId
                }

                call.respond(HttpStatusCode.Created, persistedAppointment)
            }
        }
    }
}
