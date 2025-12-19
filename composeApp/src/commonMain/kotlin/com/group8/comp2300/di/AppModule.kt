package com.group8.comp2300.di

import com.group8.comp2300.data.repository.AuthRepositoryImpl
import com.group8.comp2300.data.repository.ClinicRepositoryImpl
import com.group8.comp2300.data.repository.EducationRepositoryImpl
import com.group8.comp2300.data.repository.ShopRepositoryImpl
import com.group8.comp2300.domain.model.Screen
import com.group8.comp2300.domain.repository.AuthRepository
import com.group8.comp2300.domain.repository.ClinicRepository
import com.group8.comp2300.domain.repository.EducationRepository
import com.group8.comp2300.domain.repository.ShopRepository
import com.group8.comp2300.navigation.Navigator
import com.group8.comp2300.navigation.RealNavigator
import com.group8.comp2300.presentation.ui.screens.auth.AuthViewModel
import com.group8.comp2300.presentation.ui.screens.auth.RealAuthViewModel
import com.group8.comp2300.presentation.ui.screens.education.EducationViewModel
import com.group8.comp2300.presentation.ui.screens.medical.BookingViewModel
import com.group8.comp2300.presentation.ui.screens.shop.ShopViewModel
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Koin module for common dependencies
val appModule = module {
    viewModel<Navigator> { RealNavigator(get(), Screen.Onboarding) }

    // Repositories
    single<ShopRepository> { ShopRepositoryImpl() }
    single<AuthRepository> { AuthRepositoryImpl() }
    single<ClinicRepository> { ClinicRepositoryImpl() }
    single<EducationRepository> { EducationRepositoryImpl() }

    // ViewModels
    viewModelOf(::RealAuthViewModel) { bind<AuthViewModel>() }
    viewModelOf(::ShopViewModel)
    viewModelOf(::BookingViewModel)
    viewModelOf(::EducationViewModel)
}
