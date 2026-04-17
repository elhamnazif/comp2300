package com.group8.comp2300.feature.selfdiagnosis.navigation

import com.group8.comp2300.app.navigation.LocalNavigator
import com.group8.comp2300.app.navigation.Screen
import com.group8.comp2300.feature.selfdiagnosis.SelfDiagnosisScreen
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

val selfDiagnosisGraphModule = module {
    navigation<Screen.SelfDiagnosis> {
        val navigator = LocalNavigator.current
        SelfDiagnosisScreen(
            onBack = navigator::goBack,
            onNavigateToBooking = { navigator.navigate(Screen.Booking) },
        )
    }
}
