package com.group8.comp2300.data.repository

import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.database.data.MedicationScheduleEnt
import com.group8.comp2300.domain.model.medical.MedicationSchedule
import com.group8.comp2300.domain.repository.MedicationScheduleRepository

class MedicationScheduleRepositoryImpl(private val database: ServerDatabase) : MedicationScheduleRepository {

    private val queries = database.medicationScheduleQueries

    override fun getAllByMedicationId(medicationId: String): List<MedicationSchedule> =
        queries.selectAllSchedulesByMedId(medicationId)
            .executeAsList()
            .map { it.toDomain() }

    override fun insert(schedule: MedicationSchedule) {
        queries.insertMedSchedule(
            id = schedule.id,
            medication_id = schedule.medicationId,
            day_of_week = schedule.dayOfWeek.toLong(),
            time_of_day = schedule.timeOfDay
        )
    }

    override fun update(schedule: MedicationSchedule) {
        queries.updateMedScheduleById(
            day_of_week = schedule.dayOfWeek.toLong(),
            time_of_day = schedule.timeOfDay,
            id = schedule.id
        )
    }

    override fun delete(id: String) {
        queries.deleteMedScheduleById(id)
    }

    private fun MedicationScheduleEnt.toDomain() = MedicationSchedule(
        id = id,
        medicationId = medication_id,
        dayOfWeek = day_of_week?.toInt() ?: 0,
        timeOfDay = time_of_day
    )
}