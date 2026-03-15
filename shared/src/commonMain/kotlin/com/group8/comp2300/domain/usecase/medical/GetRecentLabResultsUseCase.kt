package com.group8.comp2300.domain.usecase.medical

import com.group8.comp2300.domain.model.medical.LabResult
import com.group8.comp2300.domain.repository.LabResultsRepository

class GetRecentLabResultsUseCase(private val repository: LabResultsRepository) {
    operator fun invoke(): List<LabResult> = repository.getRecentLabResults()
}
