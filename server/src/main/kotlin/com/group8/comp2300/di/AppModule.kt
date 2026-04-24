package com.group8.comp2300.di

import com.group8.comp2300.config.*
import com.group8.comp2300.data.repository.*
import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.domain.repository.*
import com.group8.comp2300.infrastructure.database.createServerDatabase
import com.group8.comp2300.mapper.ArticleMapper
import com.group8.comp2300.mapper.CategoryMapper
import com.group8.comp2300.security.*
import com.group8.comp2300.service.appointment.AppointmentService
import com.group8.comp2300.service.appointment.MockClinicOperationsService
import com.group8.comp2300.service.auth.AuthService
import com.group8.comp2300.service.auth.InMemoryVerificationRequestThrottle
import com.group8.comp2300.service.auth.ProfileImageStorage
import com.group8.comp2300.service.auth.VerificationRequestThrottle
import com.group8.comp2300.service.chatbot.ChatbotProviderClient
import com.group8.comp2300.service.chatbot.ChatbotService
import com.group8.comp2300.service.chatbot.GoogleChatbotProviderClient
import com.group8.comp2300.service.content.*
import com.group8.comp2300.service.email.EmailService
import com.group8.comp2300.service.medicalRecords.MedicalRecordService
import com.group8.comp2300.service.medication.MedicationService
import com.group8.comp2300.service.order.OrderService
import org.koin.dsl.module

val serverModule = module {
    single<ServerDatabase> {
        createServerDatabase().also {
            DevSeeder.seedIfDevBypassEnabled(it)
            CareCatalogSeeder.seedIfEmpty(it)
        }
    }

    // Mappers
    single { CategoryMapper(categoryRepository = get()) }
    single { ArticleMapper(categoryMapper = get()) }

    // Security
    single<JwtService> {
        JwtServiceImpl(
            secret = JwtConfig.secret,
            issuer = JwtConfig.issuer,
            audience = JwtConfig.audience,
        )
    }
    single<MedicalRecordCipher> { AesGcmMedicalRecordCipher(MedicalRecordEncryptionConfig.keyBytes) }

    // Data layer - Repositories
    single<UserRepository> { UserRepositoryImpl(get()) }
    single<ProductRepository> { ProductRepositoryImpl(get()) }
    single<OrderRepository> { OrderRepositoryImpl(get()) }
    single<RefreshTokenRepository> {
        RefreshTokenRepositoryImpl(database = get(), refreshTokenExpiration = get<JwtService>().refreshTokenExpiration)
    }
    single<PasswordResetTokenRepository> { PasswordResetTokenRepositoryImpl(get()) }
    single<EmailChangeTokenRepository> { EmailChangeTokenRepositoryImpl(get()) }
    single<AppointmentRepository> { AppointmentRepositoryImpl(get()) }
    single<AppointmentSlotRepository> { AppointmentSlotRepositoryImpl(get()) }
    single<ArticleRepository> { ArticleRepositoryImpl(get()) }
    single<BadgeRepository> { BadgeRepositoryImpl(get()) }
    single<ClinicTagRepository> { ClinicTagRepositoryImpl(get()) }
    single<ClinicRepository> { ClinicRepositoryImpl(get(), get(), get()) }
    single<ContentCategoryRepository> { ContentCategoryRepositoryImpl(get()) }
    single<MedicationRepository> { MedicationRepositoryImpl(get()) }
    single<RoutineRepository> { RoutineRepositoryImpl(get()) }
    single<RoutineOccurrenceOverrideRepository> { RoutineOccurrenceOverrideRepositoryImpl(get()) }
    single<QuizRepository> { QuizRepositoryImpl(get()) }
    single<MedicationLogRepository> { MedicationLogRepositoryImpl(get()) }
    single<MoodRepository> { MoodRepositoryImpl(get()) }
    single<MedicalRecordRepository> { MedicalRecordRepositoryImpl(get()) }
    single<UserBadgeRepository> { UserBadgeRepositoryImpl(get()) }
    single<UserQuizRepository> { UserQuizRepositoryImpl(get()) }

    // Email
    single { EmailService(ResendConfig.apiKey, ResendConfig.fromEmail, ResendConfig.appName) }
    single { ChatbotConfig }
    single<ChatbotProviderClient> { GoogleChatbotProviderClient(get()) }
    single<VerificationRequestThrottle> { InMemoryVerificationRequestThrottle() }
    single { ProfileImageStorage() }

    // Services
    single {
        AuthService(
            userRepository = get(),
            refreshTokenRepository = get(),
            passwordResetTokenRepository = get(),
            emailChangeTokenRepository = get(),
            jwtService = get(),
            emailService = if (ResendConfig.isConfigured) get<EmailService>() else null,
            verificationRequestThrottle = get(),
            profileImageStorage = get(),
        )
    }
    single { MedicalRecordService(repository = get(), medicalRecordCipher = get()) }
    single {
        MockClinicOperationsService(
            appointmentRepository = get(),
            appointmentSlotRepository = get(),
            clinicRepository = get(),
        )
    }
    single {
        AppointmentService(
            appointmentRepository = get(),
            appointmentSlotRepository = get(),
            clinicRepository = get(),
            database = get(),
            mockClinicOperationsService = get(),
        )
    }
    single { OrderService(get(), get()) }
    single {
        MedicationService(
            medicationRepository = get(),
            routineRepository = get(),
            routineOccurrenceOverrideRepository = get(),
            medicationLogRepository = get(),
        )
    }
    single {
        ContentCategoryService(
            categoryRepository = get(),
            articleRepository = get(),
            categoryMapper = get(),
            articleMapper = get(),
        )
    }
    single {
        ArticleService(
            articleRepository = get(),
            quizRepository = get(),
            articleMapper = get(),
        )
    }
    single { ChatbotService(providerClient = get(), articleRepository = get()) }
    single { QuizService(get()) }
    single { UserBadgeService(badgeRepo = get(), userBadgeRepo = get(), quizRepo = get()) }
    single { UserQuizService(userQuizRepository = get(), badgeService = get(), quizRepository = get()) }
}
