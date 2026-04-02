package com.group8.comp2300.data.repository

import com.group8.comp2300.domain.model.medical.LabResult
import com.group8.comp2300.domain.repository.LabResultsRepository
import com.group8.comp2300.mock.sampleResults

class LabResultsRepositoryImpl : LabResultsRepository {
    override fun getRecentLabResults(): List<LabResult> = sampleResults
}
