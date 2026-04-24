package com.group8.comp2300.routes

import com.group8.comp2300.domain.repository.AppointmentSlotRepository
import com.group8.comp2300.domain.repository.ClinicRepository
import com.group8.comp2300.service.appointment.MockClinicOperationsService
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.clinicRoutes() {
    val clinicRepository: ClinicRepository by inject()
    val appointmentSlotRepository: AppointmentSlotRepository by inject()
    val mockClinicOperationsService: MockClinicOperationsService by inject()

    route("/api/clinics") {
        get {
            mockClinicOperationsService.prepareCatalog()
            call.respond(
                HttpStatusCode.OK,
                clinicRepository.getAll().sortedBy { it.distanceKm },
            )
        }

        get("/{clinicId}") {
            val clinicId = call.parameters["clinicId"]
            if (clinicId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Clinic id is required"))
                return@get
            }

            val clinic = clinicRepository.getById(clinicId)
            if (clinic == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Clinic not found"))
                return@get
            }

            call.respond(HttpStatusCode.OK, clinic)
        }

        get("/{clinicId}/slots") {
            val clinicId = call.parameters["clinicId"]
            if (clinicId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Clinic id is required"))
                return@get
            }

            mockClinicOperationsService.prepareClinic(clinicId)
            call.respond(
                HttpStatusCode.OK,
                appointmentSlotRepository.getAvailableByClinic(clinicId).filter {
                    it.startTime >
                        System.currentTimeMillis()
                },
            )
        }
    }
}
