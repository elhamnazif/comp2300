package com.group8.comp2300.di

import com.group8.comp2300.data.database.DatabaseDriverFactory
import com.group8.comp2300.data.notifications.JvmRoutineNotificationPlatform
import com.group8.comp2300.data.notifications.RoutineNotificationPlatform
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

actual val platformModule: Module = module {
    singleOf(::DatabaseDriverFactory)
    single<RoutineNotificationPlatform> { JvmRoutineNotificationPlatform() }
}
