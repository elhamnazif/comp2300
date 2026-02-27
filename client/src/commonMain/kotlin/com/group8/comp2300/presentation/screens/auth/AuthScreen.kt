package com.group8.comp2300.presentation.screens.auth

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.group8.comp2300.presentation.components.AppTopBar
import com.group8.comp2300.presentation.screens.auth.components.AuthTextField
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.*
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AuthScreen(
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = koinViewModel(),
    onDismiss: () -> Unit = {},
    onNavigateToEmailVerification: (String) -> Unit = {},
    onNavigateToForgotPassword: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    val authError = state.errorMessageRes?.let { stringResource(it) } ?: state.errorMessage

    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = {},
                onBackClick = onDismiss,
                backContentDescription = stringResource(Res.string.auth_back_desc),
            )
        },
    ) { innerPadding ->
        Column(
            Modifier.fillMaxSize().padding(innerPadding).verticalScroll(scrollState).padding(24.dp).imePadding(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HeaderSection(
                isRegistering = state.isRegistering,
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

            Spacer(Modifier.height(8.dp))

            // Credentials form
            CredentialsForm(
                state = state,
                onEvent = viewModel::onEvent,
                onNavigateToForgotPassword = onNavigateToForgotPassword,
            )

            Spacer(Modifier.height(16.dp))

            // Action Buttons
            ActionButtons(
                state = state,
                onEvent = viewModel::onEvent,
                onLoginSuccess = onLoginSuccess,
                onRegisterSuccess = { email ->
                    onNavigateToEmailVerification(email)
                },
            )

            Spacer(Modifier.height(16.dp))

            // Footer (Switch Mode / Guest)
            FooterSection(
                isRegistering = state.isRegistering,
                onToggleMode = { viewModel.onEvent(AuthViewModel.AuthUiEvent.ToggleAuthMode) },
                onGuestParams = onDismiss,
            )
        }
    }
}

@Composable
private fun HeaderSection(isRegistering: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Logo Placeholder
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    shape = androidx.compose.foundation.shape.CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.PersonW400Outlinedfill1,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(32.dp),
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text =
            if (isRegistering) {
                stringResource(Res.string.auth_create_account)
            } else {
                stringResource(Res.string.auth_welcome_back)
            },
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text =
            if (isRegistering) {
                stringResource(Res.string.auth_join_desc)
            } else {
                stringResource(Res.string.auth_sign_in_desc)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun CredentialsForm(
    state: AuthViewModel.State,
    onEvent: (AuthViewModel.AuthUiEvent) -> Unit,
    onNavigateToForgotPassword: () -> Unit = {},
) {
    Column {
        val focusManager = LocalFocusManager.current

        AuthTextField(
            value = state.email,
            onValueChange = {
                onEvent(
                    AuthViewModel.AuthUiEvent.EmailChanged(
                        it,
                    ),
                )
            },
            label = stringResource(Res.string.auth_email_label),
            leadingIcon = Icons.MailOutlineW400Outlinedfill1,
            errorMessage = state.emailError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
        )

        AuthTextField(
            value = state.password,
            onValueChange = {
                onEvent(
                    AuthViewModel.AuthUiEvent.PasswordChanged(
                        it,
                    ),
                )
            },
            label = stringResource(Res.string.auth_password_label),
            leadingIcon = Icons.LockW400Outlinedfill1,
            errorMessage =
            if (state.isRegistering) {
                state.passwordError
            } else {
                null
            }, // Don't show validation error on Login
            trailingIcon = {
                IconButton(onClick = { onEvent(AuthViewModel.AuthUiEvent.TogglePasswordVisibility) }) {
                    val icon =
                        if (state.isPasswordVisible) {
                            Icons.VisibilityW500Outlined
                        } else {
                            Icons.VisibilityOffW500Outlined
                        }
                    Icon(icon, contentDescription = stringResource(Res.string.auth_toggle_password_desc))
                }
            },
            visualTransformation =
            if (state.isPasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        )

        // Forgot Password link (only for login mode)
        if (!state.isRegistering) {
            Text(
                text = stringResource(Res.string.auth_forgot_password),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 4.dp)
                    .clickable { onNavigateToForgotPassword() },
            )
        }

        if (state.isRegistering) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { onEvent(AuthViewModel.AuthUiEvent.ToggleTerms) },
            ) {
                Checkbox(
                    checked = state.termsAccepted,
                    onCheckedChange = { onEvent(AuthViewModel.AuthUiEvent.ToggleTerms) },
                )
                val annotatedText = buildAnnotatedString {
                    append(stringResource(Res.string.auth_agree_to))
                    pushStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                    append(stringResource(Res.string.auth_terms))
                    pop()
                    append(stringResource(Res.string.auth_and))
                    pushStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                    append(stringResource(Res.string.auth_privacy_policy))
                    pop()
                }
                Text(
                    annotatedText,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun ActionButtons(
    state: AuthViewModel.State,
    onEvent: (AuthViewModel.AuthUiEvent) -> Unit,
    onLoginSuccess: () -> Unit,
    onRegisterSuccess: (String) -> Unit = {},
) {
    Column {
        Button(
            onClick = {
                onEvent(AuthViewModel.AuthUiEvent.ClearError)
                if (state.isRegistering) {
                    // Registration - call preregister API then navigate to email verification
                    onEvent(AuthViewModel.AuthUiEvent.Submit { onRegisterSuccess(state.email) })
                } else {
                    // Login - call login API
                    onEvent(AuthViewModel.AuthUiEvent.Submit(onLoginSuccess))
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
            enabled = !state.isLoading && state.isValid,
            colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(
                    if (state.isRegistering) {
                        stringResource(Res.string.auth_continue)
                    } else {
                        stringResource(Res.string.auth_sign_in)
                    },
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun FooterSection(isRegistering: Boolean, onToggleMode: () -> Unit, onGuestParams: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        TextButton(onClick = onToggleMode) {
            Text(
                if (isRegistering) {
                    stringResource(Res.string.auth_already_have_account)
                } else {
                    stringResource(Res.string.auth_no_account)
                },
            )
        }
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = onGuestParams) {
            Text(stringResource(Res.string.auth_continue_as_guest), color = MaterialTheme.colorScheme.secondary)
        }
    }
}
