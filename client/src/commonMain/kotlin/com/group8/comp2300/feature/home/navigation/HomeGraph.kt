package com.group8.comp2300.feature.home.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.group8.comp2300.app.navigation.LocalNavigator
import com.group8.comp2300.app.navigation.LocalUseRootOverlayForShellChildren
import com.group8.comp2300.app.navigation.Screen
import com.group8.comp2300.app.navigation.overlayNavigationMetadata
import com.group8.comp2300.domain.model.session.userOrNull
import com.group8.comp2300.domain.repository.AuthRepository
import com.group8.comp2300.feature.home.HomeInboxAction
import com.group8.comp2300.feature.home.InboxScreen
import com.group8.comp2300.feature.home.HomeScreen
import com.group8.comp2300.feature.home.HomeViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

val homeGraphModule = module {
    navigation<Screen.Home> {
        val navigator = LocalNavigator.current
        val useRootOverlayForShellChildren = LocalUseRootOverlayForShellChildren.current
        val viewModel = koinViewModel<HomeViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val authRepository = koinInject<AuthRepository>()
        val session by authRepository.session.collectAsStateWithLifecycle()

        val navigateToOverlay: (Screen) -> Unit = { destination ->
            if (useRootOverlayForShellChildren) {
                navigator.navigate(destination)
            } else {
                navigator.navigateWithinShell(destination)
            }
        }

        HomeScreen(
            state = state,
            userFirstName = session.userOrNull?.firstName,
            onRetry = viewModel::refresh,
            onNavigateToInbox = { navigateToOverlay(Screen.HomeInbox) },
            onNavigateToShop = { navigator.navigate(Screen.Shop) },
            onNavigateToMedication = { navigator.navigate(Screen.Medication) },
            onNavigateToRoutines = { navigator.navigate(Screen.Routines) },
            onNavigateToChatbot = {
                navigateToOverlay(Screen.Chatbot)
            },
            onNavigateToSymptomChecker = { navigator.navigate(Screen.SelfDiagnosis) },
        )
    }

    navigation<Screen.HomeInbox>(metadata = overlayNavigationMetadata()) {
        val navigator = LocalNavigator.current
        val useRootOverlayForShellChildren = LocalUseRootOverlayForShellChildren.current
        val viewModel = koinViewModel<HomeViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()

        val navigateToOverlay: (Screen) -> Unit = { destination ->
            if (useRootOverlayForShellChildren) {
                navigator.navigate(destination)
            } else {
                navigator.navigateWithinShell(destination)
            }
        }

        InboxScreen(
            state = state,
            onBack = if (useRootOverlayForShellChildren) navigator::goBack else navigator::goBackWithinShell,
            onRetry = viewModel::refresh,
            onItemClick = { action ->
                when (action) {
                    HomeInboxAction.OpenCalendar -> {
                        if (useRootOverlayForShellChildren) {
                            navigator.goBack()
                        }
                        navigator.navigate(Screen.Calendar)
                    }
                    is HomeInboxAction.OpenBookingHistory -> {
                        navigateToOverlay(Screen.BookingHistory(action.appointmentId))
                    }

                    HomeInboxAction.OpenNotificationSettings -> {
                        navigateToOverlay(Screen.Notifications)
                    }
                }
            },
        )
    }
}
