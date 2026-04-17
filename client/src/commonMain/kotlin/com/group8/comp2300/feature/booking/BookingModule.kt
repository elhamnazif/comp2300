package com.group8.comp2300.feature.booking

import com.group8.comp2300.services.ClinicFilterService
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val bookingModule = module {
    singleOf(::ClinicFilterService)
    viewModelOf(::BookingViewModel)
}
