package com.group8.comp2300.feature.booking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.group8.comp2300.core.format.DateFormatter
import com.group8.comp2300.core.ui.components.AppTopBar
import com.group8.comp2300.domain.model.medical.Appointment
import com.group8.comp2300.domain.model.medical.Clinic
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock
import kotlin.time.Instant

@Composable
fun BookingHistoryScreen(
    highlightedAppointmentId: String? = null,
    onBack: () -> Unit,
    onReschedule: (Appointment) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookingViewModel = koinViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val snackbarHostState = remember { SnackbarHostState() }
    var cancelTarget by remember { mutableStateOf<Appointment?>(null) }
    val selectedAppointment = state.selectedManagedAppointmentId?.let(viewModel::getManagedAppointment)
    val clinicsById = remember(state.clinics) { state.clinics.associateBy(Clinic::id) }
    val (upcomingAppointments, historyAppointments) = remember(state.managedAppointments) {
        state.managedAppointments.partition(::isUpcomingActionableAppointment)
    }

    LaunchedEffect(highlightedAppointmentId) {
        viewModel.loadBookingHistory(refreshFromServer = true)
        highlightedAppointmentId?.let(viewModel::showManagedAppointment)
    }

    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearBookingError()
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = { Text("My bookings") },
                onBackClick = onBack,
                backContentDescription = "Back",
            )
        },
    ) { paddingValues ->
        when {
            state.isLoadingAppointments -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            state.managedAppointments.isEmpty() -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                ) {
                    item {
                        EmptyPanel(
                            title = "No bookings",
                            body = "Book a clinic to see it here.",
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    if (upcomingAppointments.isNotEmpty()) {
                        item {
                            BookingHistorySectionHeader(
                                title = "Upcoming",
                                supporting = "Manage your next clinic visits.",
                            )
                        }
                        items(upcomingAppointments, key = { it.id }) { appointment ->
                            BookingHistoryCard(
                                appointment = appointment,
                                clinic = appointment.clinicId?.let(clinicsById::get),
                                isMutating = state.isMutatingAppointment,
                                onDetails = { viewModel.showManagedAppointment(appointment.id) },
                                onReschedule = { onReschedule(appointment) },
                                onCancel = { cancelTarget = appointment },
                            )
                        }
                    }

                    if (historyAppointments.isNotEmpty()) {
                        item {
                            BookingHistorySectionHeader(
                                title = "History",
                                supporting = "Past and cancelled bookings.",
                            )
                        }
                        items(historyAppointments, key = { it.id }) { appointment ->
                            BookingHistoryCard(
                                appointment = appointment,
                                clinic = appointment.clinicId?.let(clinicsById::get),
                                isMutating = false,
                                onDetails = { viewModel.showManagedAppointment(appointment.id) },
                                onReschedule = null,
                                onCancel = null,
                            )
                        }
                    }

                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
    }

    if (selectedAppointment != null) {
        ModalBottomSheet(
            onDismissRequest = viewModel::hideManagedAppointment,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            AppointmentManagementSheet(
                appointment = selectedAppointment,
                clinic = selectedAppointment.clinicId?.let(clinicsById::get),
                isMutating = state.isMutatingAppointment,
                onReschedule = if (isUpcomingActionableAppointment(selectedAppointment)) {
                    { onReschedule(selectedAppointment) }
                } else {
                    null
                },
                onCancel = if (isUpcomingActionableAppointment(selectedAppointment)) {
                    { cancelTarget = selectedAppointment }
                } else {
                    null
                },
                onClose = viewModel::hideManagedAppointment,
            )
        }
    }

    cancelTarget?.let { appointment ->
        AlertDialog(
            onDismissRequest = { cancelTarget = null },
            title = { Text("Cancel booking") },
            text = { Text("This frees the slot.") },
            confirmButton = {
                Button(
                    enabled = !state.isMutatingAppointment,
                    onClick = {
                        viewModel.cancelAppointment(appointment.id)
                        cancelTarget = null
                    },
                ) {
                    Text("Cancel booking")
                }
            },
            dismissButton = {
                TextButton(onClick = { cancelTarget = null }) {
                    Text("Keep")
                }
            },
        )
    }
}

@Composable
private fun BookingHistorySectionHeader(title: String, supporting: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = supporting,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BookingHistoryCard(
    appointment: Appointment,
    clinic: Clinic?,
    isMutating: Boolean,
    onDetails: () -> Unit,
    onReschedule: (() -> Unit)?,
    onCancel: (() -> Unit)?,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text =
                        clinic?.name ?: appointment.title.removePrefix("Appointment at ").ifBlank {
                            "Clinic visit"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = bookingDateSummary(appointment.appointmentTime),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                BookingStatusPill(status = appointment.status)
            }

            clinic?.address?.takeIf(String::isNotBlank)?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDetails) {
                    Text("Details")
                }
                onReschedule?.let {
                    TextButton(
                        onClick = it,
                        enabled = !isMutating,
                    ) {
                        Text("Reschedule")
                    }
                }
                onCancel?.let {
                    TextButton(
                        onClick = it,
                        enabled = !isMutating,
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
private fun AppointmentManagementSheet(
    appointment: Appointment,
    clinic: Clinic?,
    isMutating: Boolean,
    onReschedule: (() -> Unit)?,
    onCancel: (() -> Unit)?,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = clinic?.name ?: appointment.title.removePrefix("Appointment at ").ifBlank { "Clinic visit" },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = bookingDateSummary(appointment.appointmentTime),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BookingStatusPill(status = appointment.status)
        clinic?.address?.takeIf(String::isNotBlank)?.let {
            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(appointmentTypeLabel(appointment.appointmentType), fontWeight = FontWeight.Medium)
        appointment.notes?.takeIf(String::isNotBlank)?.let {
            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        onReschedule?.let {
            Button(
                onClick = it,
                enabled = !isMutating,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Reschedule")
            }
        }
        onCancel?.let {
            OutlinedButton(
                onClick = it,
                enabled = !isMutating,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Cancel booking")
            }
        }
        TextButton(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Close")
        }
    }
}

@Composable
private fun BookingStatusPill(status: String) {
    val (containerColor, contentColor, label) = when (status) {
        "CANCELLED" -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Cancelled",
        )

        else -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "Confirmed",
        )
    }

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.large,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun isUpcomingActionableAppointment(appointment: Appointment): Boolean = appointment.status != "CANCELLED" &&
    appointment.appointmentTime > Clock.System.now().toEpochMilliseconds()

private fun bookingDateSummary(timestampMs: Long): String {
    val dateTime = Instant.fromEpochMilliseconds(timestampMs)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return "${DateFormatter.formatDayMonthYear(
        dateTime.date,
    )} at ${DateFormatter.formatTime(dateTime.hour, dateTime.minute)}"
}
