package com.group8.comp2300.feature.profile

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
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
import com.group8.comp2300.feature.auth.components.AuthPasswordField
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.PasswordW400Outlinedfill1
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ChangePasswordScreen(
    onBack: () -> Unit,
    onPasswordChange: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChangePasswordViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val authError = state.errorMessageRes?.let { stringResource(it) } ?: state.errorMessage
    val currentOnPasswordChange by rememberUpdatedState(onPasswordChange)

    LaunchedEffect(state.isPasswordChanged) {
        if (state.isPasswordChanged) {
            currentOnPasswordChange()
        }
    }

    AuthFormScaffold(
        onBack = onBack,
        modifier = modifier,
        bannerContent = { AuthBanner(message = authError) },
    ) {
        AuthHeroSection(
            icon = Icons.PasswordW400Outlinedfill1,
            title = stringResource(Res.string.account_change_password_screen_title),
            description = stringResource(Res.string.account_change_password_screen_desc),
        )

        Spacer(Modifier.height(24.dp))

        AuthPasswordField(
            value = state.currentPassword,
            onValueChange = { viewModel.onEvent(ChangePasswordViewModel.Event.CurrentPasswordChanged(it)) },
            label = stringResource(Res.string.account_current_password_label),
            isPasswordVisible = state.isPasswordVisible,
            onTogglePasswordVisibility = {
                viewModel.onEvent(ChangePasswordViewModel.Event.TogglePasswordVisibility)
            },
            passwordToggleDescription = stringResource(Res.string.auth_toggle_password_desc),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
        )

        Spacer(Modifier.height(16.dp))

        AuthPasswordField(
            value = state.newPassword,
            onValueChange = { viewModel.onEvent(ChangePasswordViewModel.Event.NewPasswordChanged(it)) },
            label = stringResource(Res.string.reset_password_new_label),
            errorMessage = state.newPasswordError,
            isPasswordVisible = state.isPasswordVisible,
            onTogglePasswordVisibility = {
                viewModel.onEvent(ChangePasswordViewModel.Event.TogglePasswordVisibility)
            },
            passwordToggleDescription = stringResource(Res.string.auth_toggle_password_desc),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
        )

        Spacer(Modifier.height(16.dp))

        AuthPasswordField(
            value = state.confirmPassword,
            onValueChange = { viewModel.onEvent(ChangePasswordViewModel.Event.ConfirmPasswordChanged(it)) },
            label = stringResource(Res.string.reset_password_confirm_label),
            errorMessage = state.confirmPasswordError,
            isPasswordVisible = state.isPasswordVisible,
            onTogglePasswordVisibility = {
                viewModel.onEvent(ChangePasswordViewModel.Event.TogglePasswordVisibility)
            },
            passwordToggleDescription = stringResource(Res.string.auth_toggle_password_desc),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if (state.isFormValid) {
                    focusManager.clearFocus(force = true)
                    keyboardController?.hide()
                    viewModel.onEvent(ChangePasswordViewModel.Event.Submit)
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
            text = stringResource(Res.string.account_change_password_submit),
            onClick = {
                focusManager.clearFocus(force = true)
                keyboardController?.hide()
                viewModel.onEvent(ChangePasswordViewModel.Event.Submit)
            },
            enabled = !state.isLoading && state.isFormValid,
            isLoading = state.isLoading,
        )
    }
}
