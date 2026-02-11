package com.group8.comp2300.domain.usecase.medical

import com.group8.comp2300.domain.model.medical.LabResult
import com.group8.comp2300.domain.repository.MedicalRepository

class GetRecentLabResultsUseCase(private val repository: MedicalRepository) {
    operator fun invoke(): List<LabResult> = repository.getRecentLabResults()
}
