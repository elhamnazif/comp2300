package com.group8.comp2300.di

import com.group8.comp2300.config.JwtConfig
import com.group8.comp2300.data.repository.ProductRepository
import com.group8.comp2300.data.repository.RefreshTokenRepository
import com.group8.comp2300.data.repository.UserRepository
import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.database.createServerDatabase
import com.group8.comp2300.security.JwtService
import com.group8.comp2300.security.JwtServiceImpl
import com.group8.comp2300.service.AuthService
import org.koin.dsl.module

val serverModule = module {
    single<ServerDatabase> { createServerDatabase() }

    // Security
    single<JwtService> {
        JwtServiceImpl(
            secret = JwtConfig.secret,
            issuer = JwtConfig.issuer,
            audience = JwtConfig.audience
        )
    }

    // Data layer
    single { ProductRepository(get()) }
    single { UserRepository(get()) }
    single {
        RefreshTokenRepository(database = get(), refreshTokenExpiration = get<JwtService>().refreshTokenExpiration)
    }

    // Services
    single { AuthService(get(), get(), get()) }
}
