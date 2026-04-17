package com.group8.comp2300.feature.booking.navigation

import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.group8.comp2300.app.navigation.LocalNavigator
import com.group8.comp2300.app.navigation.Screen
import com.group8.comp2300.domain.model.session.AuthSession
import com.group8.comp2300.domain.repository.AuthRepository
import com.group8.comp2300.feature.booking.BookingConfirmationScreen
import com.group8.comp2300.feature.booking.BookingDetailsScreen
import com.group8.comp2300.feature.booking.BookingScreen
import com.group8.comp2300.feature.booking.BookingSuccessScreen
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
        val state by viewModel.state.collectAsState()

        BookingScreen(
            clinics = state.clinics,
            filteredClinics = filteredClinics,
            selectedClinic = state.selectedClinic,
            searchQuery = state.searchQuery,
            selectedTag = state.selectedTag,
            isLoading = state.isLoadingClinics,
            isMapMode = state.isMapMode,
            onSearchQueryChange = viewModel::updateSearchQuery,
            onTagToggle = viewModel::toggleTag,
            onMapModeChange = viewModel::setMapMode,
            onRefresh = viewModel::loadClinics,
            onClinicClick = { clinicId -> navigator.navigate(Screen.ClinicDetail(clinicId)) },
            onClinicSelect = viewModel::selectClinic,
        )
    }

    navigation<Screen.ClinicDetail>(metadata = ListDetailSceneStrategy.detailPane()) { route ->
        val navigator = LocalNavigator.current

        BookingDetailsScreen(
            clinicId = route.clinicId,
            onBack = navigator::goBack,
            onContinueToConfirmation = { clinicId, slotId ->
                navigator.navigate(Screen.BookingConfirmation(clinicId, slotId))
            },
        )
    }

    navigation<Screen.BookingConfirmation>(metadata = ListDetailSceneStrategy.detailPane()) { route ->
        val navigator = LocalNavigator.current
        val authRepository = koinInject<AuthRepository>()
        val session by authRepository.session.collectAsState()

        BookingConfirmationScreen(
            clinicId = route.clinicId,
            slotId = route.slotId,
            onBack = navigator::goBack,
            isSignedIn = session is AuthSession.SignedIn,
            onRequireAuth = { navigator.requireAuth(Screen.BookingConfirmation(route.clinicId, route.slotId)) },
            onBookingConfirmed = { appointment ->
                navigator.navigate(
                    Screen.BookingSuccess(
                        clinicId = appointment.clinicId ?: route.clinicId,
                        appointmentId = appointment.id,
                        appointmentTime = appointment.appointmentTime,
                    ),
                )
            },
        )
    }

    navigation<Screen.BookingSuccess>(metadata = ListDetailSceneStrategy.detailPane()) { route ->
        val navigator = LocalNavigator.current
        val viewModel = koinViewModel<BookingViewModel>()

        BookingSuccessScreen(
            clinicId = route.clinicId,
            appointmentId = route.appointmentId,
            appointmentTime = route.appointmentTime,
            onBack = {
                viewModel.clearBookingFlow()
                navigator.clearAndGoTo(Screen.Booking)
            },
            onViewCalendar = {
                viewModel.clearBookingFlow()
                navigator.clearAndGoTo(Screen.Calendar)
            },
            onDone = {
                viewModel.clearBookingFlow()
                navigator.clearAndGoTo(Screen.Booking)
            },
        )
    }
}
