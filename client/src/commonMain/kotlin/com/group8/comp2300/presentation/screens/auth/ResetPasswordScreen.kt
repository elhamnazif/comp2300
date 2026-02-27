package com.group8.comp2300.presentation.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.group8.comp2300.presentation.components.AppTopBar
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.LockW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.PasswordW500Outlined
import com.group8.comp2300.symbols.icons.materialsymbols.icons.VisibilityOffW500Outlined
import com.group8.comp2300.symbols.icons.materialsymbols.icons.VisibilityW500Outlined
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
    viewModel: ResetPasswordViewModel = koinViewModel<RealResetPasswordViewModel> {
        parametersOf(token)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    val authError = state.errorMessageRes?.let { stringResource(it) } ?: state.errorMessage

    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = {},
                onBackClick = onBack,
                backContentDescription = stringResource(Res.string.auth_back_desc),
            )
        },
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(24.dp)
                .imePadding(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Card(
                shape = RoundedCornerShape(50),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                modifier = Modifier.size(80.dp),
            ) {
                Icon(
                    Icons.PasswordW500Outlined,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = stringResource(Res.string.reset_password_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(Res.string.reset_password_desc),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(24.dp))

            // Error banner
            AnimatedVisibility(
                visible = authError != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                if (authError != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    ) {
                        Text(
                            text = authError,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                }
            }

            // Success message
            AnimatedVisibility(
                visible = state.isPasswordReset,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.reset_password_success),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }

            // New Password
            OutlinedTextField(
                value = state.newPassword,
                onValueChange = { viewModel.onEvent(ResetPasswordViewModel.Event.PasswordChanged(it)) },
                label = { Text(stringResource(Res.string.reset_password_new_label)) },
                leadingIcon = {
                    Icon(Icons.LockW400Outlinedfill1, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(onClick = {
                        viewModel.onEvent(ResetPasswordViewModel.Event.TogglePasswordVisibility)
                    }) {
                        val icon = if (state.isPasswordVisible) {
                            Icons.VisibilityW500Outlined
                        } else {
                            Icons.VisibilityOffW500Outlined
                        }
                        Icon(icon, contentDescription = stringResource(Res.string.auth_toggle_password_desc))
                    }
                },
                visualTransformation = if (state.isPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                isError = state.passwordError != null,
                supportingText = state.passwordError?.let {
                    { Text(stringResource(it)) }
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                ),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
            )

            Spacer(Modifier.height(16.dp))

            // Confirm Password
            OutlinedTextField(
                value = state.confirmPassword,
                onValueChange = {
                    viewModel.onEvent(ResetPasswordViewModel.Event.ConfirmPasswordChanged(it))
                },
                label = { Text(stringResource(Res.string.reset_password_confirm_label)) },
                leadingIcon = {
                    Icon(Icons.LockW400Outlinedfill1, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(onClick = {
                        viewModel.onEvent(ResetPasswordViewModel.Event.TogglePasswordVisibility)
                    }) {
                        val icon = if (state.isPasswordVisible) {
                            Icons.VisibilityW500Outlined
                        } else {
                            Icons.VisibilityOffW500Outlined
                        }
                        Icon(icon, contentDescription = stringResource(Res.string.auth_toggle_password_desc))
                    }
                },
                visualTransformation = if (state.isPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                isError = state.confirmPasswordError != null,
                supportingText = state.confirmPasswordError?.let {
                    { Text(stringResource(it)) }
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = {
                    if (state.isFormValid) {
                        viewModel.onEvent(ResetPasswordViewModel.Event.Submit)
                    }
                }),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(Res.string.reset_password_requirements),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(24.dp))

            // Submit button
            Button(
                onClick = { viewModel.onEvent(ResetPasswordViewModel.Event.Submit) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(50),
                enabled = !state.isLoading && state.isFormValid && !state.isPasswordReset,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(
                        stringResource(Res.string.reset_password_submit),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Continue button (after reset)
            AnimatedVisibility(
                visible = state.isPasswordReset,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Button(
                    onClick = onPasswordReset,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                    ),
                ) {
                    Text(
                        stringResource(Res.string.forgot_password_back_to_login),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
