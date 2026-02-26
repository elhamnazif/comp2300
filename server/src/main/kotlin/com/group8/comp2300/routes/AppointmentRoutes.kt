package com.group8.comp2300.routes

import com.group8.comp2300.domain.repository.AppointmentRepository
import io.ktor.server.routing.Route
import org.koin.ktor.ext.inject

fun Route.appointmentRoutes() {
    val appointmentRepository: AppointmentRepository by inject()
    // TODO: Add appointment routes
}
