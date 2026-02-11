package com.group8.comp2300.di

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
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Koin module for UI-only dependencies.
val appModule = module {
    viewModel<Navigator> { RealNavigator(get(), Screen.Onboarding) }

    // ViewModels
    viewModelOf(::RealAuthViewModel) { bind<AuthViewModel>() }
    viewModelOf(::ShopViewModel)
    viewModelOf(::BookingViewModel)
    viewModelOf(::EducationViewModel)
    viewModelOf(::ProfileViewModel)
}
