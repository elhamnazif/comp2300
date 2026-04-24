package com.group8.comp2300.data.repository.medical

import com.group8.comp2300.data.local.AppointmentLocalDataSource
import com.group8.comp2300.data.notifications.AppointmentNotificationScheduler
import com.group8.comp2300.data.remote.ApiService
import com.group8.comp2300.domain.model.medical.Appointment
import com.group8.comp2300.domain.model.medical.ClinicBookingRequest
import com.group8.comp2300.domain.model.medical.resolvedStatus
import com.group8.comp2300.domain.repository.medical.AppointmentDataRepository

class AppointmentDataRepositoryImpl(
    private val appointmentLocal: AppointmentLocalDataSource,
    private val apiService: ApiService,
    private val appointmentNotificationScheduler: AppointmentNotificationScheduler,
) : AppointmentDataRepository {
    override suspend fun getAppointments(): List<Appointment> =
        appointmentLocal.getAll().filter { it.resolvedStatus().isScheduled }

    override suspend fun getBookingHistory(): List<Appointment> = appointmentLocal.getAll()

    override suspend fun getAppointment(id: String): Appointment? = appointmentLocal.getById(id)

    override suspend fun bookClinicAppointment(request: ClinicBookingRequest): Appointment =
        apiService.bookClinicAppointment(request).also { appointment ->
            appointmentLocal.insert(appointment)
            appointmentNotificationScheduler.syncAppointment(appointment)
        }

    override suspend fun cancelAppointment(id: String): Appointment =
        apiService.cancelAppointment(id).also { appointment ->
            appointmentLocal.insert(appointment)
            appointmentNotificationScheduler.syncAppointment(appointment)
        }

    override suspend fun rescheduleAppointment(id: String, request: ClinicBookingRequest): Appointment =
        apiService.rescheduleAppointment(id, request).also { appointment ->
            appointmentLocal.insert(appointment)
            appointmentNotificationScheduler.syncAppointment(appointment)
        }
}
