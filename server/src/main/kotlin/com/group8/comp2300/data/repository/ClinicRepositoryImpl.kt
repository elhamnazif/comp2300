package com.group8.comp2300.data.repository

import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.database.data.ClinicEntity
import com.group8.comp2300.domain.model.medical.Clinic
import com.group8.comp2300.domain.repository.AppointmentSlotRepository
import com.group8.comp2300.domain.repository.ClinicRepository
import com.group8.comp2300.domain.repository.ClinicTagRepository
import com.group8.comp2300.mock.sampleClinics

class ClinicRepositoryImpl(
    private val database: ServerDatabase,
    private val clinicTagRepository: ClinicTagRepository,
    private val appointmentSlotRepository: AppointmentSlotRepository,
) : ClinicRepository {

    override fun getAll(): List<Clinic> = database.clinicQueries.selectAllClinics()
        .executeAsList()
        .let { clinics ->
            val nextAvailableSlotFallback = System.currentTimeMillis()
            val tagsByClinicId = clinicTagRepository.getAllTagsByClinicId()
            val nextAvailableSlotByClinicId = appointmentSlotRepository.getNextAvailableStartTimesByClinicId()
            clinics.map { entity ->
                entity.toDomain(
                    tags = tagsByClinicId[entity.id].orEmpty(),
                    nextAvailableSlot = nextAvailableSlotByClinicId[entity.id] ?: nextAvailableSlotFallback,
                )
            }
        }

    override fun getById(id: String): Clinic? = database.clinicQueries.selectClinicById(id)
        .executeAsOneOrNull()?.toDomain(
            tags = clinicTagRepository.getTagsByClinicId(id),
            nextAvailableSlot = appointmentSlotRepository.getAvailableByClinic(id).firstOrNull()?.startTime
                ?: System.currentTimeMillis(),
        )

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

private val sampleClinicsById = sampleClinics.associateBy { it.id }

private fun ClinicEntity.toDomain(tags: List<String>, nextAvailableSlot: Long): Clinic {
    val fixture = sampleClinicsById[id]
    return Clinic(
        id = id,
        name = name,
        distanceKm = fixture?.distanceKm ?: 0.0,
        tags = if (tags.isNotEmpty()) tags else fixture?.tags.orEmpty(),
        nextAvailableSlot = nextAvailableSlot,
        lat = latitude,
        lng = longitude,
        address = address,
        phone = phone,
        imageUrl = fixture?.imageUrl,
        pricingTier = fixture?.pricingTier,
        serviceTypes = fixture?.serviceTypes.orEmpty(),
        inclusivityFlags = fixture?.inclusivityFlags ?: com.group8.comp2300.domain.model.medical.InclusivityFlags(),
    )
}
