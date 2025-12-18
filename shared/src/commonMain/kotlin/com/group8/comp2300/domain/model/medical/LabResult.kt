package com.group8.comp2300.domain.model.medical

import kotlinx.serialization.Serializable

@Serializable
data class LabResult(
        val id: String,
        val testName: String,
        val testDate: Long, // Unix timestamp
        val status: LabStatus,
        val notes: String? = null
) {
        val isPositive: Boolean
                get() = status == LabStatus.POSITIVE
}