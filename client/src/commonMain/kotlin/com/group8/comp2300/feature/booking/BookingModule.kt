package com.group8.comp2300.feature.booking

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val bookingModule = module {
    viewModelOf(::BookingViewModel)
}
