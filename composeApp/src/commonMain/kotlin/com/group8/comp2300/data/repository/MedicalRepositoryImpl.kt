package com.group8.comp2300.data.repository

import com.group8.comp2300.domain.model.medical.LabResult
import com.group8.comp2300.domain.repository.MedicalRepository
import com.group8.comp2300.mock.sampleResults

class MedicalRepositoryImpl : MedicalRepository {
    override fun getRecentLabResults(): List<LabResult> {
        return sampleResults
    }
}
