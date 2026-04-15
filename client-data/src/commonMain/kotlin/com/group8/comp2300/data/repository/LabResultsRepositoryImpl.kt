package com.group8.comp2300.data.repository

import com.group8.comp2300.domain.model.medical.LabResult
import com.group8.comp2300.domain.repository.LabResultsRepository
import com.group8.comp2300.mock.sampleResults

/**
 * Temporary fixture-backed repository until lab results are fetched from a real data source.
 */
class FixtureLabResultsRepository : LabResultsRepository {
    override fun getRecentLabResults(): List<LabResult> = sampleResults
}
