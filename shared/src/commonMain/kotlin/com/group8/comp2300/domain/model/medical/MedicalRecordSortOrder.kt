package com.group8.comp2300.domain.model.medical

import kotlinx.serialization.Serializable

@Serializable
enum class MedicalRecordSortOrder {
    DATE_DESC, // Newest to Oldest (Default)
    DATE_ASC, // Oldest to Newest
    NAME_ASC, // A to Z
    NAME_DESC, // Z to A
}

enum class RecordSortOrder(val apiValue: String) {
    RECENT("DATE_DESC"),
    OLDEST("DATE_ASC"),
    NAME_AZ("NAME_ASC"),
    NAME_ZA("NAME_DESC"),
}
