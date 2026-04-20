package com.group8.comp2300.feature.medication.navigation

import com.group8.comp2300.app.navigation.LocalNavigator
import com.group8.comp2300.app.navigation.overlayNavigationMetadata
import com.group8.comp2300.app.navigation.Screen
import com.group8.comp2300.feature.medication.MedicationScreen
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

val medicationGraphModule = module {
    navigation<Screen.Medication>(metadata = overlayNavigationMetadata()) {
        val navigator = LocalNavigator.current
        MedicationScreen(
            onBack = navigator::goBack,
        )
    }
}
