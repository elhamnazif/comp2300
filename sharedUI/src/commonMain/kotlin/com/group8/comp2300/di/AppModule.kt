package com.group8.comp2300.di

import com.group8.comp2300.`data`.database.AppDatabase
import com.group8.comp2300.data.database.DatabaseDriverFactory
import com.group8.comp2300.data.database.createDatabase
import com.group8.comp2300.data.remote.ApiService
import com.group8.comp2300.data.remote.ApiServiceImpl
import com.group8.comp2300.data.remote.createHttpClient
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
import com.group8.comp2300.domain.usecase.auth.LoginUseCase
import com.group8.comp2300.domain.usecase.auth.RegisterUseCase
import com.group8.comp2300.domain.usecase.medical.GetRecentLabResultsUseCase
import com.group8.comp2300.domain.usecase.shop.GetProductsUseCase
import com.group8.comp2300.presentation.navigation.Navigator
import com.group8.comp2300.presentation.navigation.RealNavigator
import com.group8.comp2300.presentation.navigation.Screen
import com.group8.comp2300.presentation.screens.auth.AuthViewModel
import com.group8.comp2300.presentation.screens.auth.RealAuthViewModel
import com.group8.comp2300.presentation.screens.education.EducationViewModel
import com.group8.comp2300.presentation.screens.medical.BookingViewModel
import com.group8.comp2300.presentation.screens.profile.ProfileViewModel
import com.group8.comp2300.presentation.screens.shop.ShopViewModel
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Koin module for common dependencies
val appModule = module {
    viewModel<Navigator> { RealNavigator(get(), Screen.Onboarding) }

    // Network
    single { createHttpClient() }
    singleOf(::ApiServiceImpl) { bind<ApiService>() }

    // Database (platform-specific DatabaseDriverFactory is provided in platformModule)
    single { createDatabase(get<DatabaseDriverFactory>()) }

    // Repositories
    singleOf(::ShopRepositoryImpl) { bind<ShopRepository>() }
    singleOf(::AuthRepositoryImpl) { bind<AuthRepository>() }
    single<ClinicRepository> { ClinicRepositoryImpl() }
    single<EducationRepository> { EducationRepositoryImpl() }
    singleOf(::MedicalRepositoryImpl) { bind<MedicalRepository>() }
    single<ReminderRepository> { ReminderRepositoryImpl(get()) }

    // Use Cases
    singleOf(::GetProductsUseCase)
    singleOf(::LoginUseCase)
    singleOf(::RegisterUseCase)
    singleOf(::GetRecentLabResultsUseCase)

    // ViewModels
    viewModelOf(::RealAuthViewModel) { bind<com.group8.comp2300.presentation.screens.auth.AuthViewModel>() }
    viewModelOf(::ShopViewModel)
    viewModelOf(::BookingViewModel)
    viewModelOf(::EducationViewModel)
    viewModelOf(::ProfileViewModel)
}
