package com.group8.comp2300.data.repository.medical

import com.group8.comp2300.data.local.AppointmentLocalDataSource
import com.group8.comp2300.data.offline.MedicalOfflineMutations
import com.group8.comp2300.data.offline.QueuedOfflineStore
import com.group8.comp2300.data.offline.QueuedWriteDispatcher
import com.group8.comp2300.domain.model.medical.Appointment
import com.group8.comp2300.domain.model.medical.AppointmentRequest
import com.group8.comp2300.domain.model.session.userOrNull
import com.group8.comp2300.domain.repository.AuthRepository
import com.group8.comp2300.domain.repository.medical.AppointmentDataRepository

class AppointmentDataRepositoryImpl(
    private val authRepository: AuthRepository,
    private val appointmentLocal: AppointmentLocalDataSource,
    private val queuedWriteDispatcher: QueuedWriteDispatcher,
) : AppointmentDataRepository {
    private val appointmentWrites = QueuedOfflineStore(
        mutation = MedicalOfflineMutations.appointment,
        queuedWriteDispatcher = queuedWriteDispatcher,
        buildLocal = { localId, request ->
            Appointment(
                id = localId,
                userId = authRepository.session.value.userOrNull?.id.orEmpty(),
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
        },
        saveLocal = appointmentLocal::insert,
        readLocal = appointmentLocal::getById,
    )

    override suspend fun getAppointments(): List<Appointment> = appointmentLocal.getAll()

    override suspend fun scheduleAppointment(request: AppointmentRequest): Appointment =
        appointmentWrites.write(request)
}
