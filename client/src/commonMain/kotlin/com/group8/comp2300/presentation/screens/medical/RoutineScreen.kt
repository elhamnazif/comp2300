package com.group8.comp2300.presentation.screens.medical

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.group8.comp2300.domain.model.medical.Medication
import com.group8.comp2300.domain.model.medical.MedicationStatus
import com.group8.comp2300.domain.model.medical.Routine
import com.group8.comp2300.domain.model.medical.RoutineStatus
import com.group8.comp2300.presentation.components.AppTopBar
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.AddW400Outlinedfill1
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RoutineScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    viewModel: RoutineViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by remember { mutableStateOf(false) }
    var editingRoutine by remember { mutableStateOf<Routine?>(null) }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    val activeRoutines = state.routines.filter { it.status == RoutineStatus.ACTIVE }
    val archivedRoutines = state.routines.filter { it.status == RoutineStatus.ARCHIVED }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = { Text("Schedules", fontWeight = FontWeight.Bold) },
                onBackClick = onBack,
                containerColor = MaterialTheme.colorScheme.surface,
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingRoutine = null
                showSheet = true
            }) {
                Icon(Icons.AddW400Outlinedfill1, contentDescription = "Add schedule")
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surface),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (activeRoutines.isEmpty() && archivedRoutines.isEmpty()) {
                item {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(20.dp)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("No schedules yet", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text(
                                "Create reminder schedules like Morning meds or Bedtime meds.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button(onClick = {
                                editingRoutine = null
                                showSheet = true
                            }) {
                                Text("Create schedule")
                            }
                        }
                    }
                }
            } else {
                if (activeRoutines.isNotEmpty()) {
                    item { SectionHeader("Active", activeRoutines.size) }
                }
                items(activeRoutines, key = Routine::id) { routine ->
                    RoutineCard(
                        routine = routine,
                        medications = state.medications,
                        onClick = {
                            editingRoutine = routine
                            showSheet = true
                        },
                    )
                }
            }
            if (archivedRoutines.isNotEmpty()) {
                item { SectionHeader("Archived", archivedRoutines.size) }
                items(archivedRoutines, key = Routine::id) { routine ->
                    RoutineCard(
                        routine = routine,
                        medications = state.medications,
                        isArchived = true,
                        onClick = {
                            editingRoutine = routine
                            showSheet = true
                        },
                    )
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }, sheetState = sheetState) {
            ScheduleFormSheet(
                title = if (editingRoutine == null) "Add schedule" else "Edit schedule",
                routineToEdit = editingRoutine,
                medications = state.medications.filter { it.status == MedicationStatus.ACTIVE },
                onSave = { request, id ->
                    viewModel.saveRoutine(request, id)
                    showSheet = false
                },
                onDelete = { routineId ->
                    viewModel.deleteRoutine(routineId)
                    showSheet = false
                },
                onCancel = { showSheet = false },
            )
        }
    }
}

@Composable
private fun RoutineCard(
    routine: Routine,
    medications: List<Medication>,
    onClick: () -> Unit,
    isArchived: Boolean = false,
) {
    val linked = medications.filter { it.id in routine.medicationIds }
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isArchived) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(routine.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                routine.timesOfDayMs.sorted().distinct().forEach { timeOfDayMs ->
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(999.dp)) {
                        Text(
                            formatTimeOfDayMs(timeOfDayMs),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
            }
            Text(scheduleSummary(routine), color = MaterialTheme.colorScheme.secondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                linked.forEach { medication ->
                    AssistChip(onClick = {}, label = { Text(medication.name) })
                }
            }
        }
    }
}
