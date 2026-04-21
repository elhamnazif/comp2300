package com.group8.comp2300.feature.booking.navigation

import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.group8.comp2300.app.navigation.LocalNavigator
import com.group8.comp2300.app.navigation.LocalUseRootOverlayForShellChildren
import com.group8.comp2300.app.navigation.Screen
import com.group8.comp2300.app.navigation.overlayNavigationMetadata
import com.group8.comp2300.domain.model.session.AuthSession
import com.group8.comp2300.domain.repository.AuthRepository
import com.group8.comp2300.feature.booking.*
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

val bookingGraphModule = module {
    navigation<Screen.Booking>(metadata = ListDetailSceneStrategy.listPane()) {
        val navigator = LocalNavigator.current
        val useRootOverlayForShellChildren = LocalUseRootOverlayForShellChildren.current
        val viewModel = koinViewModel<BookingViewModel>()
        val authRepository = koinInject<AuthRepository>()
        val session by authRepository.session.collectAsState()
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
            isSignedIn = session is AuthSession.SignedIn,
            onSearchQueryChange = viewModel::updateSearchQuery,
            onTagToggle = viewModel::toggleTag,
            onMapModeChange = viewModel::setMapMode,
            onRefresh = viewModel::loadClinics,
            onClinicClick = { clinicId ->
                val destination = Screen.ClinicDetail(clinicId)
                if (useRootOverlayForShellChildren) {
                    navigator.navigate(
                        destination,
                    )
                } else {
                    navigator.navigateWithinShell(destination)
                }
            },
            onClinicSelect = viewModel::selectClinic,
            onViewBookings = {
                val destination = Screen.BookingHistory()
                if (useRootOverlayForShellChildren) {
                    navigator.navigate(
                        destination,
                    )
                } else {
                    navigator.navigateWithinShell(destination)
                }
            },
        )
    }

    navigation<Screen.ClinicDetail>(
        metadata = overlayNavigationMetadata(ListDetailSceneStrategy.detailPane()),
    ) { route ->
        val navigator = LocalNavigator.current
        val useRootOverlayForShellChildren = LocalUseRootOverlayForShellChildren.current

        BookingDetailsScreen(
            clinicId = route.clinicId,
            rescheduleAppointment = route.rescheduleAppointment,
            onBack = if (useRootOverlayForShellChildren) navigator::goBack else navigator::goBackWithinShell,
            onContinueToConfirmation = { clinicId, slotId, rescheduleAppointment ->
                navigator.navigate(Screen.BookingConfirmation(clinicId, slotId, rescheduleAppointment))
            },
        )
    }

    navigation<Screen.BookingHistory>(metadata = overlayNavigationMetadata()) { route ->
        val navigator = LocalNavigator.current
        val authRepository = koinInject<AuthRepository>()
        val session by authRepository.session.collectAsState()

        if (session !is AuthSession.SignedIn) {
            LaunchedEffect(route.highlightedAppointmentId) {
                navigator.requireAuth(Screen.BookingHistory(route.highlightedAppointmentId))
            }
            return@navigation
        }

        BookingHistoryScreen(
            highlightedAppointmentId = route.highlightedAppointmentId,
            onBack = navigator::goBack,
            onReschedule = { appointment ->
                appointment.clinicId?.let { clinicId ->
                    navigator.navigate(
                        Screen.ClinicDetail(
                            clinicId = clinicId,
                            rescheduleAppointment = appointment,
                        ),
                    )
                }
            },
        )
    }

    navigation<Screen.BookingConfirmation>(metadata = overlayNavigationMetadata()) { route ->
        val navigator = LocalNavigator.current
        val authRepository = koinInject<AuthRepository>()
        val session by authRepository.session.collectAsState()

        BookingConfirmationScreen(
            clinicId = route.clinicId,
            slotId = route.slotId,
            rescheduleAppointment = route.rescheduleAppointment,
            onBack = navigator::goBack,
            isSignedIn = session is AuthSession.SignedIn,
            onRequireAuth = {
                navigator.requireAuth(
                    Screen.BookingConfirmation(
                        route.clinicId,
                        route.slotId,
                        route.rescheduleAppointment,
                    ),
                )
            },
            onBookingConfirmed = { appointment, wasRescheduled ->
                navigator.navigate(
                    Screen.BookingSuccess(
                        clinicId = appointment.clinicId ?: route.clinicId,
                        appointmentId = appointment.id,
                        appointmentTime = appointment.appointmentTime,
                        wasRescheduled = wasRescheduled,
                    ),
                )
            },
        )
    }

    navigation<Screen.BookingSuccess>(metadata = overlayNavigationMetadata()) { route ->
        val navigator = LocalNavigator.current
        val viewModel = koinViewModel<BookingViewModel>()

        BookingSuccessScreen(
            clinicId = route.clinicId,
            appointmentId = route.appointmentId,
            appointmentTime = route.appointmentTime,
            wasRescheduled = route.wasRescheduled,
            onBack = {
                viewModel.clearBookingFlow()
                if (route.wasRescheduled) {
                    navigator.clearAndGoTo(Screen.Booking)
                    navigator.navigate(Screen.BookingHistory(route.appointmentId))
                } else {
                    navigator.clearAndGoTo(Screen.Booking)
                }
            },
            onViewCalendar = {
                viewModel.clearBookingFlow()
                navigator.clearAndGoTo(Screen.Calendar)
            },
            onDone = {
                viewModel.clearBookingFlow()
                if (route.wasRescheduled) {
                    navigator.clearAndGoTo(Screen.Booking)
                    navigator.navigate(Screen.BookingHistory(route.appointmentId))
                } else {
                    navigator.clearAndGoTo(Screen.Booking)
                }
            },
        )
    }
}
