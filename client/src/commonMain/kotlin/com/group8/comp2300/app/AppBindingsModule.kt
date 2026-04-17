package com.group8.comp2300.app

import com.group8.comp2300.app.navigation.Navigator
import com.group8.comp2300.app.navigation.RealNavigator
import com.group8.comp2300.app.navigation.Screen
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appBindingsModule = module {
    viewModel<Navigator> { RealNavigator(get(), Screen.Onboarding) }
}
