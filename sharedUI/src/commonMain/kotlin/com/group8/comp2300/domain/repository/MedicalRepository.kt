package com.group8.comp2300.domain.repository

import com.group8.comp2300.domain.model.medical.LabResult

interface MedicalRepository {
    fun getRecentLabResults(): List<LabResult>
}
