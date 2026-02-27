package com.group8.comp2300.di

import com.group8.comp2300.data.auth.TokenManager
import com.group8.comp2300.data.auth.TokenManagerImpl
import com.group8.comp2300.data.database.DatabaseDriverFactory
import com.group8.comp2300.data.database.createDatabase
import com.group8.comp2300.data.local.SessionDataSource
import com.group8.comp2300.data.remote.ApiService
import com.group8.comp2300.data.remote.ApiServiceImpl
import com.group8.comp2300.data.remote.TokenProvider
import com.group8.comp2300.data.remote.createHttpClient
import com.group8.comp2300.data.remote.tokenProviderDelegate
import com.group8.comp2300.data.repository.AuthRepositoryImpl
import com.group8.comp2300.data.repository.ClinicRepositoryImpl
import com.group8.comp2300.data.repository.EducationRepositoryImpl
import com.group8.comp2300.data.repository.MedicalRepositoryImpl
import com.group8.comp2300.data.repository.ReminderRepositoryImpl
import com.group8.comp2300.data.repository.ShopRepositoryImpl
import com.group8.comp2300.domain.repository.AuthRepository
import com.group8.comp2300.domain.repository.ClinicRepository
import com.group8.comp2300.domain.repository.EducationRepository
import com.group8.comp2300.domain.repository.MedicalRepository
import com.group8.comp2300.domain.repository.ReminderRepository
import com.group8.comp2300.domain.repository.ShopRepository
import com.group8.comp2300.domain.usecase.auth.ActivateAccountUseCase
import com.group8.comp2300.domain.usecase.auth.CompleteProfileUseCase
import com.group8.comp2300.domain.usecase.auth.ForgotPasswordUseCase
import com.group8.comp2300.domain.usecase.auth.LoginUseCase
import com.group8.comp2300.domain.usecase.auth.PreregisterUseCase
import com.group8.comp2300.domain.usecase.auth.RegisterUseCase
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

    singleOf(::ShopRepositoryImpl) { bind<ShopRepository>() }
    singleOf(::MedicalRepositoryImpl) { bind<MedicalRepository>() }
    single<AuthRepository> {
        AuthRepositoryImpl(
            get(),
            get(),
            co.touchlab.kermit.Logger.withTag("AuthRepository"),
        )
    }
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
    singleOf(::ResetPasswordUseCase)
    singleOf(::GetRecentLabResultsUseCase)
}
