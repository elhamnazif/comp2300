package com.group8.comp2300.di

import android.content.Context
import com.group8.comp2300.data.database.DatabaseDriverFactory
import com.group8.comp2300.data.notifications.AndroidRoutineNotificationPlatform
import com.group8.comp2300.data.notifications.RoutineNotificationPlatform
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { DatabaseDriverFactory(get()) }
    single<RoutineNotificationPlatform> { AndroidRoutineNotificationPlatform(get<Context>()) }
}
