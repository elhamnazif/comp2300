package com.group8.comp2300.presentation.ui.screens.medical

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Self Diagnosis Screen
 *
 * TODO: Future Enhancement We want to make this screen reusable so that we can define an STI Object
 * and pass it to the screen and the screen will automatically format the questions as inputs and
 * stuff for us. For now, this is a simple implementation for HIV.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelfDiagnosisScreen(onBack: () -> Unit, onNavigateToBooking: () -> Unit) {
    var unprotectedSex by remember { mutableStateOf<Boolean?>(null) }
    var sharedNeedles by remember { mutableStateOf<Boolean?>(null) }
    var showResultDialog by remember { mutableStateOf(false) }
    var diagnosisResult by remember { mutableStateOf("") }

    fun calculateRisk() {
        diagnosisResult =
            if (unprotectedSex == true || sharedNeedles == true) {
                "High Risk"
            } else {
                "Low Risk"
            }
        showResultDialog = true
    }

    if (showResultDialog) {
        AlertDialog(
            onDismissRequest = { showResultDialog = false },
            title = { Text("Assessment Result") },
            text = {
                Column {
                    Text(
                        text = diagnosisResult,
                        style = MaterialTheme.typography.headlineMedium,
                        color =
                        if (diagnosisResult == "High Risk") {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text =
                        if (diagnosisResult == "High Risk") {
                            "Based on your answers, you may be at higher risk for HIV. We recommend consulting a healthcare provider for further testing."
                        } else {
                            "Based on your answers, your risk appears to be low. However, regular screening is always recommended for sexual health."
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResultDialog = false
                        onNavigateToBooking()
                    },
                ) { Text("Book Appointment") }
            },
            dismissButton = {
                TextButton(onClick = { showResultDialog = false }) { Text("Close") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Self Diagnosis: HIV") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
            Modifier.padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = "HIV Risk Assessment", style = MaterialTheme.typography.headlineSmall)

            Text(
                text = "Please answer the following questions to assess your risk.",
                style = MaterialTheme.typography.bodyMedium,
            )

            // Question 1
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Have you had unprotected sex in the last 3 months?",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { unprotectedSex = true },
                            colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                if (unprotectedSex == true) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme
                                        .surfaceVariant
                                },
                                contentColor =
                                if (unprotectedSex == true) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme
                                        .onSurfaceVariant
                                },
                            ),
                        ) { Text("Yes") }
                        Button(
                            onClick = { unprotectedSex = false },
                            colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                if (unprotectedSex == false) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme
                                        .surfaceVariant
                                },
                                contentColor =
                                if (unprotectedSex == false) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme
                                        .onSurfaceVariant
                                },
                            ),
                        ) { Text("No") }
                    }
                }
            }

            // Question 2
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Have you shared needles or injection equipment?",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { sharedNeedles = true },
                            colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                if (sharedNeedles == true) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme
                                        .surfaceVariant
                                },
                                contentColor =
                                if (sharedNeedles == true) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme
                                        .onSurfaceVariant
                                },
                            ),
                        ) { Text("Yes") }
                        Button(
                            onClick = { sharedNeedles = false },
                            colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                if (sharedNeedles == false) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme
                                        .surfaceVariant
                                },
                                contentColor =
                                if (sharedNeedles == false) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme
                                        .onSurfaceVariant
                                },
                            ),
                        ) { Text("No") }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { calculateRisk() },
                enabled = unprotectedSex != null && sharedNeedles != null,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Get Results") }

            Text(
                text =
                "Note: This is a preliminary self-assessment and does not replace professional medical advice.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
