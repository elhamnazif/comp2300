package com.group8.comp2300.app

import com.group8.comp2300.app.appBindingsModule
import com.group8.comp2300.core.security.pin.pinModule
import com.group8.comp2300.feature.auth.authModule
import com.group8.comp2300.feature.booking.bookingModule
import com.group8.comp2300.feature.calendar.calendarModule
import com.group8.comp2300.feature.education.educationModule
import com.group8.comp2300.feature.medication.medicationModule
import com.group8.comp2300.feature.profile.profileModule
import com.group8.comp2300.feature.records.recordsModule
import com.group8.comp2300.feature.routine.routineModule
import com.group8.comp2300.feature.shop.shopModule
import com.group8.comp2300.platform.files.fileModule
import org.koin.dsl.module

val appModule = module {
    includes(
        appBindingsModule,
        pinModule,
        fileModule,
        authModule,
        bookingModule,
        calendarModule,
        educationModule,
        medicationModule,
        profileModule,
        recordsModule,
        routineModule,
        shopModule,
    )
}
