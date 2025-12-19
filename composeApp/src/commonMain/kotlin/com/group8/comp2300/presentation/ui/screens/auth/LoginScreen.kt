package com.group8.comp2300.presentation.ui.screens.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.symbols.icons.materialsymbols.icons.VisibilityOffW500Outlined
import com.app.symbols.icons.materialsymbols.icons.VisibilityW500Outlined
import com.group8.comp2300.presentation.ui.screens.auth.components.AuthDropdown
import com.group8.comp2300.presentation.ui.screens.auth.components.AuthTextField
import com.group8.comp2300.presentation.ui.screens.auth.components.ClickableTextField
import kotlin.time.Clock
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
        viewModel: AuthViewModel = koinViewModel(),
        onLoginSuccess: () -> Unit,
        onDismiss: () -> Unit
) {
        // Collecting State safely with Lifecycle awareness
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val scrollState = rememberScrollState()

        // Auto-scroll to top on error
        LaunchedEffect(state.errorMessage) {
                if (state.errorMessage != null) {
                        scrollState.animateScrollTo(0)
                }
        }

        // Date Picker Dialog
        if (state.showDatePicker) {
                val dateState =
                        rememberDatePickerState(
                                initialSelectedDateMillis = state.dateOfBirth
                                                ?: Clock.System.now().toEpochMilliseconds()
                        )
                DatePickerDialog(
                        onDismissRequest = { viewModel.onEvent(AuthUiEvent.ShowDatePicker(false)) },
                        confirmButton = {
                                TextButton(
                                        onClick = {
                                                viewModel.onEvent(
                                                        AuthUiEvent.DateOfBirthChanged(
                                                                dateState.selectedDateMillis
                                                        )
                                                )
                                        }
                                ) { Text("OK") }
                        },
                        dismissButton = {
                                TextButton(
                                        onClick = {
                                                viewModel.onEvent(AuthUiEvent.ShowDatePicker(false))
                                        }
                                ) { Text("Cancel") }
                        }
                ) { DatePicker(state = dateState) }
        }

        Scaffold { innerPadding ->
                Column(
                        Modifier.fillMaxSize()
                                .padding(innerPadding)
                                .verticalScroll(scrollState)
                                .padding(24.dp)
                                .imePadding(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                        HeaderSection(
                                isRegistering = state.isRegistering,
                                step = state.step,
                                onBack = { viewModel.onEvent(AuthUiEvent.PrevStep) }
                        )

                        Spacer(Modifier.height(32.dp))

                        // Swap Content based on step
                        if (state.isRegistering && state.step == 1) {
                                PersonalDetailsStep(state, viewModel::onEvent)
                        } else {
                                CredentialsStep(state, viewModel::onEvent)
                        }

                        // Error Display
                        if (state.errorMessage != null) {
                                Spacer(Modifier.height(16.dp))
                                Text(
                                        text = state.errorMessage!!,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodyMedium
                                )
                        }

                        Spacer(Modifier.height(32.dp))

                        // Action Buttons
                        ActionButtons(
                                state = state,
                                onEvent = viewModel::onEvent,
                                onLoginSuccess = onLoginSuccess
                        )

                        Spacer(Modifier.height(16.dp))

                        // Footer (Switch Mode / Guest)
                        if (state.step == 0) {
                                FooterSection(
                                        isRegistering = state.isRegistering,
                                        onToggleMode = {
                                                viewModel.onEvent(AuthUiEvent.ToggleAuthMode)
                                        },
                                        onGuestParams = onDismiss
                                )
                        }
                }
        }
}

@Composable
private fun HeaderSection(isRegistering: Boolean, step: Int, onBack: () -> Unit) {
        if (isRegistering && step == 1) {
                Box(Modifier.fillMaxWidth()) {
                        IconButton(onClick = onBack, Modifier.align(Alignment.CenterStart)) {
                                Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                )
                        }
                }
        }
        Text(
                text = if (isRegistering) "Create Account" else "Welcome Back",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
                text = if (isRegistering) "Join to access full features" else "Sign in to continue",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
        )
}

@Composable
private fun CredentialsStep(state: AuthUiState, onEvent: (AuthUiEvent) -> Unit) {
        val focusManager = LocalFocusManager.current

        AuthTextField(
                value = state.email,
                onValueChange = { onEvent(AuthUiEvent.EmailChanged(it)) },
                label = "Email",
                leadingIcon = Icons.Default.Email,
                errorMessage = state.emailError,
                keyboardOptions =
                        KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                        ),
                keyboardActions =
                        KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )
        Spacer(Modifier.height(16.dp))

        AuthTextField(
                value = state.password,
                onValueChange = { onEvent(AuthUiEvent.PasswordChanged(it)) },
                label = "Password",
                leadingIcon = Icons.Default.Lock,
                errorMessage =
                        if (state.isRegistering) state.passwordError
                        else null, // Don't show validation error on Login
                trailingIcon = {
                        IconButton(onClick = { onEvent(AuthUiEvent.TogglePasswordVisibility) }) {
                                val icon =
                                        if (state.isPasswordVisible)
                                                com.app.symbols.icons.materialsymbols.Icons
                                                        .VisibilityW500Outlined
                                        else
                                                com.app.symbols.icons.materialsymbols.Icons
                                                        .VisibilityOffW500Outlined
                                Icon(icon, contentDescription = "Toggle password visibility")
                        }
                },
                visualTransformation =
                        if (state.isPasswordVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                keyboardOptions =
                        KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                        ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )
        Spacer(Modifier.height(16.dp))

        if (state.isRegistering) {
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                                Modifier.fillMaxWidth().clickable {
                                        onEvent(AuthUiEvent.ToggleTerms)
                                }
                ) {
                        Checkbox(
                                checked = state.termsAccepted,
                                onCheckedChange = { onEvent(AuthUiEvent.ToggleTerms) }
                        )
                        val annotatedText = buildAnnotatedString {
                                append("I agree to the ")
                                pushStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                                append("Terms")
                                pop()
                                append(" and ")
                                pushStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                                append("Privacy Policy")
                                pop()
                        }
                        Text(
                                annotatedText,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 8.dp)
                        )
                }
        }
}

@Composable
private fun PersonalDetailsStep(state: AuthUiState, onEvent: (AuthUiEvent) -> Unit) {
        val focusManager = LocalFocusManager.current

        AuthTextField(
                value = state.firstName,
                onValueChange = { onEvent(AuthUiEvent.FirstNameChanged(it)) },
                label = "First Name",
                leadingIcon = Icons.Default.Person,
                keyboardOptions =
                        KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                        ),
                keyboardActions =
                        KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )
        Spacer(Modifier.height(16.dp))

        AuthTextField(
                value = state.lastName,
                onValueChange = { onEvent(AuthUiEvent.LastNameChanged(it)) },
                label = "Last Name",
                leadingIcon = Icons.Default.Person,
                keyboardOptions =
                        KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Done
                        ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )
        Spacer(Modifier.height(16.dp))

        ClickableTextField(
                value = state.getFormattedDate(),
                label = "Date of Birth",
                leadingIcon = Icons.Default.DateRange,
                onClick = { onEvent(AuthUiEvent.ShowDatePicker(true)) }
        )
        Spacer(Modifier.height(16.dp))

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Text("Optional Information", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))

        AuthDropdown(
                label = "Gender",
                selectedValue = state.gender,
                options = listOf("Male", "Female", "Non-binary", "Prefer not to say"),
                onOptionSelected = { onEvent(AuthUiEvent.GenderChanged(it)) }
        )
        Spacer(Modifier.height(16.dp))

        AuthDropdown(
                label = "Sexual Orientation",
                selectedValue = state.sexualOrientation,
                options =
                        listOf(
                                "Heterosexual",
                                "Gay",
                                "Lesbian",
                                "Bisexual",
                                "Pansexual",
                                "Asexual"
                        ),
                onOptionSelected = { onEvent(AuthUiEvent.OrientationChanged(it)) }
        )
}

@Composable
private fun ActionButtons(
        state: AuthUiState,
        onEvent: (AuthUiEvent) -> Unit,
        onLoginSuccess: () -> Unit
) {
        Button(
                onClick = {
                        onEvent(AuthUiEvent.ClearError)
                        when {
                                state.isRegistering && state.step == 0 -> {
                                        if (state.isStep1Valid) onEvent(AuthUiEvent.NextStep)
                                }
                                else -> onEvent(AuthUiEvent.Submit(onLoginSuccess))
                        }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled =
                        !state.isLoading &&
                                if (state.isRegistering && state.step == 0) state.isStep1Valid
                                else if (state.isRegistering) state.isStep2Valid
                                else (state.email.isNotBlank() && state.password.isNotBlank()),
                colors =
                        ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
        ) {
                if (state.isLoading) {
                        CircularProgressIndicator(Modifier.size(24.dp))
                } else {
                        Text(
                                when {
                                        state.isRegistering && state.step == 0 -> "Continue"
                                        state.isRegistering -> "Sign Up"
                                        else -> "Sign In"
                                }
                        )
                }
        }
}

@Composable
private fun FooterSection(
        isRegistering: Boolean,
        onToggleMode: () -> Unit,
        onGuestParams: () -> Unit
) {
        TextButton(onClick = onToggleMode) {
                Text(
                        if (isRegistering) "Already have an account? Sign In"
                        else "Don't have an account? Sign Up"
                )
        }
        TextButton(onClick = onGuestParams) {
                Text("Continue as Guest", color = MaterialTheme.colorScheme.secondary)
        }
}
