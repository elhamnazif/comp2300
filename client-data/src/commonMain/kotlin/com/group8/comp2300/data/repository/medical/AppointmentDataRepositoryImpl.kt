package com.group8.comp2300.data.repository.medical

import com.group8.comp2300.data.local.AppointmentLocalDataSource
import com.group8.comp2300.data.offline.OutboxEntityType
import com.group8.comp2300.data.offline.QueuedWriteDispatcher
import com.group8.comp2300.domain.model.medical.Appointment
import com.group8.comp2300.domain.model.medical.AppointmentRequest
import com.group8.comp2300.domain.model.session.AuthSession
import com.group8.comp2300.domain.model.session.userOrNull
import com.group8.comp2300.domain.repository.AuthRepository
import com.group8.comp2300.domain.repository.medical.AppointmentDataRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.uuid.Uuid

class AppointmentDataRepositoryImpl(
    private val authRepository: AuthRepository,
    private val appointmentLocal: AppointmentLocalDataSource,
    private val queuedWriteDispatcher: QueuedWriteDispatcher,
) : AppointmentDataRepository {
    override suspend fun getAppointments(): List<Appointment> = appointmentLocal.getAll()

    override suspend fun scheduleAppointment(request: AppointmentRequest): Appointment {
        val user = (authRepository.session.value as? AuthSession.SignedIn)?.user
            ?: error("Sign in required to schedule appointments")

        val localId = Uuid.random().toString()
        val placeholder = Appointment(
            id = localId,
            userId = user.id,
            title = request.title,
            appointmentTime = request.appointmentTime,
            appointmentType = request.appointmentType,
            clinicId = null,
            bookingId = null,
            status = "PENDING_SYNC",
            notes = request.notes,
            hasReminder = true,
            paymentStatus = "PENDING",
        )

        appointmentLocal.insert(placeholder)
        queuedWriteDispatcher.replacePending(
            entityType = OutboxEntityType.APPOINTMENT,
            localId = localId,
            payload = Json.encodeToString(request),
        )
        return appointmentLocal.getAll().firstOrNull { it.id == localId } ?: placeholder
    }
}
