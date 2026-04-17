package com.group8.comp2300.feature.records

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val recordsModule = module {
    viewModelOf(::MedicalRecordViewModel)
}
