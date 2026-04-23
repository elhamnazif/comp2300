package com.group8.comp2300.app.navigation

import androidx.navigation3.runtime.NavKey
import com.group8.comp2300.domain.model.medical.Appointment
import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen : NavKey {
    // Root App Shell
    @Serializable
    data object MainShell : Screen

    // Top Level Tabs
    @Serializable
    data object Home : Screen

    @Serializable
    data object HomeInbox : Screen

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

    @Serializable
    data object EditProfile : Screen

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
    data class ClinicDetail(val clinicId: String, val rescheduleAppointment: Appointment? = null) : Screen

    @Serializable
    data class BookingHistory(val highlightedAppointmentId: String? = null) : Screen

    @Serializable
    data class BookingConfirmation(
        val clinicId: String,
        val slotId: String,
        val rescheduleAppointment: Appointment? = null,
    ) : Screen

    @Serializable
    data class BookingSuccess(
        val clinicId: String,
        val appointmentId: String,
        val appointmentTime: Long,
        val wasRescheduled: Boolean = false,
    ) : Screen

    @Serializable
    data class ProductDetail(val productId: String) : Screen

    @Serializable
    data class ArticleDetail(val articleId: String) : Screen

    @Serializable
    data class QuizScreen(val quizId: String) : Screen

    // Secondary Screens
    @Serializable
    data object Medication : Screen

    @Serializable
    data object Routines : Screen

    @Serializable
    data object SelfDiagnosis : Screen

    @Serializable
    data object MedicalRecords : Screen

    @Serializable
    data object PrivacySecurity : Screen

    @Serializable
    data object Accessibility : Screen

    @Serializable
    data object Appearance : Screen

    @Serializable
    data object Notifications : Screen

    @Serializable
    data object HelpSupport : Screen

    @Serializable
    data object Chatbot : Screen

    @Serializable
    data object PrivacyLegalese : Screen

    @Serializable
    data object Cart : Screen

    @Serializable
    data object Checkout : Screen

    @Serializable
    data class OrderSuccess(val orderId: String, val total: Double) : Screen
}

val mainTabScreens: List<Screen> =
    listOf(
        Screen.Home,
        Screen.Booking,
        Screen.Calendar,
        Screen.Education,
        Screen.Profile,
    )

fun Screen.isMainTab(): Boolean = this in mainTabScreens

fun Screen.requiresAuthentication(): Boolean = when (this) {
    Screen.Chatbot,
    Screen.Checkout,
    Screen.EditProfile,
    Screen.MedicalRecords,
    is Screen.BookingHistory,
    -> true

    else -> false
}
