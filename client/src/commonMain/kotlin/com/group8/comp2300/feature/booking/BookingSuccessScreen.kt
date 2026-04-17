package com.group8.comp2300.feature.booking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.group8.comp2300.core.ui.components.AppTopBar
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.CheckCircleW400Outlinedfill1
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BookingSuccessScreen(
    clinicId: String,
    appointmentId: String,
    appointmentTime: Long,
    onBack: () -> Unit,
    onViewCalendar: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookingViewModel = koinViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val clinic = state.selectedClinic?.takeIf { it.id == clinicId } ?: viewModel.getClinic(clinicId)
    val appointment = viewModel.getLastBookedAppointment(appointmentId)

    LaunchedEffect(clinicId) {
        if (clinic == null) {
            viewModel.loadClinicDetails(clinicId)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = { Text("Confirmed") },
                onBackClick = onBack,
                backContentDescription = "Back",
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.CheckCircleW400Outlinedfill1,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text("Booking confirmed", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(slotSummary(appointmentTime), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Clinic", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(clinic?.name ?: "Clinic booked", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        clinic?.address?.takeIf(String::isNotBlank)?.let {
                            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        appointment?.appointmentType?.let {
                            Text(appointmentTypeLabel(it), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        appointment?.bookingId?.let {
                            Text("Reference: $it", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = onViewCalendar,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("View calendar")
                }
            }

            item {
                TextButton(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Done")
                }
            }
        }
    }
}

private fun appointmentTypeLabel(value: String): String = when (value) {
    "STI_TESTING" -> "STI testing"
    "SYMPTOMS" -> "Symptoms"
    "CONTRACEPTION" -> "Contraception"
    "PREP_PEP" -> "PrEP / PEP"
    "FOLLOW_UP" -> "Follow-up"
    else -> value.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase)
}
