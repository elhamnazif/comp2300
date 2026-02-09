package com.group8.comp2300.mock

data class MockCalendarAppointment(
    val id: String,
    val title: String,
    val type: String,
    val date: String,
    val time: String
)

data class MockDoctor(val name: String)

val sampleCalendarAppointments =
    listOf(
        MockCalendarAppointment("1", "Dr. Smith", "Consultation", "Nov 12", "10:00 AM"),
        MockCalendarAppointment("2", "Lab Corp", "Lab Work", "Nov 15", "08:30 AM")
    )

val sampleCalendarDoctors =
    listOf(MockDoctor("Dr. Smith"), MockDoctor("Dr. Jones"), MockDoctor("Sexual Health Clinic"))
