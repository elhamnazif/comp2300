package com.group8.comp2300.feature.auth

import com.group8.comp2300.feature.auth.completeprofile.CompleteProfileViewModel
import com.group8.comp2300.feature.auth.emailverification.EmailVerificationViewModel
import com.group8.comp2300.feature.auth.forgotpassword.ForgotPasswordViewModel
import com.group8.comp2300.feature.auth.login.AuthViewModel
import com.group8.comp2300.feature.auth.resetpassword.ResetPasswordViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val authModule = module {
    viewModel {
        AuthViewModel(
            loginUseCase = get(),
            preregisterUseCase = get(),
            authRepository = get(),
        )
    }
    viewModel { params ->
        EmailVerificationViewModel(
            activateAccountUseCase = get(),
            resendVerificationEmailUseCase = get(),
            initialEmail = params.get(),
        )
    }
    viewModel { params ->
        CompleteProfileViewModel(
            completeProfileUseCase = get(),
            initialEmail = params.get(),
        )
    }
    viewModelOf(::ForgotPasswordViewModel)
    viewModel { params ->
        ResetPasswordViewModel(
            resetPasswordUseCase = get(),
            initialToken = params.get(),
        )
    }
}
