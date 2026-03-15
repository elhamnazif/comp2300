package com.group8.comp2300.presentation.screens.medical

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.group8.comp2300.domain.model.medical.Medication
import com.group8.comp2300.domain.model.medical.MedicationCreateRequest
import com.group8.comp2300.domain.model.medical.MedicationFrequency
import com.group8.comp2300.domain.model.medical.MedicationStatus
import com.group8.comp2300.presentation.components.AppTopBar
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.*
import comp2300.i18n.generated.resources.*
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock

private object MedConstants {
    val PresetColors = Medication.PRESET_COLORS.map(::parseColorHex)
    val Frequencies = MedicationFrequency.entries
}

@Composable
fun MedicationScreen(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    viewModel: MedicationViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showBottomSheet by remember { mutableStateOf(false) }
    var editingMedication by remember { mutableStateOf<Medication?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.dismissError()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = { Text(stringResource(Res.string.medical_medication_title), fontWeight = FontWeight.Bold) },
                onBackClick = onBack,
                containerColor = MaterialTheme.colorScheme.surface,
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingMedication = null
                    showBottomSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(
                    Icons.AddW400Outlinedfill1,
                    contentDescription = stringResource(Res.string.medical_medication_add_desc),
                )
            }
        },
    ) { innerPadding ->
        val activeMeds = state.medications.filter { it.status == MedicationStatus.ACTIVE }
        val archivedMeds = state.medications.filter { it.status == MedicationStatus.ARCHIVED }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).background(MaterialTheme.colorScheme.surface),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item {
                SectionHeader(
                    title = stringResource(Res.string.medical_medication_section_active),
                    count = activeMeds.size,
                )
            }

            if (activeMeds.isEmpty()) {
                item { EmptyStateMessage(stringResource(Res.string.medical_medication_empty_active)) }
            } else {
                items(activeMeds, key = Medication::id) { medication ->
                    MedicationCard(
                        medication = medication,
                        onClick = {
                            editingMedication = medication
                            showBottomSheet = true
                        },
                    )
                }
            }

            if (archivedMeds.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(16.dp))
                    SectionHeader(
                        title = stringResource(Res.string.medical_medication_section_archived),
                        count = archivedMeds.size,
                    )
                }
                items(archivedMeds, key = Medication::id) { medication ->
                    MedicationCard(
                        medication = medication,
                        onClick = {
                            editingMedication = medication
                            showBottomSheet = true
                        },
                        isArchived = true,
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }

        if (showBottomSheet) {
            ModalBottomSheet(onDismissRequest = { showBottomSheet = false }, sheetState = sheetState) {
                MedicationFormSheet(
                    medicationToEdit = editingMedication,
                    onSave = { request, id ->
                        viewModel.saveMedication(request = request, id = id)
                        showBottomSheet = false
                    },
                    onDelete = { medicationId ->
                        viewModel.deleteMedication(medicationId)
                        showBottomSheet = false
                    },
                    onCancel = { showBottomSheet = false },
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, count: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(8.dp))
        Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
            Text(count.toString(), modifier = Modifier.padding(horizontal = 6.dp))
        }
    }
}

@Composable
fun EmptyStateMessage(msg: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
        Text(msg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun MedicationCard(
    medication: Medication,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isArchived: Boolean = false,
) {
    val cardAlpha = if (isArchived) 0.6f else 1f
    val containerColor =
        if (isArchived) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().alpha(cardAlpha),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            val color = parseColorHex(medication.colorHex)
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(color))
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = medication.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color =
                    if (isArchived) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.InfoW400Outlinedfill1,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${medication.dosage} • ${medication.frequency.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                medication.instruction?.takeIf(String::isNotBlank)?.let { instruction ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(Res.string.medical_medication_note_format, instruction),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        maxLines = 1,
                    )
                }
            }

            Icon(
                Icons.EditW400Outlinedfill1,
                contentDescription = stringResource(Res.string.medical_medication_edit_desc),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
fun MedicationFormSheet(
    medicationToEdit: Medication?,
    onSave: (MedicationCreateRequest, String?) -> Unit,
    onDelete: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isEditMode = medicationToEdit != null
    var name by remember { mutableStateOf(medicationToEdit?.name ?: "") }
    var dosage by remember { mutableStateOf(medicationToEdit?.dosage ?: "") }
    var frequency by remember { mutableStateOf(medicationToEdit?.frequency ?: MedConstants.Frequencies.first()) }
    var instructions by remember { mutableStateOf(medicationToEdit?.instruction.orEmpty()) }
    var selectedColor by remember { mutableStateOf(parseColorHex(medicationToEdit?.colorHex)) }
    var status by remember { mutableStateOf(medicationToEdit?.status ?: MedicationStatus.ACTIVE) }
    val isFormValid = name.isNotBlank() && dosage.isNotBlank()

    Column(
        modifier =
        modifier.fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .imePadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text =
                if (isEditMode) {
                    stringResource(Res.string.medical_medication_form_edit_title)
                } else {
                    stringResource(Res.string.medical_medication_form_add_title)
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            if (isEditMode) {
                IconButton(onClick = { onDelete(medicationToEdit.id) }) {
                    Icon(
                        Icons.DeleteW400Outlined,
                        contentDescription = stringResource(Res.string.medical_medication_delete_desc),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(Res.string.medical_medication_form_name_label)) },
            placeholder = { Text(stringResource(Res.string.medical_medication_form_name_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions =
            KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
        )

        OutlinedTextField(
            value = dosage,
            onValueChange = { dosage = it },
            label = { Text(stringResource(Res.string.medical_medication_form_dosage_label)) },
            placeholder = { Text(stringResource(Res.string.medical_medication_form_dosage_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(Res.string.medical_medication_form_frequency_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MedConstants.Frequencies.forEach { option ->
                    FilterChip(
                        selected = frequency == option,
                        onClick = { frequency = option },
                        label = { Text(option.displayName) },
                        leadingIcon = {
                            if (frequency == option) {
                                Icon(Icons.CheckW400Outlinedfill1, null, Modifier.size(18.dp))
                            }
                        },
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(Res.string.medical_medication_form_color_tag_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MedConstants.PresetColors.forEach { color ->
                    val isSelected = selectedColor == color
                    Box(
                        modifier =
                        Modifier.size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color =
                                if (isSelected) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    Color.Transparent
                                },
                                shape = CircleShape,
                            )
                            .clickable { selectedColor = color },
                    )
                }
            }
        }

        OutlinedTextField(
            value = instructions,
            onValueChange = { instructions = it },
            label = { Text(stringResource(Res.string.medical_medication_form_instructions_label)) },
            placeholder = { Text(stringResource(Res.string.medical_medication_form_instructions_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
        )

        if (isEditMode) {
            Surface(
                onClick = {
                    status =
                        if (status == MedicationStatus.ACTIVE) {
                            MedicationStatus.ARCHIVED
                        } else {
                            MedicationStatus.ACTIVE
                        }
                },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            stringResource(Res.string.medical_medication_form_archive_title),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            if (status == MedicationStatus.ARCHIVED) {
                                stringResource(Res.string.medical_medication_form_archive_status_on)
                            } else {
                                stringResource(Res.string.medical_medication_form_archive_status_off)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    Switch(
                        checked = status == MedicationStatus.ARCHIVED,
                        onCheckedChange = { checked ->
                            status = if (checked) MedicationStatus.ARCHIVED else MedicationStatus.ACTIVE
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                onSave(
                    MedicationCreateRequest(
                        name = name,
                        dosage = dosage,
                        quantity = medicationToEdit?.quantity.orEmpty(),
                        frequency = frequency.name,
                        instruction = instructions.takeIf(String::isNotBlank),
                        colorHex = selectedColor.toHexString(),
                        startDate = medicationToEdit?.startDate ?: today.toString(),
                        endDate = medicationToEdit?.endDate ?: today.plus(1, DateTimeUnit.YEAR).toString(),
                        hasReminder = medicationToEdit?.hasReminder ?: true,
                        status = status.name,
                    ),
                    medicationToEdit?.id,
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = isFormValid,
        ) {
            Text(
                if (isEditMode) {
                    stringResource(Res.string.medical_medication_form_save_button)
                } else {
                    stringResource(Res.string.medical_medication_form_add_title)
                },
            )
        }

        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(Res.string.medical_medication_form_cancel_button),
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

private fun parseColorHex(hex: String?): Color = try {
    val raw = (hex ?: Medication.PRESET_COLORS.first()).removePrefix("#")
    Color((raw.toLong(16) or 0xFF000000).toULong())
} catch (_: Exception) {
    Color(0xFF42A5F5)
}

private fun Color.toHexString(): String {
    val red = (this.red * 255).toInt().coerceIn(0, 255)
    val green = (this.green * 255).toInt().coerceIn(0, 255)
    val blue = (this.blue * 255).toInt().coerceIn(0, 255)
    return "#${red.hex2()}${green.hex2()}${blue.hex2()}"
}

private fun Int.hex2(): String = toString(16).uppercase().padStart(2, '0')
