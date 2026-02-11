package com.group8.comp2300.di

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
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val coreModule = module {
    single { createHttpClient() }
    singleOf(::ApiServiceImpl) { bind<ApiService>() }

    single { createDatabase(get<DatabaseDriverFactory>()) }

    singleOf(::ShopRepositoryImpl) { bind<ShopRepository>() }
    singleOf(::AuthRepositoryImpl) { bind<AuthRepository>() }
    single<ClinicRepository> { ClinicRepositoryImpl() }
    single<EducationRepository> { EducationRepositoryImpl() }
    singleOf(::MedicalRepositoryImpl) { bind<MedicalRepository>() }
    single<ReminderRepository> { ReminderRepositoryImpl(get()) }

    singleOf(::GetProductsUseCase)
    singleOf(::LoginUseCase)
    singleOf(::RegisterUseCase)
    singleOf(::GetRecentLabResultsUseCase)
}
