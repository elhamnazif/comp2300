package com.group8.comp2300.app

import com.group8.comp2300.feature.auth.navigation.authGraphModule
import com.group8.comp2300.feature.booking.navigation.bookingGraphModule
import com.group8.comp2300.feature.calendar.navigation.calendarGraphModule
import com.group8.comp2300.feature.education.navigation.educationGraphModule
import com.group8.comp2300.feature.home.navigation.homeGraphModule
import com.group8.comp2300.feature.labresults.navigation.labResultsGraphModule
import com.group8.comp2300.feature.medication.navigation.medicationGraphModule
import com.group8.comp2300.feature.profile.navigation.profileGraphModule
import com.group8.comp2300.feature.records.navigation.recordsGraphModule
import com.group8.comp2300.feature.routine.navigation.routineGraphModule
import com.group8.comp2300.feature.selfdiagnosis.navigation.selfDiagnosisGraphModule
import com.group8.comp2300.feature.settings.navigation.settingsGraphModule
import com.group8.comp2300.feature.shop.navigation.shopGraphModule
import org.koin.dsl.module

val navigationGraphModule = module {
    includes(
        authGraphModule,
        homeGraphModule,
        profileGraphModule,
        calendarGraphModule,
        medicationGraphModule,
        routineGraphModule,
        selfDiagnosisGraphModule,
        labResultsGraphModule,
        recordsGraphModule,
        settingsGraphModule,
        shopGraphModule,
        bookingGraphModule,
        educationGraphModule,
    )
}
