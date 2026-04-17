package com.group8.comp2300.feature.education

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val educationModule = module {
    viewModelOf(::EducationViewModel)
}
