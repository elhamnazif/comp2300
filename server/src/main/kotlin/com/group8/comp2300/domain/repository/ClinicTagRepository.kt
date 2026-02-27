package com.group8.comp2300.domain.repository

interface ClinicTagRepository {

    /**
     * Associates a specific tag name with a clinic.
     */
    fun addTag(clinicId: String, tagName: String)

    /**
     * Retrieves all tag names associated with a specific clinic.
     * Returns a simple list of strings for UI chips or labels.
     */
    fun getTagsByClinicId(clinicId: String): List<String>

    /**
     * Removes a single specific tag from a clinic.
     */
    fun removeTag(clinicId: String, tagName: String)

    fun removeAllTagsForClinic(clinicId: String)
}
