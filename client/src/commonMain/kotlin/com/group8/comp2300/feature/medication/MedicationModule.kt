package com.group8.comp2300.feature.medication

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val medicationModule = module {
    viewModelOf(::MedicationViewModel)
}
