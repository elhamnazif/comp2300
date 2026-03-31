package com.group8.comp2300.di

import com.group8.comp2300.data.auth.TokenManager
import com.group8.comp2300.data.auth.TokenManagerImpl
import com.group8.comp2300.data.database.DatabaseDriverFactory
import com.group8.comp2300.data.database.createDatabase
import com.group8.comp2300.data.local.AppointmentLocalDataSource
import com.group8.comp2300.data.local.MedicationLocalDataSource
import com.group8.comp2300.data.local.MedicationLogLocalDataSource
import com.group8.comp2300.data.local.MoodLocalDataSource
import com.group8.comp2300.data.local.OutboxDataSource
import com.group8.comp2300.data.local.PersonalDataCleaner
import com.group8.comp2300.data.local.ProductLocalDataSource
import com.group8.comp2300.data.local.ReminderLocalDataSource
import com.group8.comp2300.data.local.RoutineOccurrenceOverrideLocalDataSource
import com.group8.comp2300.data.local.RoutineLocalDataSource
import com.group8.comp2300.data.local.SessionDataSource
import com.group8.comp2300.data.offline.AppointmentMutationHandler
import com.group8.comp2300.data.offline.CompositeOfflineDataRefresher
import com.group8.comp2300.data.offline.MedicalOfflineDataRefresher
import com.group8.comp2300.data.offline.MedicationDeleteMutationHandler
import com.group8.comp2300.data.offline.MedicationLogMutationHandler
import com.group8.comp2300.data.offline.MedicationUpsertMutationHandler
import com.group8.comp2300.data.offline.MoodMutationHandler
import com.group8.comp2300.data.offline.MutationHandlerRegistry
import com.group8.comp2300.data.offline.OfflineDataRefresher
import com.group8.comp2300.data.offline.OfflineMutationHandler
import com.group8.comp2300.data.offline.QueuedWriteDispatcher
import com.group8.comp2300.data.offline.RoutineDeleteMutationHandler
import com.group8.comp2300.data.offline.RoutineOccurrenceOverrideMutationHandler
import com.group8.comp2300.data.offline.RoutineUpsertMutationHandler
import com.group8.comp2300.data.offline.SyncCoordinatorImpl
import com.group8.comp2300.data.remote.ApiService
import com.group8.comp2300.data.remote.ApiServiceImpl
import com.group8.comp2300.data.remote.TokenProvider
import com.group8.comp2300.data.remote.createHttpClient
import com.group8.comp2300.data.remote.tokenProviderDelegate
import com.group8.comp2300.data.repository.AuthRepositoryImpl
import com.group8.comp2300.data.repository.ClinicRepositoryImpl
import com.group8.comp2300.data.repository.EducationRepositoryImpl
import com.group8.comp2300.data.repository.LabResultsRepositoryImpl
import com.group8.comp2300.data.repository.ReminderRepositoryImpl
import com.group8.comp2300.data.repository.ShopRepositoryImpl
import com.group8.comp2300.data.repository.medical.AppointmentDataRepositoryImpl
import com.group8.comp2300.data.repository.medical.CalendarDataRepositoryImpl
import com.group8.comp2300.data.repository.medical.MedicationDataRepositoryImpl
import com.group8.comp2300.data.repository.medical.MedicationLogDataRepositoryImpl
import com.group8.comp2300.data.repository.medical.MoodDataRepositoryImpl
import com.group8.comp2300.data.repository.medical.RoutineDataRepositoryImpl
import com.group8.comp2300.domain.repository.AuthRepository
import com.group8.comp2300.domain.repository.ClinicRepository
import com.group8.comp2300.domain.repository.EducationRepository
import com.group8.comp2300.domain.repository.LabResultsRepository
import com.group8.comp2300.domain.repository.ReminderRepository
import com.group8.comp2300.domain.repository.ShopRepository
import com.group8.comp2300.domain.repository.medical.AppointmentDataRepository
import com.group8.comp2300.domain.repository.medical.CalendarDataRepository
import com.group8.comp2300.domain.repository.medical.MedicationDataRepository
import com.group8.comp2300.domain.repository.medical.MedicationLogDataRepository
import com.group8.comp2300.domain.repository.medical.MoodDataRepository
import com.group8.comp2300.domain.repository.medical.RoutineDataRepository
import com.group8.comp2300.domain.repository.medical.SyncCoordinator
import com.group8.comp2300.domain.usecase.auth.ActivateAccountUseCase
import com.group8.comp2300.domain.usecase.auth.CompleteProfileUseCase
import com.group8.comp2300.domain.usecase.auth.ForgotPasswordUseCase
import com.group8.comp2300.domain.usecase.auth.LoginUseCase
import com.group8.comp2300.domain.usecase.auth.PreregisterUseCase
import com.group8.comp2300.domain.usecase.auth.RegisterUseCase
import com.group8.comp2300.domain.usecase.auth.ResendVerificationEmailUseCase
import com.group8.comp2300.domain.usecase.auth.ResetPasswordUseCase
import com.group8.comp2300.domain.usecase.medical.GetRecentLabResultsUseCase
import com.group8.comp2300.domain.usecase.shop.GetProductsUseCase
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
    single { PersonalDataCleaner(get()) }
    singleOf(::AppointmentMutationHandler) { bind<OfflineMutationHandler>() }
    singleOf(::MedicationUpsertMutationHandler) { bind<OfflineMutationHandler>() }
    singleOf(::MedicationDeleteMutationHandler) { bind<OfflineMutationHandler>() }
    singleOf(::RoutineUpsertMutationHandler) { bind<OfflineMutationHandler>() }
    singleOf(::RoutineDeleteMutationHandler) { bind<OfflineMutationHandler>() }
    singleOf(::RoutineOccurrenceOverrideMutationHandler) { bind<OfflineMutationHandler>() }
    singleOf(::MedicationLogMutationHandler) { bind<OfflineMutationHandler>() }
    singleOf(::MoodMutationHandler) { bind<OfflineMutationHandler>() }
    single { MutationHandlerRegistry(getAll()) }

    singleOf(::MedicalOfflineDataRefresher)
    single<OfflineDataRefresher> { CompositeOfflineDataRefresher(listOf(get<MedicalOfflineDataRefresher>())) }
    single<SyncCoordinator> { SyncCoordinatorImpl(get(), get(), get(), get()) }
    single { QueuedWriteDispatcher(get(), get(), get()) }

    single<ShopRepository> { ShopRepositoryImpl(get(), get()) }
    single<AppointmentDataRepository> { AppointmentDataRepositoryImpl(get(), get(), get()) }
    single<MedicationDataRepository> { MedicationDataRepositoryImpl(get(), get(), get()) }
    single<RoutineDataRepository> { RoutineDataRepositoryImpl(get(), get(), get()) }
    single<MedicationLogDataRepository> { MedicationLogDataRepositoryImpl(get(), get(), get(), get(), get()) }
    single<MoodDataRepository> { MoodDataRepositoryImpl(get(), get(), get()) }
    single<CalendarDataRepository> { CalendarDataRepositoryImpl(get(), get(), get(), get(), get()) }
    single<LabResultsRepository> { LabResultsRepositoryImpl() }
    single<AuthRepository> { AuthRepositoryImpl(get(), get(), get(), get()) }
    single<ClinicRepository> { ClinicRepositoryImpl() }
    single<EducationRepository> { EducationRepositoryImpl() }
    single<ReminderRepository> { ReminderRepositoryImpl(get()) }

    singleOf(::GetProductsUseCase)
    singleOf(::LoginUseCase)
    singleOf(::RegisterUseCase)
    singleOf(::ActivateAccountUseCase)
    singleOf(::CompleteProfileUseCase)
    singleOf(::ForgotPasswordUseCase)
    singleOf(::PreregisterUseCase)
    singleOf(::ResendVerificationEmailUseCase)
    singleOf(::ResetPasswordUseCase)
    singleOf(::GetRecentLabResultsUseCase)
}
