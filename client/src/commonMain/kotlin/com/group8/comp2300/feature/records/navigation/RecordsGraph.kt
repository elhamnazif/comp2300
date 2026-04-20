package com.group8.comp2300.feature.records.navigation

import com.group8.comp2300.app.navigation.LocalNavigator
import com.group8.comp2300.app.navigation.overlayNavigationMetadata
import com.group8.comp2300.app.navigation.Screen
import com.group8.comp2300.feature.records.MedicalRecordScreen
import org.koin.compose.viewmodel.koinViewModel
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

val recordsGraphModule = module {
    navigation<Screen.MedicalRecords>(metadata = overlayNavigationMetadata()) {
        val navigator = LocalNavigator.current
        MedicalRecordScreen(
            viewModel = koinViewModel(),
            onNavigateBack = navigator::goBack,
        )
    }
}
