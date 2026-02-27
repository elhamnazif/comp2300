package com.group8.comp2300.di

import org.koin.dsl.module

val navigationModule = module {
    includes(
        authNavigationModule,
        mainNavigationModule,
        shopNavigationModule,
        medicalNavigationModule,
        educationNavigationModule,
        secondaryNavigationModule,
    )
}
