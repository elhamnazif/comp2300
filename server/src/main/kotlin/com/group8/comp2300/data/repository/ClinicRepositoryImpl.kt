package com.group8.comp2300.data.repository

import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.database.data.ClinicEntity
import com.group8.comp2300.domain.model.medical.Clinic
import com.group8.comp2300.domain.repository.ClinicRepository
import com.group8.comp2300.domain.repository.ClinicTagRepository

class ClinicRepositoryImpl(private val database: ServerDatabase, private val clinicTagRepository: ClinicTagRepository) :
    ClinicRepository {

    override fun getById(id: String): Clinic? = database.clinicQueries.selectClinicById(id)
        .executeAsOneOrNull()?.toDomain(clinicTagRepository.getTagsByClinicId(id))

    override fun insert(clinic: Clinic) {
        database.clinicQueries.insertClinic(
            id = clinic.id,
            name = clinic.name,
            address = clinic.address ?: "",
            phone = clinic.phone,
            latitude = clinic.lat,
            longitude = clinic.lng,
        )
        clinic.tags.forEach { tag ->
            clinicTagRepository.addTag(clinic.id, tag)
        }
    }

    override fun update(clinic: Clinic) {
        database.clinicQueries.updateClinicById(
            name = clinic.name,
            address = clinic.address ?: "",
            phone = clinic.phone,
            latitude = clinic.lat,
            longitude = clinic.lng,
            id = clinic.id,
        )
        clinicTagRepository.removeAllTagsForClinic(clinic.id)
        clinic.tags.forEach { tag ->
            clinicTagRepository.addTag(clinic.id, tag)
        }
    }

    override fun delete(id: String) {
        clinicTagRepository.removeAllTagsForClinic(id)
        database.clinicQueries.deleteClinicById(id)
    }
}

private fun ClinicEntity.toDomain(tags: List<String>) = Clinic(
    id = id,
    name = name,
    distanceKm = 0.0,
    tags = tags,
    nextAvailableSlot = System.currentTimeMillis(),
    lat = latitude,
    lng = longitude,
    address = address,
    phone = phone,
)
