package com.group8.comp2300.presentation.screens.medical.labresults

import androidx.lifecycle.ViewModel
import com.group8.comp2300.domain.model.medical.LabResult
import com.group8.comp2300.domain.usecase.medical.GetRecentLabResultsUseCase

class LabResultsViewModel(
    getRecentLabResults: GetRecentLabResultsUseCase,
) : ViewModel() {
    val results: List<LabResult> = getRecentLabResults()
}
