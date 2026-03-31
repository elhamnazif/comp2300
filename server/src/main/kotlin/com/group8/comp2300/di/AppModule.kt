package com.group8.comp2300.di

import com.group8.comp2300.config.DevSeeder
import com.group8.comp2300.config.JwtConfig
import com.group8.comp2300.config.ResendConfig
import com.group8.comp2300.data.repository.AppointmentRepositoryImpl
import com.group8.comp2300.data.repository.AppointmentSlotRepositoryImpl
import com.group8.comp2300.data.repository.ClinicRepositoryImpl
import com.group8.comp2300.data.repository.ClinicTagRepositoryImpl
import com.group8.comp2300.data.repository.MedicationLogRepositoryImpl
import com.group8.comp2300.data.repository.MedicationRepositoryImpl
import com.group8.comp2300.data.repository.MoodRepositoryImpl
import com.group8.comp2300.data.repository.PasswordResetTokenRepositoryImpl
import com.group8.comp2300.data.repository.ProductRepositoryImpl
import com.group8.comp2300.data.repository.RefreshTokenRepositoryImpl
import com.group8.comp2300.data.repository.RoutineRepositoryImpl
import com.group8.comp2300.data.repository.RoutineOccurrenceOverrideRepositoryImpl
import com.group8.comp2300.data.repository.UserRepositoryImpl
import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.domain.repository.AppointmentRepository
import com.group8.comp2300.domain.repository.AppointmentSlotRepository
import com.group8.comp2300.domain.repository.ClinicRepository
import com.group8.comp2300.domain.repository.ClinicTagRepository
import com.group8.comp2300.domain.repository.MedicationLogRepository
import com.group8.comp2300.domain.repository.MedicationRepository
import com.group8.comp2300.domain.repository.MoodRepository
import com.group8.comp2300.domain.repository.PasswordResetTokenRepository
import com.group8.comp2300.domain.repository.ProductRepository
import com.group8.comp2300.domain.repository.RefreshTokenRepository
import com.group8.comp2300.domain.repository.RoutineRepository
import com.group8.comp2300.domain.repository.RoutineOccurrenceOverrideRepository
import com.group8.comp2300.domain.repository.UserRepository
import com.group8.comp2300.infrastructure.database.createServerDatabase
import com.group8.comp2300.security.JwtService
import com.group8.comp2300.security.JwtServiceImpl
import com.group8.comp2300.service.appointment.AppointmentService
import com.group8.comp2300.service.auth.AuthService
import com.group8.comp2300.service.email.EmailService
import com.group8.comp2300.service.payment.PaymentService
import com.group8.comp2300.service.payment.PaymentServiceImpl
import org.koin.dsl.module

val serverModule = module {
    single<ServerDatabase> {
        createServerDatabase().also { DevSeeder.seedIfDevBypassEnabled(it) }
    }

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
    single<PasswordResetTokenRepository> { PasswordResetTokenRepositoryImpl(get()) }
    single<AppointmentRepository> { AppointmentRepositoryImpl(get()) }
    single<AppointmentSlotRepository> { AppointmentSlotRepositoryImpl(get()) }
    single<ClinicTagRepository> { ClinicTagRepositoryImpl(get()) }
    single<ClinicRepository> { ClinicRepositoryImpl(get(), get()) }
    single<MedicationRepository> { MedicationRepositoryImpl(get()) }
    single<RoutineRepository> { RoutineRepositoryImpl(get()) }
    single<RoutineOccurrenceOverrideRepository> { RoutineOccurrenceOverrideRepositoryImpl(get()) }
    single<MedicationLogRepository> { MedicationLogRepositoryImpl(get()) }
    single<MoodRepository> { MoodRepositoryImpl(get()) }

    // Email
    single { EmailService(ResendConfig.apiKey, ResendConfig.fromEmail, ResendConfig.appName) }

    // Services
    single {
        AuthService(
            userRepository = get(),
            refreshTokenRepository = get(),
            passwordResetTokenRepository = get(),
            jwtService = get(),
            emailService = if (ResendConfig.isConfigured) get<EmailService>() else null,
        )
    }
    single<PaymentService> { PaymentServiceImpl() }
    single { AppointmentService(get(), get(), get()) }
}
