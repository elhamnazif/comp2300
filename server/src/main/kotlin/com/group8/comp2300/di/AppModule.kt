package com.group8.comp2300.di

import com.group8.comp2300.data.repository.ProductRepository
import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.database.createServerDatabase
import org.koin.dsl.module

val serverModule = module {
    single<ServerDatabase> { createServerDatabase() }
    single { ProductRepository(get()) }
}
