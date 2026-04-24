package com.group8.comp2300.feature.profile

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
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
import com.group8.comp2300.symbols.icons.materialsymbols.icons.DeleteW400Outlinedfill1
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DeactivateAccountScreen(
    onBack: () -> Unit,
    onAccountDeactivate: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeactivateAccountViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val authError = state.errorMessageRes?.let { stringResource(it) } ?: state.errorMessage
    val currentOnAccountDeactivate by rememberUpdatedState(onAccountDeactivate)

    LaunchedEffect(state.isComplete) {
        if (state.isComplete) {
            currentOnAccountDeactivate()
        }
    }

    AuthFormScaffold(
        onBack = onBack,
        modifier = modifier,
        bannerContent = { AuthBanner(message = authError) },
    ) {
        if (state.confirmStep) {
            AuthHeroSection(
                icon = Icons.DeleteW400Outlinedfill1,
                title = stringResource(Res.string.account_deactivate_confirm_title),
                description = stringResource(Res.string.account_deactivate_confirm_desc),
            )

            Spacer(Modifier.height(24.dp))

            AuthLoadingButton(
                text = stringResource(Res.string.account_deactivate_confirm),
                onClick = { viewModel.onEvent(DeactivateAccountViewModel.Event.ConfirmDeactivate) },
                enabled = !state.isLoading,
                isLoading = state.isLoading,
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            )

            Spacer(Modifier.height(8.dp))

            TextButton(onClick = { viewModel.onEvent(DeactivateAccountViewModel.Event.EditPassword) }) {
                Text(stringResource(Res.string.common_cancel))
            }
        } else {
            AuthHeroSection(
                icon = Icons.DeleteW400Outlinedfill1,
                title = stringResource(Res.string.account_deactivate_screen_title),
                description = stringResource(Res.string.account_deactivate_screen_desc),
            )

            Spacer(Modifier.height(24.dp))

            AuthPasswordField(
                value = state.currentPassword,
                onValueChange = { viewModel.onEvent(DeactivateAccountViewModel.Event.CurrentPasswordChanged(it)) },
                label = stringResource(Res.string.account_current_password_label),
                isPasswordVisible = state.isPasswordVisible,
                onTogglePasswordVisibility = {
                    viewModel.onEvent(DeactivateAccountViewModel.Event.TogglePasswordVisibility)
                },
                passwordToggleDescription = stringResource(Res.string.auth_toggle_password_desc),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (state.currentPassword.isNotBlank()) {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                        viewModel.onEvent(DeactivateAccountViewModel.Event.Continue)
                    }
                }),
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(Res.string.account_deactivate_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )

            Spacer(Modifier.height(24.dp))

            AuthLoadingButton(
                text = stringResource(Res.string.account_deactivate_continue),
                onClick = {
                    focusManager.clearFocus(force = true)
                    keyboardController?.hide()
                    viewModel.onEvent(DeactivateAccountViewModel.Event.Continue)
                },
                enabled = state.currentPassword.isNotBlank(),
                isLoading = false,
            )
        }
    }
}
