package com.group8.comp2300.feature.profile

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.group8.comp2300.feature.auth.components.*
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.MailOutlineW400Outlinedfill1
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ChangeEmailScreen(
    currentEmail: String,
    onBack: () -> Unit,
    onEmailChange: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChangeEmailViewModel = koinViewModel<ChangeEmailViewModel> {
        parametersOf(currentEmail)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val authError = state.errorMessageRes?.let { stringResource(it) } ?: state.errorMessage
    val currentOnEmailChange by rememberUpdatedState(onEmailChange)

    LaunchedEffect(state.isComplete) {
        if (state.isComplete) {
            currentOnEmailChange()
        }
    }

    AuthFormScaffold(
        onBack = onBack,
        modifier = modifier,
        bannerContent = { AuthBanner(message = authError) },
    ) {
        if (state.requestSent) {
            AuthHeroSection(
                icon = Icons.MailOutlineW400Outlinedfill1,
                title = stringResource(Res.string.account_change_email_verify_title),
                description = stringResource(Res.string.account_change_email_verify_desc),
                emphasisText = state.requestedEmail,
                supportingText = stringResource(Res.string.account_change_email_verify_supporting),
            )

            Spacer(Modifier.height(24.dp))

            VerificationCodeField(
                value = state.code,
                onValueChange = { viewModel.onEvent(ChangeEmailViewModel.Event.CodeChanged(it)) },
                label = stringResource(Res.string.email_verification_token_label),
                placeholder = stringResource(Res.string.email_verification_token_placeholder),
            )

            Spacer(Modifier.height(16.dp))

            AuthLoadingButton(
                text = stringResource(Res.string.account_change_email_confirm),
                onClick = {
                    focusManager.clearFocus(force = true)
                    keyboardController?.hide()
                    viewModel.onEvent(ChangeEmailViewModel.Event.ConfirmCode)
                },
                enabled = !state.isLoading && state.canConfirmCode,
                isLoading = state.isLoading,
            )

            Spacer(Modifier.height(8.dp))

            TextButton(onClick = { viewModel.onEvent(ChangeEmailViewModel.Event.EditEmail) }) {
                Text(stringResource(Res.string.account_change_email_edit))
            }
        } else {
            AuthHeroSection(
                icon = Icons.MailOutlineW400Outlinedfill1,
                title = stringResource(Res.string.account_change_email_screen_title),
                description = stringResource(Res.string.account_change_email_screen_desc),
            )

            Spacer(Modifier.height(24.dp))

            AuthTextField(
                value = state.newEmail,
                onValueChange = { viewModel.onEvent(ChangeEmailViewModel.Event.NewEmailChanged(it)) },
                label = stringResource(Res.string.account_new_email_label),
                leadingIcon = Icons.MailOutlineW400Outlinedfill1,
                errorMessage = state.newEmailError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            )

            Spacer(Modifier.height(16.dp))

            AuthPasswordField(
                value = state.currentPassword,
                onValueChange = { viewModel.onEvent(ChangeEmailViewModel.Event.CurrentPasswordChanged(it)) },
                label = stringResource(Res.string.account_current_password_label),
                isPasswordVisible = state.isPasswordVisible,
                onTogglePasswordVisibility = {
                    viewModel.onEvent(ChangeEmailViewModel.Event.TogglePasswordVisibility)
                },
                passwordToggleDescription = stringResource(Res.string.auth_toggle_password_desc),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (state.canSubmitRequest) {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                        viewModel.onEvent(ChangeEmailViewModel.Event.SubmitRequest)
                    }
                }),
            )

            Spacer(Modifier.height(24.dp))

            AuthLoadingButton(
                text = stringResource(Res.string.account_change_email_submit),
                onClick = {
                    focusManager.clearFocus(force = true)
                    keyboardController?.hide()
                    viewModel.onEvent(ChangeEmailViewModel.Event.SubmitRequest)
                },
                enabled = !state.isLoading && state.canSubmitRequest,
                isLoading = state.isLoading,
            )
        }
    }
}
