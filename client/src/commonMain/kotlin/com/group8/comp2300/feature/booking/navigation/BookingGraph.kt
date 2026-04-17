package com.group8.comp2300.feature.booking.navigation

import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.group8.comp2300.app.navigation.LocalNavigator
import com.group8.comp2300.app.navigation.Screen
import com.group8.comp2300.domain.model.session.AuthSession
import com.group8.comp2300.domain.repository.AuthRepository
import com.group8.comp2300.feature.booking.BookingDetailsScreen
import com.group8.comp2300.feature.booking.BookingScreen
import com.group8.comp2300.feature.booking.BookingViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

val bookingGraphModule = module {
    navigation<Screen.Booking>(metadata = ListDetailSceneStrategy.listPane()) {
        val navigator = LocalNavigator.current
        val viewModel = koinViewModel<BookingViewModel>()
        val filteredClinics by viewModel.filteredClinics.collectAsState()
        val selectedClinic by viewModel.selectedClinic.collectAsState()
        val searchQuery by viewModel.searchQuery.collectAsState()

        BookingScreen(
            allClinics = viewModel.allClinics,
            filteredClinics = filteredClinics,
            selectedClinic = selectedClinic,
            searchQuery = searchQuery,
            onSearchQueryChange = { viewModel.updateSearchQuery(it) },
            onClinicClick = { clinicId -> navigator.navigate(Screen.ClinicDetail(clinicId)) },
            onClinicSelect = viewModel::selectClinic,
        )
    }

    navigation<Screen.ClinicDetail>(metadata = ListDetailSceneStrategy.detailPane()) { route ->
        val navigator = LocalNavigator.current
        val authRepository = koinInject<AuthRepository>()
        val session by authRepository.session.collectAsState()

        BookingDetailsScreen(
            clinicId = route.clinicId,
            onBack = navigator::goBack,
            isSignedIn = session is AuthSession.SignedIn,
            onRequireAuth = { navigator.requireAuth(Screen.ClinicDetail(route.clinicId)) },
            onBookingConfirmed = {
                navigator.navigate(Screen.Calendar)
            },
        )
    }
}
