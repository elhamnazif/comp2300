package com.group8.comp2300.domain.repository

import com.group8.comp2300.domain.model.medical.LabResult

interface LabResultsRepository {
    fun getRecentLabResults(): List<LabResult>
}
