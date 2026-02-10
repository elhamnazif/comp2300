package com.group8.comp2300.di

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.group8.comp2300.presentation.navigation.LocalNavigator
import com.group8.comp2300.presentation.navigation.Screen
import com.group8.comp2300.presentation.ui.screens.medical.BookingDetailsScreen
import com.group8.comp2300.presentation.ui.screens.medical.BookingScreen
import com.group8.comp2300.presentation.ui.screens.medical.BookingViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class, ExperimentalMaterial3AdaptiveApi::class)
val medicalNavigationModule = module {
    navigation<Screen.Booking>(metadata = ListDetailSceneStrategy.listPane()) {
        val navigator = LocalNavigator.current
        val viewModel = koinViewModel<BookingViewModel>()
        val uiState by viewModel.state.collectAsState()

        BookingScreen(
            clinics = uiState.clinics,
            selectedClinic = uiState.selectedClinic,
            onClinicClick = { clinicId -> navigator.navigate(Screen.ClinicDetail(clinicId)) },
            onClinicSelect = viewModel::selectClinic
        )
    }

    navigation<Screen.ClinicDetail>(metadata = ListDetailSceneStrategy.detailPane()) { route ->
        val navigator = LocalNavigator.current

        BookingDetailsScreen(
            clinicId = route.clinicId,
            onBack = navigator::goBack,
            onConfirm = {
                if (navigator.isGuest) {
                    navigator.requireAuth()
                } else {
                    // TODO: Handle booking confirmation
                }
            }
        )
    }
}
