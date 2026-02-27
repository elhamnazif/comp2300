package com.group8.comp2300.di

import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.group8.comp2300.presentation.navigation.LocalNavigator
import com.group8.comp2300.presentation.navigation.Screen
import com.group8.comp2300.presentation.screens.medical.BookingDetailsScreen
import com.group8.comp2300.presentation.screens.medical.BookingScreen
import com.group8.comp2300.presentation.screens.medical.BookingViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

val medicalNavigationModule = module {
    navigation<Screen.Booking>(metadata = ListDetailSceneStrategy.listPane()) {
        val navigator = LocalNavigator.current
        val viewModel = koinViewModel<BookingViewModel>()
        val filteredClinics by viewModel.filteredClinics.collectAsState()
        val selectedClinic by viewModel.selectedClinic.collectAsState()
        val searchQuery by viewModel.searchQuery.collectAsState()

        BookingScreen(
            clinics = filteredClinics,
            selectedClinic = selectedClinic,
            searchQuery = searchQuery,
            onSearchQueryChange = { viewModel.updateSearchQuery(it) },
            onClinicClick = { clinicId -> navigator.navigate(Screen.ClinicDetail(clinicId)) },
            onClinicSelect = viewModel::selectClinic,
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
            },
        )
    }
}
