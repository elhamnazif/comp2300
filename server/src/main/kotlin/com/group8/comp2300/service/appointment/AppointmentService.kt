package com.group8.comp2300.service.appointment

import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.domain.model.medical.Appointment
import com.group8.comp2300.domain.model.medical.AppointmentStatus
import com.group8.comp2300.domain.model.medical.ClinicBookingRequest
import com.group8.comp2300.domain.repository.AppointmentRepository
import com.group8.comp2300.domain.repository.AppointmentSlotRepository
import com.group8.comp2300.domain.repository.ClinicRepository
import io.ktor.http.HttpStatusCode
import java.util.UUID

sealed class AppointmentResult {
    data class Success(val appointment: Appointment) : AppointmentResult()
    data class Error(val status: HttpStatusCode, val message: String) : AppointmentResult()
}

class AppointmentService(
    private val appointmentRepository: AppointmentRepository,
    private val appointmentSlotRepository: AppointmentSlotRepository,
    private val clinicRepository: ClinicRepository,
    private val database: ServerDatabase
) {
    fun getAppointmentsForUser(userId: String): List<Appointment> {
        return appointmentRepository.getByUserId(userId)
    }

    fun bookAppointment(userId: String, request: ClinicBookingRequest): AppointmentResult {
        val clinic = clinicRepository.getById(request.clinicId)
            ?: return AppointmentResult.Error(HttpStatusCode.NotFound, "Clinic not found")

        val slot = appointmentSlotRepository.getSlotById(request.slotId)
        if (slot == null || slot.clinicId != clinic.id) {
            return AppointmentResult.Error(HttpStatusCode.NotFound, "Slot not found")
        }

        if (slot.isBooked || slot.startTime <= System.currentTimeMillis()) {
            return AppointmentResult.Error(HttpStatusCode.Conflict, "Slot is no longer available")
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
            ?: return AppointmentResult.Error(HttpStatusCode.Conflict, "Slot is no longer available")

        return AppointmentResult.Success(persistedAppointment)
    }

    fun cancelAppointment(userId: String, appointmentId: String): AppointmentResult {
        val appointment = appointmentRepository.getAppointmentById(appointmentId)
        if (appointment == null || appointment.userId != userId) {
            return AppointmentResult.Error(HttpStatusCode.NotFound, "Appointment not found")
        }

        if (appointment.status != AppointmentStatus.CONFIRMED.name ||
            appointment.appointmentTime <= System.currentTimeMillis()
        ) {
            return AppointmentResult.Error(HttpStatusCode.Conflict, "Appointment can no longer be changed")
        }

        val updatedAppointment = appointment.copy(
            status = AppointmentStatus.CANCELLED.name,
            bookingId = null,
        )

        database.appointmentQueries.transaction {
            appointment.bookingId?.let { appointmentSlotRepository.updateSlotBookingStatus(it, false) }
            appointmentRepository.updateAppointment(updatedAppointment)
        }

        return AppointmentResult.Success(updatedAppointment)
    }

    fun rescheduleAppointment(userId: String, appointmentId: String, request: ClinicBookingRequest): AppointmentResult {
        val appointment = appointmentRepository.getAppointmentById(appointmentId)
        if (appointment == null || appointment.userId != userId) {
            return AppointmentResult.Error(HttpStatusCode.NotFound, "Appointment not found")
        }

        if (appointment.status != AppointmentStatus.CONFIRMED.name ||
            appointment.appointmentTime <= System.currentTimeMillis()
        ) {
            return AppointmentResult.Error(HttpStatusCode.Conflict, "Appointment can no longer be changed")
        }

        if (appointment.clinicId == null || appointment.clinicId != request.clinicId) {
            return AppointmentResult.Error(HttpStatusCode.BadRequest, "Reschedule must stay with the same clinic")
        }

        val clinic = clinicRepository.getById(request.clinicId)
            ?: return AppointmentResult.Error(HttpStatusCode.NotFound, "Clinic not found")

        val newSlot = appointmentSlotRepository.getSlotById(request.slotId)
        if (newSlot == null || newSlot.clinicId != clinic.id) {
            return AppointmentResult.Error(HttpStatusCode.NotFound, "Slot not found")
        }

        if (newSlot.startTime <= System.currentTimeMillis()) {
            return AppointmentResult.Error(HttpStatusCode.Conflict, "Slot is no longer available")
        }

        if (appointment.bookingId == newSlot.id) {
            return AppointmentResult.Error(HttpStatusCode.Conflict, "Choose a different slot")
        }

        if (newSlot.isBooked) {
            return AppointmentResult.Error(HttpStatusCode.Conflict, "Slot is no longer available")
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
            ?: return AppointmentResult.Error(HttpStatusCode.Conflict, "Slot is no longer available")

        return AppointmentResult.Success(persistedAppointment)
    }
}
