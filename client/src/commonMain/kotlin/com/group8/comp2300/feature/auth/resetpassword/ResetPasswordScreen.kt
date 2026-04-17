package com.group8.comp2300.feature.auth.resetpassword

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.group8.comp2300.feature.auth.components.AuthBanner
import com.group8.comp2300.feature.auth.components.AuthFormScaffold
import com.group8.comp2300.feature.auth.components.AuthHeroSection
import com.group8.comp2300.feature.auth.components.AuthLoadingButton
import com.group8.comp2300.feature.auth.components.AuthPasswordField
import com.group8.comp2300.feature.auth.components.AuthPasswordFieldStyle
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.PasswordW400Outlinedfill1
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ResetPasswordScreen(
    token: String,
    onPasswordReset: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ResetPasswordViewModel = koinViewModel<ResetPasswordViewModel> {
        parametersOf(token)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val authError = state.errorMessageRes?.let { stringResource(it) } ?: state.errorMessage

    AuthFormScaffold(
        onBack = onBack,
        modifier = modifier,
    ) {
        AuthHeroSection(
            icon = Icons.PasswordW400Outlinedfill1,
            title = stringResource(Res.string.reset_password_title),
            description = stringResource(Res.string.reset_password_desc),
        )

        Spacer(Modifier.height(24.dp))

        AuthBanner(message = authError)

        AuthBanner(
            message = if (state.isPasswordReset) {
                stringResource(Res.string.reset_password_success)
            } else {
                null
            },
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
        )

        AuthPasswordField(
            value = state.newPassword,
            onValueChange = { viewModel.onEvent(ResetPasswordViewModel.Event.PasswordChanged(it)) },
            label = stringResource(Res.string.reset_password_new_label),
            isPasswordVisible = state.isPasswordVisible,
            onTogglePasswordVisibility = {
                viewModel.onEvent(ResetPasswordViewModel.Event.TogglePasswordVisibility)
            },
            passwordToggleDescription = stringResource(Res.string.auth_toggle_password_desc),
            style = AuthPasswordFieldStyle.Outlined,
            errorMessage = state.passwordError,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next,
            ),
        )

        Spacer(Modifier.height(16.dp))

        AuthPasswordField(
            value = state.confirmPassword,
            onValueChange = {
                viewModel.onEvent(ResetPasswordViewModel.Event.ConfirmPasswordChanged(it))
            },
            label = stringResource(Res.string.reset_password_confirm_label),
            isPasswordVisible = state.isPasswordVisible,
            onTogglePasswordVisibility = {
                viewModel.onEvent(ResetPasswordViewModel.Event.TogglePasswordVisibility)
            },
            passwordToggleDescription = stringResource(Res.string.auth_toggle_password_desc),
            style = AuthPasswordFieldStyle.Outlined,
            errorMessage = state.confirmPasswordError,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = {
                if (state.isFormValid) {
                    viewModel.onEvent(ResetPasswordViewModel.Event.Submit)
                }
            }),
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(Res.string.reset_password_requirements),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(24.dp))

        AuthLoadingButton(
            text = stringResource(Res.string.reset_password_submit),
            onClick = { viewModel.onEvent(ResetPasswordViewModel.Event.Submit) },
            enabled = !state.isLoading && state.isFormValid && !state.isPasswordReset,
            isLoading = state.isLoading,
        )

        Spacer(Modifier.height(16.dp))

        AnimatedVisibility(
            visible = state.isPasswordReset,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            AuthLoadingButton(
                text = stringResource(Res.string.forgot_password_back_to_login),
                onClick = onPasswordReset,
                enabled = true,
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            )
        }
    }
}
