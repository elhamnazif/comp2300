package com.group8.comp2300.feature.routine

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val routineModule = module {
    viewModelOf(::RoutineViewModel)
}
