package com.group8.comp2300.data.repository

import com.group8.comp2300.database.data.ClinicEntity
import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.domain.model.medical.Clinic
import com.group8.comp2300.domain.repository.ClinicRepository

class ClinicRepositoryImpl(private val database: ServerDatabase) : ClinicRepository {

    override fun getById(id: String): Clinic? =
        database.clinicQueries.selectClinicById(id)
            .executeAsOneOrNull()?.toDomain()

    override fun insert(clinic: Clinic) {
        database.clinicQueries.insertClinic(
            id = clinic.id,
            name = clinic.name,
            address = clinic.address,
            phone = clinic.phone,
            latitude = clinic.lat,
            longitude = clinic.lng,
        )
    }

    override fun update(clinic: Clinic) {
        database.clinicQueries.updateClinicById(
            id = clinic.id,
            name = clinic.name,
            address = clinic.address,
            phone = clinic.phone,
            latitude = clinic.lat,
            longitude = clinic.lng,
        )
    }

    override fun delete(id: String) {
        database.clinicQueries.deleteClinicById(id)
    }

    private fun ClinicEntity.toDomain() = Clinic(
        id = id,
        name = name,
        address = address,
        phone = phone,
        lat = latitude ?: 0.0,
        lng = longitude ?: 0.0,
    )
}