package com.group8.comp2300.feature.labresults

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val labResultsModule = module {
    viewModelOf(::LabResultsViewModel)
}
