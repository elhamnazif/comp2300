package com.group8.comp2300.di

// ViewModels are now in their respective screen packages and picked up by wildcards above
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.group8.comp2300.domain.model.Screen
import com.group8.comp2300.domain.model.education.ContentType
import com.group8.comp2300.navigation.LocalNavigator
import com.group8.comp2300.presentation.ui.screens.auth.*
import com.group8.comp2300.presentation.ui.screens.education.*
import com.group8.comp2300.presentation.ui.screens.home.*
import com.group8.comp2300.presentation.ui.screens.medical.*
import com.group8.comp2300.presentation.ui.screens.profile.*
import com.group8.comp2300.presentation.ui.screens.shop.*
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

// https://insert-koin.io/docs/reference/koin-android/dsl-update
@OptIn(KoinExperimentalAPI::class, ExperimentalMaterial3AdaptiveApi::class)
val navigationModule = module {

    // ═══════════════════════════════════════════════════════════════════════════
    // ONBOARDING & AUTH
    // ═══════════════════════════════════════════════════════════════════════════

    navigation<Screen.Onboarding> {
        val navigator = LocalNavigator.current
        OnboardingScreen(
            isGuest = navigator.isGuest,
            onRequireAuth = navigator::requireAuth,
            onFinished = { navigator.clearAndGoTo(Screen.Home) },
        )
    }

    navigation<Screen.Login> {
        val navigator = LocalNavigator.current
        LoginScreen(
            onLoginSuccess = navigator::goBack,
            onDismiss = navigator::goBack,
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MAIN TABS
    // ═══════════════════════════════════════════════════════════════════════════

    navigation<Screen.Home> {
        val navigator = LocalNavigator.current
        HomeScreen(
            onNavigateToShop = { navigator.navigate(Screen.Shop) },
            onNavigateToCalendar = { navigator.navigate(Screen.Calendar) },
            onNavigateToEducation = { navigator.navigate(Screen.Education) },
            onNavigateToMedication = { navigator.navigate(Screen.Medication) },
            onNavigateToSymptomChecker = { navigator.navigate(Screen.SelfDiagnosis) },
            onNavigateToClinicMap = { navigator.navigate(Screen.Booking) },
        )
    }

    navigation<Screen.Calendar> {
        val navigator = LocalNavigator.current
        CalendarScreen(
            isGuest = navigator.isGuest,
            onRequireAuth = navigator::requireAuth,
        )
    }

    navigation<Screen.Profile> {
        val navigator = LocalNavigator.current
        ProfileScreen(
            isGuest = navigator.isGuest,
            onRequireAuth = navigator::requireAuth,
            onNavigateToLabResults = { navigator.navigate(Screen.LabResults) },
            onNavigateToPrivacySecurity = { navigator.navigate(Screen.PrivacySecurity) },
            onNavigateToNotifications = { navigator.navigate(Screen.Notifications) },
            onNavigateToHelpSupport = { navigator.navigate(Screen.HelpSupport) },
        )
    }

    navigation<Screen.Medication> {
        val navigator = LocalNavigator.current
        MedicationScreen(
            isGuest = navigator.isGuest,
            onRequireAuth = navigator::requireAuth,
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SHOP (Adaptive List/Detail)
    // ═══════════════════════════════════════════════════════════════════════════

    navigation<Screen.Shop>(metadata = ListDetailSceneStrategy.listPane()) {
        val navigator = LocalNavigator.current
        val viewModel = koinViewModel<ShopViewModel>()
        val uiState by viewModel.state.collectAsState()

        ShopScreen(
            products = uiState.products,
            selectedCategory = uiState.selectedCategory,
            cartItemCount = uiState.cartItemCount,
            // When clicking a product, the Navigator should know to show the Detail pane
            onProductClick = { prodId -> navigator.navigate(Screen.ProductDetail(prodId)) },
            onCategorySelect = viewModel::selectCategory,
            onAddToCart = viewModel::addToCart,
            isGuest = navigator.isGuest,
            onRequireAuth = navigator::requireAuth,
        )
    }

    navigation<Screen.ProductDetail>(metadata = ListDetailSceneStrategy.detailPane()) { route ->
        val navigator = LocalNavigator.current

        ProductDetailScreen(
            productId = route.productId,
            onBack = navigator::goBack,
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BOOKING / CLINICS (Adaptive List/Detail)
    // ═══════════════════════════════════════════════════════════════════════════

    navigation<Screen.Booking>(metadata = ListDetailSceneStrategy.listPane()) {
        val navigator = LocalNavigator.current
        val viewModel = koinViewModel<BookingViewModel>()
        val uiState by viewModel.state.collectAsState()

        BookingScreen(
            clinics = uiState.clinics,
            selectedClinic = uiState.selectedClinic,
            onClinicClick = { clinicId -> navigator.navigate(Screen.ClinicDetail(clinicId)) },
            onClinicSelect = viewModel::selectClinic,
            isGuest = navigator.isGuest,
            onRequireAuth = navigator::requireAuth,
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

    // ═══════════════════════════════════════════════════════════════════════════
    // EDUCATION (Adaptive List/Detail)
    // ═══════════════════════════════════════════════════════════════════════════

    navigation<Screen.Education>(metadata = ListDetailSceneStrategy.listPane()) {
        val navigator = LocalNavigator.current
        val viewModel = koinViewModel<EducationViewModel>()
        val uiState by viewModel.state.collectAsState()

        EducationScreen(
            filteredContent = uiState.filteredContent,
            featuredItem = uiState.featuredItem,
            selectedCategory = uiState.selectedCategory,
            onContentClick = { id ->
                // Note: You might need a way to determine type without fetching the whole item
                // if you want this navigation logic to be purely route-based.
                val content = viewModel.getContentById(id)
                if (content?.type == ContentType.QUIZ) {
                    navigator.navigate(Screen.QuizScreen(id))
                } else {
                    navigator.navigate(Screen.VideoDetail(id))
                }
            },
            onCategorySelect = viewModel::selectCategory,
        )
    }

    navigation<Screen.VideoDetail>(metadata = ListDetailSceneStrategy.detailPane()) { route ->
        val navigator = LocalNavigator.current
        val viewModel = koinViewModel<EducationViewModel> { parametersOf(route.videoId) }

        VideoDetailScreen(
            viewModel = viewModel,
            videoId = route.videoId,
            onBack = navigator::goBack,
            onActionClick = { action ->
                if (action.contains("Clinic")) {
                    navigator.clearAndGoTo(Screen.Booking)
                }
            },
        )
    }

    navigation<Screen.QuizScreen>(metadata = ListDetailSceneStrategy.detailPane()) { route ->
        val navigator = LocalNavigator.current
        val viewModel = koinViewModel<EducationViewModel> { parametersOf(route.quizId) }

        QuizScreen(
            viewModel = viewModel,
            quizId = route.quizId,
            onBack = navigator::goBack,
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SECONDARY SCREENS
    // ═══════════════════════════════════════════════════════════════════════════

    navigation<Screen.SelfDiagnosis> {
        val navigator = LocalNavigator.current
        SelfDiagnosisScreen(
            onBack = navigator::goBack,
            onNavigateToBooking = { navigator.navigate(Screen.Booking) },
        )
    }

    navigation<Screen.LabResults> {
        val navigator = LocalNavigator.current
        LabResultsScreen(
            onBack = navigator::goBack,
            onScheduleTest = { navigator.navigate(Screen.Booking) },
        )
    }

    navigation<Screen.PrivacySecurity> {
        val navigator = LocalNavigator.current
        PrivacySecurityScreen(onBack = navigator::goBack)
    }

    navigation<Screen.Notifications> {
        val navigator = LocalNavigator.current
        NotificationsScreen(onBack = navigator::goBack)
    }

    navigation<Screen.HelpSupport> {
        val navigator = LocalNavigator.current
        HelpSupportScreen(onBack = navigator::goBack)
    }
}
