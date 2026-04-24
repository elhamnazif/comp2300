package com.group8.comp2300.feature.auth.login

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.group8.comp2300.core.ui.components.AppTopBar
import com.group8.comp2300.feature.auth.components.AuthBanner
import com.group8.comp2300.feature.auth.components.AuthLoadingButton
import com.group8.comp2300.feature.auth.components.AuthPasswordField
import com.group8.comp2300.feature.auth.components.AuthTextField
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.MailOutlineW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.PersonW400Outlinedfill1
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AuthScreen(
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    initialSuccessMessage: String? = null,
    viewModel: AuthViewModel = koinViewModel(),
    onDismiss: () -> Unit = {},
    onNavigateToEmailVerification: (String) -> Unit = {},
    onNavigateToForgotPassword: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val authError = state.errorMessageRes?.let { stringResource(it) } ?: state.errorMessage
    val successMessage = if (authError == null) initialSuccessMessage else null

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
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 104.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column {
                    AuthBanner(
                        message = successMessage,
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    )
                    AuthBanner(message = authError)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .imePadding(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item {
                    HeaderSection(
                        isRegistering = state.isRegistering,
                    )
                }

                item { Spacer(Modifier.height(24.dp)) }

                // Credentials form
                item {
                    CredentialsForm(
                        state = state,
                        onEvent = viewModel::onEvent,
                        onNavigateToForgotPassword = onNavigateToForgotPassword,
                    )
                }

                item { Spacer(Modifier.height(16.dp)) }

                // Action Buttons
                item {
                    ActionButtons(
                        state = state,
                        onEvent = viewModel::onEvent,
                        onPrepareSubmit = {
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                        },
                        onLoginSuccess = onLoginSuccess,
                        onRegisterSuccess = { email ->
                            onNavigateToEmailVerification(email)
                        },
                    )
                }

                item { Spacer(Modifier.height(16.dp)) }

                // Footer (Switch Mode / Guest)
                item {
                    FooterSection(
                        isRegistering = state.isRegistering,
                        onToggleMode = { viewModel.onEvent(AuthViewModel.AuthUiEvent.ToggleAuthMode) },
                        onGuestParams = onDismiss,
                    )
                }
            }
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
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
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

        AuthPasswordField(
            value = state.password,
            onValueChange = {
                onEvent(
                    AuthViewModel.AuthUiEvent.PasswordChanged(
                        it,
                    ),
                )
            },
            label = stringResource(Res.string.auth_password_label),
            errorMessage =
            if (state.isRegistering) {
                state.passwordError
            } else {
                null
            }, // Don't show validation error on Login
            isPasswordVisible = state.isPasswordVisible,
            onTogglePasswordVisibility = { onEvent(AuthViewModel.AuthUiEvent.TogglePasswordVisibility) },
            passwordToggleDescription = stringResource(Res.string.auth_toggle_password_desc),
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
    onPrepareSubmit: () -> Unit,
    onLoginSuccess: () -> Unit,
    onRegisterSuccess: (String) -> Unit = {},
) {
    Column {
        AuthLoadingButton(
            text = if (state.isRegistering) {
                stringResource(Res.string.auth_continue)
            } else {
                stringResource(Res.string.auth_sign_in)
            },
            onClick = {
                onPrepareSubmit()
                onEvent(AuthViewModel.AuthUiEvent.ClearError)
                if (state.isRegistering) {
                    // Registration - call preregister API then navigate to email verification
                    onEvent(AuthViewModel.AuthUiEvent.Submit { onRegisterSuccess(state.email) })
                } else {
                    // Login - call login API
                    onEvent(AuthViewModel.AuthUiEvent.Submit(onLoginSuccess))
                }
            },
            enabled = !state.isLoading && state.isValid,
            isLoading = state.isLoading,
        )
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
