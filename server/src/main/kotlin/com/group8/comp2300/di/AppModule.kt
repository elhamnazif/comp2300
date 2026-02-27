package com.group8.comp2300.di

import com.group8.comp2300.config.JwtConfig
import com.group8.comp2300.data.repository.AppointmentRepositoryImpl
import com.group8.comp2300.data.repository.ProductRepositoryImpl
import com.group8.comp2300.data.repository.RefreshTokenRepositoryImpl
import com.group8.comp2300.data.repository.SlotRepositoryImpl
import com.group8.comp2300.data.repository.UserRepositoryImpl
import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.domain.repository.AppointmentRepository
import com.group8.comp2300.domain.repository.ProductRepository
import com.group8.comp2300.domain.repository.RefreshTokenRepository
import com.group8.comp2300.domain.repository.SlotRepository
import com.group8.comp2300.domain.repository.UserRepository
import com.group8.comp2300.infrastructure.database.createServerDatabase
import com.group8.comp2300.security.JwtService
import com.group8.comp2300.security.JwtServiceImpl
import com.group8.comp2300.service.appointment.AppointmentService
import com.group8.comp2300.service.auth.AuthService
import com.group8.comp2300.service.payment.PaymentService
import com.group8.comp2300.service.payment.PaymentServiceImpl
import org.koin.dsl.module

val serverModule = module {
    single<ServerDatabase> { createServerDatabase() }

    // Security
    single<JwtService> {
        JwtServiceImpl(
            secret = JwtConfig.secret,
            issuer = JwtConfig.issuer,
            audience = JwtConfig.audience,
        )
    }

    // Data layer - Repositories
    single<UserRepository> { UserRepositoryImpl(get()) }
    single<ProductRepository> { ProductRepositoryImpl(get()) }
    single<RefreshTokenRepository> {
        RefreshTokenRepositoryImpl(database = get(), refreshTokenExpiration = get<JwtService>().refreshTokenExpiration)
    }
    single<AppointmentRepository> { AppointmentRepositoryImpl(get()) }
    single<SlotRepository> { SlotRepositoryImpl(get()) }

    // Services
    single { AuthService(get(), get(), get()) }
    single<PaymentService> { PaymentServiceImpl() }
    single { AppointmentService(get(), get(), get()) }
}
