package com.group8.comp2300.feature.auth.completeprofile

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.group8.comp2300.feature.auth.components.*
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
    viewModel: CompleteProfileViewModel = koinViewModel<CompleteProfileViewModel> {
        parametersOf(email)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

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

    AuthFormScaffold(
        onBack = onBack,
        modifier = modifier,
        bannerContent = {
            AuthBanner(message = authError)
        },
    ) {
        AuthHeroSection(
            icon = Icons.PersonW400Outlinedfill1,
            title = stringResource(Res.string.complete_profile_title),
            description = stringResource(Res.string.complete_profile_desc),
        )

        Spacer(Modifier.height(24.dp))

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

        AuthLoadingButton(
            text = stringResource(Res.string.complete_profile_button),
            onClick = {
                focusManager.clearFocus(force = true)
                keyboardController?.hide()
                viewModel.onEvent(CompleteProfileViewModel.Event.Submit)
            },
            enabled = !state.isLoading && state.isFormValid,
            isLoading = state.isLoading,
        )
    }
}
