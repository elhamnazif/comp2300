package com.group8.comp2300.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.group8.comp2300.core.ui.components.AppTopBar
import com.group8.comp2300.feature.auth.components.*
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.DateRangeW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.PersonW400Outlinedfill1
import comp2300.i18n.generated.resources.*
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock

internal enum class ProfileEditorMode {
    Complete,
    Edit,
}

private fun optionalLabel(label: String, optional: String): String = "$label ($optional)"

@Composable
internal fun ProfileEditorScreen(
    mode: ProfileEditorMode,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileEditorViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val currentOnSave by rememberUpdatedState(onSave)

    val filePickerLauncher = rememberFilePickerLauncher(
        type = FileKitType.File(extensions = listOf("jpg", "jpeg", "png", "webp")),
    ) { file ->
        if (file != null) {
            coroutineScope.launch {
                viewModel.onPhotoSelected(file.name, file.readBytes())
            }
        }
    }

    if (state.isComplete) {
        currentOnSave()
    }

    if (state.showDatePicker) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = state.dateOfBirthMillis ?: Clock.System.now().toEpochMilliseconds(),
        )
        DatePickerDialog(
            onDismissRequest = { viewModel.setDatePickerVisible(false) },
            confirmButton = {
                TextButton(onClick = { viewModel.onDateOfBirthChanged(dateState.selectedDateMillis) }) {
                    Text(stringResource(Res.string.auth_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setDatePickerVisible(false) }) {
                    Text(stringResource(Res.string.common_cancel))
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
                title = {
                    Text(
                        text = when (mode) {
                            ProfileEditorMode.Complete -> stringResource(Res.string.complete_profile_title)
                            ProfileEditorMode.Edit -> stringResource(Res.string.profile_edit_label)
                        },
                    )
                },
                onBackClick = onBack,
                backContentDescription = stringResource(Res.string.auth_back_desc),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .imePadding()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            AuthBanner(message = state.errorMessage, modifier = Modifier.padding(top = 8.dp))

            Text(
                text = when (mode) {
                    ProfileEditorMode.Complete -> stringResource(Res.string.complete_profile_desc)
                    ProfileEditorMode.Edit -> stringResource(Res.string.profile_edit_desc)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProfileAvatar(
                    initials = state.initials,
                    imageModel = state.previewImageModel,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = { filePickerLauncher.launch() }) {
                        Text(stringResource(Res.string.profile_change_photo))
                    }
                    if (state.previewImageModel != null || state.originalProfileImageUrl != null) {
                        TextButton(onClick = viewModel::removePhoto) {
                            Text(stringResource(Res.string.profile_remove_photo))
                        }
                    }
                }
            }

            AuthTextField(
                value = state.firstName,
                onValueChange = viewModel::onFirstNameChanged,
                label = stringResource(Res.string.auth_first_name_label),
                leadingIcon = Icons.PersonW400Outlinedfill1,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            )

            AuthTextField(
                value = state.lastName,
                onValueChange = viewModel::onLastNameChanged,
                label = stringResource(Res.string.auth_last_name_label),
                leadingIcon = Icons.PersonW400Outlinedfill1,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            )

            AuthTextField(
                value = state.phone,
                onValueChange = viewModel::onPhoneChanged,
                label = optionalLabel(
                    stringResource(Res.string.auth_phone_label),
                    stringResource(Res.string.common_optional),
                ),
                leadingIcon = Icons.PersonW400Outlinedfill1,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            )

            ClickableTextField(
                value = state.formattedDate(),
                label = stringResource(Res.string.auth_dob_label),
                leadingIcon = Icons.DateRangeW400Outlinedfill1,
                onClick = { viewModel.setDatePickerVisible(true) },
            )

            AuthDropdown(
                label = optionalLabel(
                    stringResource(Res.string.auth_gender_label),
                    stringResource(Res.string.common_optional),
                ),
                selectedValue = state.gender,
                options = listOf(
                    stringResource(Res.string.auth_gender_male),
                    stringResource(Res.string.auth_gender_female),
                    stringResource(Res.string.auth_gender_non_binary),
                    stringResource(Res.string.auth_gender_prefer_not_to_say),
                ),
                onSelectOption = viewModel::onGenderChanged,
                allowClear = true,
                clearLabel = stringResource(Res.string.common_none),
            )

            AuthDropdown(
                label = optionalLabel(
                    stringResource(Res.string.auth_orientation_label),
                    stringResource(Res.string.common_optional),
                ),
                selectedValue = state.sexualOrientation,
                options = listOf(
                    stringResource(Res.string.auth_orientation_heterosexual),
                    stringResource(Res.string.auth_orientation_gay),
                    stringResource(Res.string.auth_orientation_lesbian),
                    stringResource(Res.string.auth_orientation_bisexual),
                    stringResource(Res.string.auth_orientation_pansexual),
                    stringResource(Res.string.auth_orientation_asexual),
                ),
                onSelectOption = viewModel::onOrientationChanged,
                allowClear = true,
                clearLabel = stringResource(Res.string.common_none),
            )

            Spacer(Modifier.height(8.dp))

            AuthLoadingButton(
                text = when (mode) {
                    ProfileEditorMode.Complete -> stringResource(Res.string.complete_profile_button)
                    ProfileEditorMode.Edit -> stringResource(Res.string.profile_save_button)
                },
                onClick = {
                    focusManager.clearFocus(force = true)
                    keyboardController?.hide()
                    viewModel.save()
                },
                enabled = state.isSaveEnabled,
                isLoading = state.isLoading,
                modifier = Modifier.padding(bottom = 24.dp),
            )
        }
    }
}
