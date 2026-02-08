@file:Suppress("FunctionName")

package com.group8.comp2300.presentation.ui.screens.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.app.symbols.icons.materialsymbols.Icons
import com.app.symbols.icons.materialsymbols.icons.*
import com.app.symbols.icons.materialsymbols.icons.VisibilityOffW500Outlined
import com.app.symbols.icons.materialsymbols.icons.VisibilityW500Outlined
import com.group8.comp2300.presentation.ui.screens.auth.components.AuthDropdown
import com.group8.comp2300.presentation.ui.screens.auth.components.AuthTextField
import com.group8.comp2300.presentation.ui.screens.auth.components.ClickableTextField
import comp2300.i18n.generated.resources.*
import kotlin.time.Clock
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = koinViewModel(),
    onDismiss: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
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
                initialSelectedDateMillis = state.dateOfBirth ?: Clock.System.now().toEpochMilliseconds()
            )
        DatePickerDialog(
            onDismissRequest = { viewModel.onEvent(AuthViewModel.AuthUiEvent.ShowDatePicker(false)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onEvent(AuthViewModel.AuthUiEvent.DateOfBirthChanged(dateState.selectedDateMillis))
                    }
                ) {
                    Text(stringResource(Res.string.auth_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(AuthViewModel.AuthUiEvent.ShowDatePicker(false)) }) {
                    Text(stringResource(Res.string.auth_cancel))
                }
            }
        ) {
            DatePicker(state = dateState)
        }
    }

    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            Modifier.fillMaxSize().padding(innerPadding).verticalScroll(scrollState).padding(24.dp).imePadding(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HeaderSection(
                isRegistering = state.isRegistering,
                step = state.step,
                onBack = { viewModel.onEvent(AuthViewModel.AuthUiEvent.PrevStep) }
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
                    text = stringResource(state.errorMessage!!),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(32.dp))

            // Action Buttons
            ActionButtons(state = state, onEvent = viewModel::onEvent, onLoginSuccess = onLoginSuccess)

            Spacer(Modifier.height(16.dp))

            // Footer (Switch Mode / Guest)
            if (state.step == 0) {
                FooterSection(
                    isRegistering = state.isRegistering,
                    onToggleMode = { viewModel.onEvent(AuthViewModel.AuthUiEvent.ToggleAuthMode) },
                    onGuestParams = onDismiss
                )
            }
        }
    }
}

@Composable
private fun HeaderSection(isRegistering: Boolean, step: Int, onBack: () -> Unit) {
    Column {
        if (isRegistering && step == 1) {
            Box(Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack, Modifier.align(Alignment.CenterStart)) {
                    Icon(
                        Icons.ArrowBackW400Outlinedfill1,
                        contentDescription = stringResource(Res.string.auth_back_desc)
                    )
                }
            }
        }
        Text(
            text =
                if (isRegistering) {
                    stringResource(Res.string.auth_create_account)
                } else {
                    stringResource(Res.string.auth_welcome_back)
                },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text =
                if (isRegistering) {
                    stringResource(Res.string.auth_join_desc)
                } else {
                    stringResource(Res.string.auth_sign_in_desc)
                },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
private fun CredentialsStep(state: AuthViewModel.State, onEvent: (AuthViewModel.AuthUiEvent) -> Unit) {
    Column {
        val focusManager = LocalFocusManager.current

        AuthTextField(
            value = state.email,
            onValueChange = { onEvent(AuthViewModel.AuthUiEvent.EmailChanged(it)) },
            label = stringResource(Res.string.auth_email_label),
            leadingIcon = Icons.MailOutlineW400Outlinedfill1,
            errorMessage = state.emailError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )
        Spacer(Modifier.height(16.dp))

        AuthTextField(
            value = state.password,
            onValueChange = { onEvent(AuthViewModel.AuthUiEvent.PasswordChanged(it)) },
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
                            com.app.symbols.icons.materialsymbols.Icons.VisibilityW500Outlined
                        } else {
                            com.app.symbols.icons.materialsymbols.Icons.VisibilityOffW500Outlined
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
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )
        Spacer(Modifier.height(16.dp))

        if (state.isRegistering) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { onEvent(AuthViewModel.AuthUiEvent.ToggleTerms) }
            ) {
                Checkbox(
                    checked = state.termsAccepted,
                    onCheckedChange = { onEvent(AuthViewModel.AuthUiEvent.ToggleTerms) }
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
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun PersonalDetailsStep(state: AuthViewModel.State, onEvent: (AuthViewModel.AuthUiEvent) -> Unit) {
    Column {
        val focusManager = LocalFocusManager.current

        AuthTextField(
            value = state.firstName,
            onValueChange = { onEvent(AuthViewModel.AuthUiEvent.FirstNameChanged(it)) },
            label = stringResource(Res.string.auth_first_name_label),
            leadingIcon = Icons.PersonW400Outlinedfill1,
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )
        Spacer(Modifier.height(16.dp))

        AuthTextField(
            value = state.lastName,
            onValueChange = { onEvent(AuthViewModel.AuthUiEvent.LastNameChanged(it)) },
            label = stringResource(Res.string.auth_last_name_label),
            leadingIcon = Icons.PersonW400Outlinedfill1,
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
            label = stringResource(Res.string.auth_dob_label),
            leadingIcon = Icons.DateRangeW400Outlinedfill1,
            onClick = { onEvent(AuthViewModel.AuthUiEvent.ShowDatePicker(true)) }
        )
        Spacer(Modifier.height(16.dp))

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Text(stringResource(Res.string.auth_optional_info), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))

        AuthDropdown(
            label = stringResource(Res.string.auth_gender_label),
            selectedValue = state.gender,
            options =
                listOf(
                    stringResource(Res.string.auth_gender_male),
                    stringResource(Res.string.auth_gender_female),
                    stringResource(Res.string.auth_gender_non_binary),
                    stringResource(Res.string.auth_gender_prefer_not_to_say)
                ),
            onSelectOption = { onEvent(AuthViewModel.AuthUiEvent.GenderChanged(it)) }
        )
        Spacer(Modifier.height(16.dp))

        AuthDropdown(
            label = stringResource(Res.string.auth_orientation_label),
            selectedValue = state.sexualOrientation,
            options =
                listOf(
                    stringResource(Res.string.auth_orientation_heterosexual),
                    stringResource(Res.string.auth_orientation_gay),
                    stringResource(Res.string.auth_orientation_lesbian),
                    stringResource(Res.string.auth_orientation_bisexual),
                    stringResource(Res.string.auth_orientation_pansexual),
                    stringResource(Res.string.auth_orientation_asexual)
                ),
            onSelectOption = { onEvent(AuthViewModel.AuthUiEvent.OrientationChanged(it)) }
        )
    }
}

@Composable
private fun ActionButtons(
    state: AuthViewModel.State,
    onEvent: (AuthViewModel.AuthUiEvent) -> Unit,
    onLoginSuccess: () -> Unit
) {
    Column {
        Button(
            onClick = {
                onEvent(AuthViewModel.AuthUiEvent.ClearError)
                when {
                    state.isRegistering && state.step == 0 -> {
                        if (state.isStep1Valid) onEvent(AuthViewModel.AuthUiEvent.NextStep)
                    }

                    else -> onEvent(AuthViewModel.AuthUiEvent.Submit(onLoginSuccess))
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled =
                !state.isLoading &&
                    if (state.isRegistering && state.step == 0) {
                        state.isStep1Valid
                    } else if (state.isRegistering) {
                        state.isStep2Valid
                    } else {
                        (state.email.isNotBlank() && state.password.isNotBlank())
                    },
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
                        state.isRegistering && state.step == 0 -> stringResource(Res.string.auth_continue)
                        state.isRegistering -> stringResource(Res.string.auth_sign_up)
                        else -> stringResource(Res.string.auth_sign_in)
                    }
                )
            }
        }
    }
}

@Composable
private fun FooterSection(isRegistering: Boolean, onToggleMode: () -> Unit, onGuestParams: () -> Unit) {
    Row(horizontalArrangement = Arrangement.Center) {
        TextButton(onClick = onToggleMode) {
            Text(
                if (isRegistering) {
                    stringResource(Res.string.auth_already_have_account)
                } else {
                    stringResource(Res.string.auth_no_account)
                }
            )
        }
        TextButton(onClick = onGuestParams) {
            Text(stringResource(Res.string.auth_continue_as_guest), color = MaterialTheme.colorScheme.secondary)
        }
    }
}
