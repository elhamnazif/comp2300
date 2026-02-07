@file:Suppress("FunctionName")

package com.group8.comp2300.presentation.ui.screens.medical

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.symbols.icons.materialsymbols.Icons
import com.app.symbols.icons.materialsymbols.icons.*
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * Self Diagnosis Screen
 *
 * TODO: Future Enhancement We want to make this screen reusable so that we can define an STI Object and pass it to the
 *   screen and the screen will automatically format the questions as inputs and stuff for us. For now, this is a simple
 *   implementation for HIV.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelfDiagnosisScreen(onBack: () -> Unit, onNavigateToBooking: () -> Unit, modifier: Modifier = Modifier) {
    var unprotectedSex by remember { mutableStateOf<Boolean?>(null) }
    var sharedNeedles by remember { mutableStateOf<Boolean?>(null) }
    var showResultDialog by remember { mutableStateOf(false) }
    var diagnosisResult by remember { mutableStateOf("") }

    fun calculateRisk() {
        diagnosisResult =
            if (unprotectedSex == true || sharedNeedles == true) {
                "High Risk" // internal key for logic
            } else {
                "Low Risk" // internal key for logic
            }
        showResultDialog = true
    }

    if (showResultDialog) {
        AlertDialog(
            onDismissRequest = { showResultDialog = false },
            title = { Text(stringResource(Res.string.medical_self_diagnosis_result_dialog_title)) },
            text = {
                Column {
                    Text(
                        text =
                        if (diagnosisResult == "High Risk") {
                            stringResource(Res.string.medical_self_diagnosis_risk_high)
                        } else {
                            stringResource(Res.string.medical_self_diagnosis_risk_low)
                        },
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
                            stringResource(Res.string.medical_self_diagnosis_feedback_high)
                        } else {
                            stringResource(Res.string.medical_self_diagnosis_feedback_low)
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
                ) {
                    Text(stringResource(Res.string.medical_self_diagnosis_book_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResultDialog = false }) {
                    Text(stringResource(Res.string.medical_self_diagnosis_close_button))
                }
            },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.medical_self_diagnosis_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.ArrowBackW400Outlinedfill1,
                            contentDescription = stringResource(Res.string.medical_self_diagnosis_back_desc),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(Res.string.medical_self_diagnosis_header),
                style = MaterialTheme.typography.headlineSmall,
            )

            Text(
                text = stringResource(Res.string.medical_self_diagnosis_desc),
                style = MaterialTheme.typography.bodyMedium,
            )

            // Question 1
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(Res.string.medical_self_diagnosis_q1),
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
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                contentColor =
                                if (unprotectedSex == true) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            ),
                        ) {
                            Text("Yes")
                        }
                        Button(
                            onClick = { unprotectedSex = false },
                            colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                if (unprotectedSex == false) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                contentColor =
                                if (unprotectedSex == false) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            ),
                        ) {
                            Text("No")
                        }
                    }
                }
            }

            // Question 2
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(Res.string.medical_self_diagnosis_q2),
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
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                contentColor =
                                if (sharedNeedles == true) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            ),
                        ) {
                            Text("Yes")
                        }
                        Button(
                            onClick = { sharedNeedles = false },
                            colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                if (sharedNeedles == false) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                contentColor =
                                if (sharedNeedles == false) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            ),
                        ) {
                            Text("No")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { calculateRisk() },
                enabled = unprotectedSex != null && sharedNeedles != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.medical_self_diagnosis_submit_button))
            }

            Text(
                text = stringResource(Res.string.medical_self_diagnosis_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
