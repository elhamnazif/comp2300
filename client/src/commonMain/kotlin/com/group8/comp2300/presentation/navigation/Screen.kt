package com.group8.comp2300.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen : NavKey {
    // Top Level Tabs
    @Serializable
    data object Home : Screen

    @Serializable
    data object Booking : Screen

    @Serializable
    data object Calendar : Screen

    @Serializable
    data object Shop : Screen

    @Serializable
    data object Education : Screen

    @Serializable
    data object Profile : Screen

    // Auth & Onboarding
    @Serializable
    data object Onboarding : Screen

    @Serializable
    data object Login : Screen

    @Serializable
    data class EmailVerification(val email: String) : Screen

    @Serializable
    data class CompleteProfile(val email: String) : Screen

    @Serializable
    data object ForgotPassword : Screen

    @Serializable
    data class ResetPassword(val token: String) : Screen

    // Detail Screens
    @Serializable
    data class ClinicDetail(val clinicId: String) : Screen

    @Serializable
    data class ProductDetail(val productId: String) : Screen

    @Serializable
    data class VideoDetail(val videoId: String) : Screen

    @Serializable
    data class QuizScreen(val quizId: String) : Screen

    // Secondary Screens
    @Serializable
    data object Medication : Screen

    @Serializable
    data object SelfDiagnosis : Screen

    @Serializable
    data object LabResults : Screen

    @Serializable
    data object PrivacySecurity : Screen

    @Serializable
    data object Notifications : Screen

    @Serializable
    data object HelpSupport : Screen
}
