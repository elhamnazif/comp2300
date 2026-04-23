package com.group8.comp2300.di

import com.group8.comp2300.data.auth.TokenManager
import com.group8.comp2300.data.auth.TokenManagerImpl
import com.group8.comp2300.data.database.DatabaseDriverFactory
import com.group8.comp2300.data.database.createDatabase
import com.group8.comp2300.data.local.*
import com.group8.comp2300.data.notifications.*
import com.group8.comp2300.data.offline.*
import com.group8.comp2300.data.remote.*
import com.group8.comp2300.data.repository.*
import com.group8.comp2300.data.repository.medical.*
import com.group8.comp2300.domain.repository.*
import com.group8.comp2300.domain.repository.medical.*
import com.group8.comp2300.domain.usecase.auth.*
import com.group8.comp2300.domain.usecase.shop.GetProductsUseCase
import com.russhwolf.settings.Settings
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val coreModule = module {
    single {
        val tokenProvider: TokenProvider = get()
        tokenProviderDelegate.setDelegate(tokenProvider)
        createHttpClient()
    }
    singleOf(::ApiServiceImpl) { bind<ApiService>() }

    single { createDatabase(get<DatabaseDriverFactory>()) }

    // Token management
    single { SessionDataSource(get()) }
    single { PinDataSource(get()) }
    single { PinRateLimiter(get()) }
    single<TokenManager> { TokenManagerImpl(get()) }

    // Set up token provider delegate for HTTP client auth
    // This connects the TokenManager to the HttpClient's Bearer auth
    single<TokenProvider> {
        object : TokenProvider {
            private val tokenManager: TokenManager = get()

            override suspend fun getAccessToken(): String? = tokenManager.getAccessToken()

            override suspend fun getRefreshToken(): String? = tokenManager.getRefreshToken()

            override suspend fun saveTokens(accessToken: String, refreshToken: String, expiresAt: Long) {
                val userId = tokenManager.getUserId() ?: return
                tokenManager.saveTokens(userId, accessToken, refreshToken, expiresAt)
            }

            override suspend fun clearTokens() = tokenManager.clearTokens()
        }
    }

    // Offline-first local data sources
    single { AppointmentLocalDataSource(get()) }
    single { MedicationLocalDataSource(get()) }
    single { RoutineLocalDataSource(get()) }
    single { RoutineOccurrenceOverrideLocalDataSource(get()) }
    single { MoodLocalDataSource(get()) }
    single { MedicationLogLocalDataSource(get()) }
    single { ReminderLocalDataSource(get()) }
    single { OutboxDataSource(get()) }
    single { ProductLocalDataSource(get()) }
    single { CartLocalDataSource(get()) }
    single { PersonalDataCleaner(get()) }
    single { Settings() }
    single { AppearanceSettingsDataSource(get()) }
    single { AccessibilitySettingsDataSource(get()) }
    single { LocalAuthSettingsDataSource(get(), get()) }
    single { PrivacySettingsDataSource(get()) }
    single { OfflineMapSettingsDataSource(get()) }
    single { NotificationContentFormatter() }
    single { RoutineNotificationRegistry(get()) }
    single<RoutineNotificationScheduler> {
        RoutineNotificationSchedulerImpl(
            routineLocal = get(),
            routineOccurrenceOverrideLocal = get(),
            registry = get(),
            platform = get<RoutineNotificationService>(),
            privacySettingsDataSource = get(),
            notificationContentFormatter = get(),
        )
    }
    single { RoutineNotificationBootstrap(get()) }
    singleOf(::MedicationUpsertMutationHandler) { bind<OfflineMutationHandler>() }
    singleOf(::MedicationDeleteMutationHandler) { bind<OfflineMutationHandler>() }
    singleOf(::RoutineUpsertMutationHandler) { bind<OfflineMutationHandler>() }
    singleOf(::RoutineDeleteMutationHandler) { bind<OfflineMutationHandler>() }
    singleOf(::RoutineOccurrenceOverrideMutationHandler) { bind<OfflineMutationHandler>() }
    singleOf(::MedicationLogMutationHandler) { bind<OfflineMutationHandler>() }
    singleOf(::MoodMutationHandler) { bind<OfflineMutationHandler>() }
    single { OfflineMutationHandlers(getAll()) }

    singleOf(::MedicalOfflineCacheRefresher) { bind<OfflineCacheRefresher>() }
    single<OfflineSyncCoordinator> { OfflineSyncCoordinatorImpl(get(), get(), get(), get()) }
    single { OfflineMutationQueue(get(), get(), get()) }

    single<ShopRepository> { ShopRepositoryImpl(get(), get(), get(), get()) }
    single<AppointmentDataRepository> { AppointmentDataRepositoryImpl(get(), get()) }
    single<MedicationDataRepository> { MedicationDataRepositoryImpl(get(), get(), get()) }
    single<RoutineDataRepository> { RoutineDataRepositoryImpl(get(), get(), get(), get()) }
    single<MedicationLogDataRepository> { MedicationLogDataRepositoryImpl(get(), get(), get(), get(), get(), get()) }
    single<MoodDataRepository> { MoodDataRepositoryImpl(get(), get(), get()) }
    single<CalendarDataRepository> { CalendarDataRepositoryImpl(get(), get(), get(), get(), get()) }
    single<MedicalRecordDataRepository> { MedicalRecordDataRepositoryImpl(get()) }
    single<AuthRepository> { AuthRepositoryImpl(get(), get(), get(), get(), get()) }
    single<ChatbotRepository> { ChatbotRepositoryImpl(get()) }
    single<ClinicRepository> { ClinicRepositoryImpl(get()) }
    single<EducationRepository> { EducationRepositoryImpl(get()) }
    single<ReminderRepository> { ReminderRepositoryImpl(get()) }

    singleOf(::GetProductsUseCase)
    singleOf(::LoginUseCase)
    singleOf(::ActivateAccountUseCase)
    singleOf(::CompleteProfileUseCase)
    singleOf(::ForgotPasswordUseCase)
    singleOf(::PreregisterUseCase)
    singleOf(::ResendVerificationEmailUseCase)
    singleOf(::ResetPasswordUseCase)
}
