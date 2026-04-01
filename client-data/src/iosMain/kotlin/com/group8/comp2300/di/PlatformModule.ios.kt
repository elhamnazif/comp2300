package com.group8.comp2300.di

import com.group8.comp2300.data.database.DatabaseDriverFactory
import com.group8.comp2300.data.notifications.IosRoutineNotificationService
import com.group8.comp2300.data.notifications.RoutineNotificationService
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

actual val platformModule: Module = module {
    singleOf(::DatabaseDriverFactory)
    single<RoutineNotificationService> { IosRoutineNotificationService() }
}
