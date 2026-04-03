package com.group8.comp2300.domain.model.medical

import kotlinx.serialization.Serializable

@Serializable
enum class MedicalRecordSortOrder {
    DATE_DESC, // Newest to Oldest (Default)
    DATE_ASC, // Oldest to Newest
    NAME_ASC, // A to Z
    NAME_DESC, // Z to A
}

enum class RecordSortOrder(val apiValue: String, val displayName: String) {
    RECENT("DATE_DESC", "Recent"),
    OLDEST("DATE_ASC", "Oldest"),
    NAME_AZ("NAME_ASC", "Name A–Z"),
    NAME_ZA("NAME_DESC", "Name Z–A"),
}
