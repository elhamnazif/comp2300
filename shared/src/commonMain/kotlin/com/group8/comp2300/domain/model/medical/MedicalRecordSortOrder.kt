package com.group8.comp2300.domain.model.medical

import kotlinx.serialization.Serializable

@Serializable
enum class MedicalRecordSortOrder {
    DATE_DESC, // Newest to Oldest (Default)
    DATE_ASC, // Oldest to Newest
    NAME_ASC, // A to Z
    NAME_DESC, // Z to A
}
