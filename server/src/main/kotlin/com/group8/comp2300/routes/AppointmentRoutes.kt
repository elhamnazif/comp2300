package com.group8.comp2300.routes

import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.domain.model.medical.Appointment
import com.group8.comp2300.domain.model.medical.AppointmentStatus
import com.group8.comp2300.domain.model.medical.ClinicBookingRequest
import com.group8.comp2300.domain.repository.AppointmentRepository
import com.group8.comp2300.domain.repository.AppointmentSlotRepository
import com.group8.comp2300.domain.repository.ClinicRepository
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
                val appointments = appointmentRepository.getByUserId(userId)
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

        post("/{appointmentId}/cancel") {
            withUserId { userId ->
                val appointmentId = call.parameters["appointmentId"]
                if (appointmentId.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Appointment id is required"))
                    return@withUserId
                }

                val appointment = appointmentRepository.getAppointmentById(appointmentId)
                if (appointment == null || appointment.userId != userId) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Appointment not found"))
                    return@withUserId
                }

                if (appointment.status != AppointmentStatus.CONFIRMED.name ||
                    appointment.appointmentTime <= System.currentTimeMillis()
                ) {
                    call.respond(HttpStatusCode.Conflict, mapOf("error" to "Appointment can no longer be changed"))
                    return@withUserId
                }

                val updatedAppointment = appointment.copy(
                    status = AppointmentStatus.CANCELLED.name,
                    bookingId = null,
                )

                database.appointmentQueries.transaction {
                    appointment.bookingId?.let { appointmentSlotRepository.updateSlotBookingStatus(it, false) }
                    appointmentRepository.updateAppointment(updatedAppointment)
                }

                call.respond(HttpStatusCode.OK, updatedAppointment)
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
                val appointment = appointmentRepository.getAppointmentById(appointmentId)
                if (appointment == null || appointment.userId != userId) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Appointment not found"))
                    return@withUserId
                }

                if (appointment.status != AppointmentStatus.CONFIRMED.name ||
                    appointment.appointmentTime <= System.currentTimeMillis()
                ) {
                    call.respond(HttpStatusCode.Conflict, mapOf("error" to "Appointment can no longer be changed"))
                    return@withUserId
                }

                if (appointment.clinicId == null || appointment.clinicId != request.clinicId) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Reschedule must stay with the same clinic"),
                    )
                    return@withUserId
                }

                val clinic = clinicRepository.getById(request.clinicId)
                if (clinic == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Clinic not found"))
                    return@withUserId
                }

                val newSlot = appointmentSlotRepository.getSlotById(request.slotId)
                if (newSlot == null || newSlot.clinicId != clinic.id) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Slot not found"))
                    return@withUserId
                }

                if (newSlot.startTime <= System.currentTimeMillis()) {
                    call.respond(HttpStatusCode.Conflict, mapOf("error" to "Slot is no longer available"))
                    return@withUserId
                }

                if (appointment.bookingId == newSlot.id) {
                    call.respond(HttpStatusCode.Conflict, mapOf("error" to "Choose a different slot"))
                    return@withUserId
                }

                if (newSlot.isBooked) {
                    call.respond(HttpStatusCode.Conflict, mapOf("error" to "Slot is no longer available"))
                    return@withUserId
                }

                val updatedAppointment = appointment.copy(
                    title = "Appointment at ${clinic.name}",
                    appointmentTime = newSlot.startTime,
                    appointmentType = request.appointmentType,
                    bookingId = newSlot.id,
                    notes = request.reason?.trim()?.ifBlank { null },
                    hasReminder = request.hasReminder,
                    status = AppointmentStatus.CONFIRMED.name,
                )

                database.appointmentQueries.transaction {
                    val latestSlot = appointmentSlotRepository.getSlotById(newSlot.id)
                    if (latestSlot == null || latestSlot.isBooked) {
                        rollback()
                        return@transaction
                    }
                    appointment.bookingId?.let { appointmentSlotRepository.updateSlotBookingStatus(it, false) }
                    appointmentSlotRepository.updateSlotBookingStatus(newSlot.id, true)
                    appointmentRepository.updateAppointment(updatedAppointment)
                }

                val persistedAppointment = appointmentRepository.getAppointmentById(appointmentId)
                if (persistedAppointment == null) {
                    call.respond(HttpStatusCode.Conflict, mapOf("error" to "Slot is no longer available"))
                    return@withUserId
                }

                call.respond(HttpStatusCode.OK, persistedAppointment)
            }
        }
    }
}
