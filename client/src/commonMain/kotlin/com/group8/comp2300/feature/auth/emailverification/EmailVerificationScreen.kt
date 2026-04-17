package com.group8.comp2300.feature.auth.emailverification

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.group8.comp2300.feature.auth.components.AuthBanner
import com.group8.comp2300.feature.auth.components.AuthFormScaffold
import com.group8.comp2300.feature.auth.components.AuthHeroSection
import com.group8.comp2300.feature.auth.components.AuthLoadingButton
import com.group8.comp2300.feature.auth.components.VerificationCodeField
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.MailOutlineW400Outlined
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun EmailVerificationScreen(
    email: String,
    onVerified: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EmailVerificationViewModel = koinViewModel<EmailVerificationViewModel> {
        parametersOf(email)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val authError = state.errorMessageRes?.let { stringResource(it) } ?: state.errorMessage

    // Navigate when verification succeeds
    LaunchedEffect(state.isVerified) {
        if (state.isVerified) {
            onVerified()
        }
    }

    AuthFormScaffold(
        onBack = onBack,
        modifier = modifier,
    ) {
        AuthHeroSection(
            icon = Icons.MailOutlineW400Outlined,
            title = stringResource(Res.string.email_verification_title),
            description = stringResource(Res.string.email_verification_desc),
            emphasisText = email,
            supportingText = stringResource(Res.string.email_verification_check_inbox),
        )

        Spacer(Modifier.height(24.dp))

        AuthBanner(message = authError)

        VerificationCodeField(
            value = state.token,
            onValueChange = {
                viewModel.onEvent(EmailVerificationViewModel.Event.TokenChanged(it))
            },
            label = stringResource(Res.string.email_verification_token_label),
            placeholder = stringResource(Res.string.email_verification_token_placeholder),
        )

        Spacer(Modifier.height(16.dp))

        AuthLoadingButton(
            text = stringResource(Res.string.email_verification_verify_button),
            onClick = { viewModel.onEvent(EmailVerificationViewModel.Event.VerifyToken) },
            enabled = !state.isLoading && state.token.isNotBlank(),
            isLoading = state.isLoading,
        )

        Spacer(Modifier.height(16.dp))

        if (state.canResend) {
            TextButton(
                onClick = { viewModel.onEvent(EmailVerificationViewModel.Event.ResendEmail) },
                enabled = !state.isLoading,
            ) {
                Text(stringResource(Res.string.email_verification_resend))
            }
        } else {
            Text(
                text = stringResource(Res.string.email_verification_resend_in, state.resendCooldown),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}
