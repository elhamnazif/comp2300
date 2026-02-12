package com.group8.comp2300.di

import android.content.Context
import com.group8.comp2300.data.database.DatabaseDriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { DatabaseDriverFactory(get()) }
}
