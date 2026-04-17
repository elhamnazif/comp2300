package com.group8.comp2300.feature.auth.forgotpassword

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.group8.comp2300.feature.auth.components.AuthBanner
import com.group8.comp2300.feature.auth.components.AuthFormScaffold
import com.group8.comp2300.feature.auth.components.AuthHeroSection
import com.group8.comp2300.feature.auth.components.AuthLoadingButton
import com.group8.comp2300.feature.auth.components.AuthTextField
import com.group8.comp2300.feature.auth.components.VerificationCodeField
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.MailOutlineW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.MarkEmailReadW400Outlinedfill1
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    onCodeEntered: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ForgotPasswordViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val authError = state.errorMessageRes?.let { stringResource(it) } ?: state.errorMessage

    AuthFormScaffold(
        onBack = onBack,
        modifier = modifier,
        bannerContent = {
            AuthBanner(message = authError)
        },
    ) {
        AnimatedContent(
            targetState = state.emailSent,
            label = "ForgotPasswordContent",
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
        ) { emailSent ->
            if (emailSent) {
                SuccessContent(
                    email = state.email,
                    code = state.code,
                    isCodeValid = state.isCodeValid,
                    onCodeChanged = { viewModel.onEvent(ForgotPasswordViewModel.Event.CodeChanged(it)) },
                    onContinue = { onCodeEntered(state.code) },
                )
            } else {
                FormContent(
                    state = state,
                    onEvent = viewModel::onEvent,
                    onPrepareSubmit = {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                    },
                )
            }
        }
    }
}

@Composable
private fun SuccessContent(
    email: String,
    code: String,
    isCodeValid: Boolean,
    onCodeChanged: (String) -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AuthHeroSection(
            icon = Icons.MarkEmailReadW400Outlinedfill1,
            title = stringResource(Res.string.forgot_password_success_title),
            description = stringResource(Res.string.forgot_password_success_desc),
            emphasisText = email,
        )

        Spacer(Modifier.height(24.dp))

        VerificationCodeField(
            value = code,
            onValueChange = onCodeChanged,
            label = stringResource(Res.string.email_verification_token_label),
            placeholder = stringResource(Res.string.email_verification_token_placeholder),
        )

        Spacer(Modifier.height(16.dp))

        AuthLoadingButton(
            text = stringResource(Res.string.auth_continue),
            onClick = onContinue,
            enabled = isCodeValid,
        )
    }
}

@Composable
private fun FormContent(
    state: ForgotPasswordViewModel.State,
    onEvent: (ForgotPasswordViewModel.Event) -> Unit,
    onPrepareSubmit: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AuthHeroSection(
            icon = Icons.MailOutlineW400Outlinedfill1,
            title = stringResource(Res.string.forgot_password_title),
            description = stringResource(Res.string.forgot_password_desc),
        )

        Spacer(Modifier.height(24.dp))

        AuthTextField(
            value = state.email,
            onValueChange = { onEvent(ForgotPasswordViewModel.Event.EmailChanged(it)) },
            label = stringResource(Res.string.forgot_password_email_label),
            leadingIcon = Icons.MailOutlineW400Outlinedfill1,
            errorMessage = state.emailError,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = {
                if (state.email.isNotBlank() && state.emailError == null) {
                    onPrepareSubmit()
                    onEvent(ForgotPasswordViewModel.Event.Submit)
                }
            }),
        )

        Spacer(Modifier.height(16.dp))

        AuthLoadingButton(
            text = stringResource(Res.string.forgot_password_submit),
            onClick = {
                onPrepareSubmit()
                onEvent(ForgotPasswordViewModel.Event.Submit)
            },
            enabled = !state.isLoading && state.email.isNotBlank() && state.emailError == null,
            isLoading = state.isLoading,
        )
    }
}
