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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.group8.comp2300.presentation.components.AppTopBar
import com.group8.comp2300.presentation.screens.auth.components.AuthDropdown
import com.group8.comp2300.presentation.screens.auth.components.AuthTextField
import com.group8.comp2300.presentation.screens.auth.components.ClickableTextField
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.DateRangeW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.PersonW400Outlinedfill1
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Clock

@Composable
fun CompleteProfileScreen(
    email: String,
    onComplete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CompleteProfileViewModel = koinViewModel<RealCompleteProfileViewModel> {
        parametersOf(email)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    val authError = state.errorMessageRes?.let { stringResource(it) } ?: state.errorMessage

    // Navigate on completion
    if (state.isComplete) {
        onComplete()
    }

    // Date Picker Dialog
    if (state.showDatePicker) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = state.dateOfBirth ?: Clock.System.now().toEpochMilliseconds(),
        )
        DatePickerDialog(
            onDismissRequest = { viewModel.onEvent(CompleteProfileViewModel.Event.ShowDatePicker(false)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onEvent(
                            CompleteProfileViewModel.Event.DateOfBirthChanged(dateState.selectedDateMillis),
                        )
                    },
                ) {
                    Text(stringResource(Res.string.auth_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(CompleteProfileViewModel.Event.ShowDatePicker(false)) }) {
                    Text(stringResource(Res.string.auth_cancel))
                }
            },
        ) {
            DatePicker(state = dateState)
        }
    }

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
            // Icon
            Card(
                shape = RoundedCornerShape(50),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                modifier = Modifier.size(80.dp),
            ) {
                Icon(
                    Icons.PersonW400Outlinedfill1,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = stringResource(Res.string.complete_profile_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(Res.string.complete_profile_desc),
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

            // Form fields
            val focusManager = LocalFocusManager.current

            AuthTextField(
                value = state.firstName,
                onValueChange = { viewModel.onEvent(CompleteProfileViewModel.Event.FirstNameChanged(it)) },
                label = stringResource(Res.string.auth_first_name_label),
                leadingIcon = Icons.PersonW400Outlinedfill1,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    keyboardType = KeyboardType.Text,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            )

            AuthTextField(
                value = state.lastName,
                onValueChange = { viewModel.onEvent(CompleteProfileViewModel.Event.LastNameChanged(it)) },
                label = stringResource(Res.string.auth_last_name_label),
                leadingIcon = Icons.PersonW400Outlinedfill1,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    keyboardType = KeyboardType.Text,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            )

            ClickableTextField(
                value = state.getFormattedDate(),
                label = stringResource(Res.string.auth_dob_label),
                leadingIcon = Icons.DateRangeW400Outlinedfill1,
                onClick = { viewModel.onEvent(CompleteProfileViewModel.Event.ShowDatePicker(true)) },
            )

            Spacer(Modifier.height(16.dp))

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text(stringResource(Res.string.auth_optional_info), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))

            AuthDropdown(
                label = stringResource(Res.string.auth_gender_label),
                selectedValue = state.gender,
                options = listOf(
                    stringResource(Res.string.auth_gender_male),
                    stringResource(Res.string.auth_gender_female),
                    stringResource(Res.string.auth_gender_non_binary),
                    stringResource(Res.string.auth_gender_prefer_not_to_say),
                ),
                onSelectOption = { viewModel.onEvent(CompleteProfileViewModel.Event.GenderChanged(it)) },
            )

            Spacer(Modifier.height(16.dp))

            AuthDropdown(
                label = stringResource(Res.string.auth_orientation_label),
                selectedValue = state.sexualOrientation,
                options = listOf(
                    stringResource(Res.string.auth_orientation_heterosexual),
                    stringResource(Res.string.auth_orientation_gay),
                    stringResource(Res.string.auth_orientation_lesbian),
                    stringResource(Res.string.auth_orientation_bisexual),
                    stringResource(Res.string.auth_orientation_pansexual),
                    stringResource(Res.string.auth_orientation_asexual),
                ),
                onSelectOption = { viewModel.onEvent(CompleteProfileViewModel.Event.OrientationChanged(it)) },
            )

            Spacer(Modifier.height(24.dp))

            // Submit button
            Button(
                onClick = { viewModel.onEvent(CompleteProfileViewModel.Event.Submit) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(50),
                enabled = !state.isLoading && state.isFormValid,
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
                        stringResource(Res.string.complete_profile_button),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
