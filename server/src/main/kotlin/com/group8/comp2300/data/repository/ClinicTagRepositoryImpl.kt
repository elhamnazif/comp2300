package com.group8.comp2300.data.repository

import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.domain.repository.ClinicTagRepository

class ClinicTagRepositoryImpl(private val database: ServerDatabase) : ClinicTagRepository {

    override fun addTag(clinicId: String, tagName: String) {
        database.clinicTagsQueries.addClinicTag(
            clinic_id = clinicId,
            tag_name = tagName,
        )
    }

    override fun getTagsByClinicId(clinicId: String): List<String> =
        database.clinicTagsQueries.selectAllTagsByClinicId(clinicId)
            .executeAsList()

    override fun removeTag(clinicId: String, tagName: String) {
        database.clinicTagsQueries.deleteClinicTag(
            clinic_id = clinicId,
            tag_name = tagName,
        )
    }

    override fun removeAllTagsForClinic(clinicId: String) {
        database.clinicTagsQueries.deleteAllTagsByClinicId(clinicId)
    }
}