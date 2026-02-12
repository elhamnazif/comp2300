@file:Suppress("FunctionName")

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
import com.group8.comp2300.mock.sampleMedications
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.*
import comp2300.i18n.generated.resources.*
import kotlin.time.Clock
import org.jetbrains.compose.resources.stringResource

// --- Data Models ---

enum class MedicationStatus {
    Active,
    Archived
}

data class Medication(
    val id: String,
    val name: String,
    val dosage: String,
    val frequency: String,
    val instructions: String,
    val color: Color,
    val status: MedicationStatus = MedicationStatus.Active
)

// --- Constants ---

private object MedConstants {
    val PresetColors =
        listOf(
            Color(0xFF42A5F5), // Blue
            Color(0xFFEF5350), // Red
            Color(0xFF66BB6A), // Green
            Color(0xFFFFA726), // Orange
            Color(0xFFAB47BC), // Purple
            Color(0xFF26C6DA), // Cyan
            Color(0xFF78909C) // Blue Grey
        )

    val Frequencies = listOf("Daily", "Twice Daily", "Weekly", "On Demand")
}

// --- Main Screen ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationScreen(modifier: Modifier = Modifier, isGuest: Boolean = false, onRequireAuth: () -> Unit = {}) {
    // Mock Database State
    // Mock Database State
    val medications = remember {
        mutableStateListOf<Medication>().apply {
            addAll(
                sampleMedications.map { domainMed ->
                    Medication(
                        id = domainMed.id,
                        name = domainMed.name,
                        dosage = domainMed.dosage,
                        frequency = domainMed.frequency.displayName,
                        instructions = domainMed.instructions,
                        color =
                            try {
                                // Simple hex parsing (simplified)
                                val hex = domainMed.colorHex.removePrefix("#")
                                Color(hex.toLong(16) or 0xFF00000000)
                            } catch (e: Exception) {
                                MedConstants.PresetColors.first()
                            },
                        status =
                            if (domainMed.status == com.group8.comp2300.domain.model.medical.MedicationStatus.ACTIVE) {
                                MedicationStatus.Active
                            } else {
                                MedicationStatus.Archived
                            }
                    )
                }
            )
        }
    }

    // Sheet State
    var showBottomSheet by remember { mutableStateOf(false) }
    var editingMedication by remember { mutableStateOf<Medication?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.medical_medication_title), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (isGuest) {
                        onRequireAuth()
                    } else {
                        editingMedication = null // Null means "New Mode"
                        showBottomSheet = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.AddW400Outlinedfill1,
                    contentDescription = stringResource(Res.string.medical_medication_add_desc)
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).background(MaterialTheme.colorScheme.surface),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Active Medications Section
            val activeMeds = medications.filter { it.status == MedicationStatus.Active }
            item {
                SectionHeader(
                    title = stringResource(Res.string.medical_medication_section_active),
                    count = activeMeds.size
                )
            }

            if (activeMeds.isEmpty()) {
                item {
                    EmptyStateMessage(
                        stringResource(Res.string.medical_medication_empty_active)
                    )
                }
            } else {
                items(activeMeds, key = { it.id }) { med ->
                    MedicationCard(
                        medication = med,
                        onClick = {
                            editingMedication = med
                            showBottomSheet = true
                        }
                    )
                }
            }

            // 2. Archived Medications Section
            val archivedMeds = medications.filter { it.status == MedicationStatus.Archived }
            if (archivedMeds.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(16.dp))
                    SectionHeader(
                        title = stringResource(Res.string.medical_medication_section_archived),
                        count = archivedMeds.size
                    )
                }
                items(archivedMeds, key = { it.id }) { med ->
                    MedicationCard(
                        medication = med,
                        onClick = {
                            editingMedication = med
                            showBottomSheet = true
                        },
                        isArchived = true
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) } // Space for FAB
        }

        if (showBottomSheet) {
            ModalBottomSheet(onDismissRequest = { showBottomSheet = false }, sheetState = sheetState) {
                MedicationFormSheet(
                    medicationToEdit = editingMedication,
                    onSave = { med ->
                        val index = medications.indexOfFirst { it.id == med.id }
                        if (index != -1) {
                            medications[index] = med // Update existing
                        } else {
                            medications.add(med) // Add new
                        }
                        showBottomSheet = false
                    },
                    onDelete = { medId ->
                        medications.removeAll { it.id == medId }
                        showBottomSheet = false
                    },
                    onCancel = { showBottomSheet = false }
                )
            }
        }
    }
}

// --- Components ---

@Composable
fun SectionHeader(title: String, count: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
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
    isArchived: Boolean = false
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
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Color Indicator
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(medication.color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(medication.color))
            }

            Spacer(Modifier.width(16.dp))

            // Details
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
                        }
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.InfoW400Outlinedfill1,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${medication.dosage} • ${medication.frequency}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                if (medication.instructions.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(Res.string.medical_medication_note_format, medication.instructions),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        maxLines = 1
                    )
                }
            }

            Icon(
                Icons.EditW400Outlinedfill1,
                contentDescription = stringResource(Res.string.medical_medication_edit_desc),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// --- Form Sheet ---

@Composable
fun MedicationFormSheet(
    medicationToEdit: Medication?,
    onSave: (Medication) -> Unit,
    onDelete: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEditMode = medicationToEdit != null

    // Form State
    var name by remember { mutableStateOf(medicationToEdit?.name ?: "") }
    var dosage by remember { mutableStateOf(medicationToEdit?.dosage ?: "") }
    var frequency by remember { mutableStateOf(medicationToEdit?.frequency ?: MedConstants.Frequencies.first()) }
    var instructions by remember { mutableStateOf(medicationToEdit?.instructions ?: "") }
    var selectedColor by remember { mutableStateOf(medicationToEdit?.color ?: MedConstants.PresetColors.first()) }
    var status by remember { mutableStateOf(medicationToEdit?.status ?: MedicationStatus.Active) }

    // Validation
    val isFormValid = name.isNotBlank() && dosage.isNotBlank()

    Column(
        modifier =
            modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text =
                    if (isEditMode) {
                        stringResource(Res.string.medical_medication_form_edit_title)
                    } else {
                        stringResource(Res.string.medical_medication_form_add_title)
                    },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            if (isEditMode) {
                IconButton(onClick = { onDelete(medicationToEdit.id) }) {
                    Icon(
                        Icons.DeleteW400Outlined,
                        contentDescription = stringResource(Res.string.medical_medication_delete_desc),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Name & Dosage
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(Res.string.medical_medication_form_name_label)) },
            placeholder = { Text(stringResource(Res.string.medical_medication_form_name_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions =
                KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next)
        )

        OutlinedTextField(
            value = dosage,
            onValueChange = { dosage = it },
            label = { Text(stringResource(Res.string.medical_medication_form_dosage_label)) },
            placeholder = { Text(stringResource(Res.string.medical_medication_form_dosage_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )

        // Frequency Selection
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(Res.string.medical_medication_form_frequency_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MedConstants.Frequencies.forEach { freq ->
                    val freqRes =
                        when (freq) {
                            "Daily" -> Res.string.medical_medication_freq_daily
                            "Twice Daily" -> Res.string.medical_medication_freq_twice_daily
                            "Weekly" -> Res.string.medical_medication_freq_weekly
                            "On Demand" -> Res.string.medical_medication_freq_on_demand
                            else -> Res.string.medical_medication_freq_daily // Fallback
                        }
                    FilterChip(
                        selected = frequency == freq,
                        onClick = { frequency = freq },
                        label = { Text(stringResource(freqRes)) },
                        leadingIcon = {
                            if (frequency == freq) {
                                Icon(Icons.CheckW400Outlinedfill1, null, Modifier.size(18.dp))
                            }
                        }
                    )
                }
            }
        }

        // Color Picker
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(Res.string.medical_medication_form_color_tag_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
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
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = color }
                    )
                }
            }
        }

        // Optional Instructions
        OutlinedTextField(
            value = instructions,
            onValueChange = { instructions = it },
            label = { Text(stringResource(Res.string.medical_medication_form_instructions_label)) },
            placeholder = { Text(stringResource(Res.string.medical_medication_form_instructions_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )

        // Archive Toggle (Only in Edit Mode)
        if (isEditMode) {
            Surface(
                onClick = {
                    status =
                        if (status == MedicationStatus.Active) {
                            MedicationStatus.Archived
                        } else {
                            MedicationStatus.Active
                        }
                },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            stringResource(Res.string.medical_medication_form_archive_title),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (status == MedicationStatus.Archived) {
                                stringResource(Res.string.medical_medication_form_archive_status_on)
                            } else {
                                stringResource(Res.string.medical_medication_form_archive_status_off)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Switch(
                        checked = status == MedicationStatus.Archived,
                        onCheckedChange = { isChecked ->
                            status =
                                if (isChecked) {
                                    MedicationStatus.Archived
                                } else {
                                    MedicationStatus.Active
                                }
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Actions
        Button(
            onClick = {
                val newMed =
                    Medication(
                        id = medicationToEdit?.id ?: Clock.System.now().toString(), // Simple ID generation
                        name = name,
                        dosage = dosage,
                        frequency = frequency,
                        instructions = instructions,
                        color = selectedColor,
                        status = status
                    )
                onSave(newMed)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = isFormValid
        ) {
            Text(
                if (isEditMode) {
                    stringResource(Res.string.medical_medication_form_save_button)
                } else {
                    stringResource(Res.string.medical_medication_form_add_title)
                }
            )
        }

        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(Res.string.medical_medication_form_cancel_button),
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
