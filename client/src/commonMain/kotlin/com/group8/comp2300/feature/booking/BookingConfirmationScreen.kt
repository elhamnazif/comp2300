package com.group8.comp2300.feature.booking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.group8.comp2300.app.navigation.Screen
import com.group8.comp2300.core.ui.components.AppTopBar
import com.group8.comp2300.domain.model.medical.Appointment
import com.group8.comp2300.util.formatCurrency
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BookingConfirmationScreen(
    clinicId: String,
    slotId: String,
    rescheduleAppointment: Appointment? = null,
    onBack: () -> Unit,
    isSignedIn: Boolean,
    onRequireAuth: () -> Unit,
    onContinueToPayment: (Screen.BookingPayment) -> Unit,
    onBookingConfirmed: (Appointment, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookingViewModel = koinViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val clinic = state.selectedClinic?.takeIf { it.id == clinicId } ?: viewModel.getClinic(clinicId)
    val slot = viewModel.getSlot(slotId)
    val draft = state.bookingDraft?.takeIf { it.clinicId == clinicId && it.slotId == slotId }
    val snackbarHostState = remember { SnackbarHostState() }
    val appointmentTypes = listOf(
        "STI_TESTING" to "STI testing",
        "SYMPTOMS" to "Symptoms",
        "CONTRACEPTION" to "Contraception",
        "PREP_PEP" to "PrEP / PEP",
        "FOLLOW_UP" to "Follow-up",
    )
    val symptomChips = listOf(
        "Discharge",
        "Burning urination",
        "Itching",
        "Sores",
        "Rash",
        "Pelvic pain",
        "Pain during sex",
        "Bleeding",
        "Missed period",
        "Exposure concern",
    )

    LaunchedEffect(clinicId, slotId, rescheduleAppointment?.id) {
        viewModel.ensureBookingDraft(
            clinicId = clinicId,
            slotId = slotId,
            rescheduleAppointment = rescheduleAppointment,
        )
        if (clinic == null || slot == null) {
            viewModel.loadClinicDetails(clinicId)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.bookingEvents.collect { event ->
            when (event) {
                is BookingViewModel.BookingEvent.Submitted -> onBookingConfirmed(
                    event.appointment,
                    event.wasRescheduled,
                )
            }
        }
    }

    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearBookingError()
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = { Text(if (rescheduleAppointment == null) "Review" else "Review change") },
                onBackClick = onBack,
                backContentDescription = "Back",
            )
        },
        bottomBar = {
            if (clinic != null && slot != null && draft != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceBright,
                    tonalElevation = 6.dp,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (isSignedIn) {
                            Text(
                                text = slotSummary(slot.startTime),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Button(
                            onClick = {
                                if (!isSignedIn) {
                                    onRequireAuth()
                                    return@Button
                                }
                                if (rescheduleAppointment != null) {
                                    viewModel.bookClinicAppointment(
                                        clinicId = clinic.id,
                                        slotId = slot.id,
                                        appointmentType = draft.appointmentType,
                                        reason = draft.reason,
                                        hasReminder = draft.hasReminder,
                                    )
                                } else {
                                    onContinueToPayment(
                                        Screen.BookingPayment(
                                            clinicId = clinic.id,
                                            slotId = slot.id,
                                            appointmentType = draft.appointmentType,
                                            reason = draft.reason,
                                            hasReminder = draft.hasReminder,
                                        ),
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isSubmitting,
                        ) {
                            if (state.isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            } else {
                                Text(
                                    when {
                                        !isSignedIn && rescheduleAppointment != null -> "Sign in to update"
                                        !isSignedIn -> "Sign in to continue"
                                        rescheduleAppointment != null -> "Update booking"
                                        else -> "Continue to payment"
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
    ) { paddingValues ->
        if (clinic != null && slot == null && !state.isLoadingSlots) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
            ) {
                item {
                    EmptyPanel(
                        title = "Slot gone",
                        body = "Pick another slot.",
                    )
                }
            }
            return@Scaffold
        }

        if (clinic == null || slot == null || draft == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 320.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "Loading review",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            item {
                BookingReviewHeader(
                    clinicName = clinic.name,
                    appointmentTime = slot.startTime,
                    address = clinic.address,
                    phone = clinic.phone,
                    isReschedule = rescheduleAppointment != null,
                    onChangeTime = onBack,
                )
            }

            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            item {
                SectionHeader(
                    title = "Visit type",
                    supporting = "Choose the reason for this visit.",
                )
            }

            item {
                VisitTypeChips(
                    options = appointmentTypes,
                    selectedType = draft.appointmentType,
                    onSelect = { viewModel.updateBookingDraft(appointmentType = it) },
                )
            }

            item {
                SectionHeader(
                    title = "Common symptoms",
                    supporting = "Tap to add what applies.",
                )
            }

            item {
                SymptomChips(
                    symptoms = symptomChips,
                    reason = draft.reason,
                    onToggle = { symptom ->
                        viewModel.updateBookingDraft(reason = toggleSymptomInReason(draft.reason, symptom))
                    },
                )
            }

            item {
                SectionHeader(
                    title = "Notes",
                    supporting = "Optional. Keep it short.",
                )
            }

            item {
                OutlinedTextField(
                    value = draft.reason,
                    onValueChange = { viewModel.updateBookingDraft(reason = it) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    placeholder = { Text("Add symptoms, exposure details, or questions") },
                )
            }

            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            if (rescheduleAppointment == null) {
                item {
                    EstimatedFeeRow(amount = draft.quotedFee)
                }
            }

            item {
                ReminderRow(
                    checked = draft.hasReminder,
                    onCheckedChange = { viewModel.updateBookingDraft(hasReminder = it) },
                )
            }
        }
    }
}

@Composable
private fun BookingReviewHeader(
    clinicName: String,
    appointmentTime: Long,
    address: String?,
    phone: String?,
    isReschedule: Boolean,
    onChangeTime: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = clinicName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = slotSummary(appointmentTime),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            address?.takeIf(String::isNotBlank)?.let {
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            phone?.takeIf(String::isNotBlank)?.let {
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(
                onClick = onChangeTime,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Text(if (isReschedule) "Change slot" else "Change time")
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, supporting: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = supporting,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VisitTypeChips(options: List<Pair<String, String>>, selectedType: String, onSelect: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = selectedType == value,
                onClick = { onSelect(value) },
                label = { Text(label) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SymptomChips(symptoms: List<String>, reason: String, onToggle: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        symptoms.forEach { symptom ->
            FilterChip(
                selected = isSymptomSelected(reason, symptom),
                onClick = { onToggle(symptom) },
                label = { Text(symptom) },
                border = BorderStroke(
                    1.dp,
                    if (isSymptomSelected(reason, symptom)) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                ),
            )
        }
    }
}

@Composable
private fun ReminderRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Reminder", fontWeight = FontWeight.Medium)
            Text("Keep this visit in your timeline.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun EstimatedFeeRow(amount: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Estimated fee", fontWeight = FontWeight.Medium)
            Text("Due at payment.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text = formatCurrency(amount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private const val SymptomPrefix = "Symptoms:"

private fun isSymptomSelected(reason: String, symptom: String): Boolean = extractSymptoms(reason).contains(symptom)

private fun toggleSymptomInReason(reason: String, symptom: String): String {
    val currentSymptoms = extractSymptoms(reason).toMutableList()
    if (currentSymptoms.contains(symptom)) {
        currentSymptoms.remove(symptom)
    } else {
        currentSymptoms.add(symptom)
    }

    val otherLines = reason
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() && !it.startsWith(SymptomPrefix) }
        .toMutableList()

    if (currentSymptoms.isNotEmpty()) {
        otherLines.add(0, "$SymptomPrefix ${currentSymptoms.joinToString(", ")}")
    }

    return otherLines.joinToString("\n")
}

private fun extractSymptoms(reason: String): List<String> = reason.lineSequence()
    .map { it.trim() }
    .firstOrNull { it.startsWith(SymptomPrefix) }
    ?.removePrefix(SymptomPrefix)
    ?.split(',')
    ?.map { it.trim() }
    ?.filter(String::isNotBlank)
    ?: emptyList()
