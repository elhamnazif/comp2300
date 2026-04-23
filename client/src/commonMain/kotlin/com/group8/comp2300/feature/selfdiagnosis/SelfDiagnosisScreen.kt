package com.group8.comp2300.feature.selfdiagnosis

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.group8.comp2300.core.ui.components.AppTopBar
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource

private enum class DiagnosisRisk {
    HIGH,
    LOW,
}

@Composable
fun SelfDiagnosisScreen(onBack: () -> Unit, onNavigateToBooking: () -> Unit, modifier: Modifier = Modifier) {
    var unprotectedSex by remember { mutableStateOf<Boolean?>(null) }
    var sharedNeedles by remember { mutableStateOf<Boolean?>(null) }
    var showResultDialog by remember { mutableStateOf(false) }
    var diagnosisRisk by remember { mutableStateOf(DiagnosisRisk.LOW) }

    fun calculateRisk() {
        diagnosisRisk = if (unprotectedSex == true || sharedNeedles == true) DiagnosisRisk.HIGH else DiagnosisRisk.LOW
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
                        if (diagnosisRisk == DiagnosisRisk.HIGH) {
                            stringResource(Res.string.medical_self_diagnosis_risk_high)
                        } else {
                            stringResource(Res.string.medical_self_diagnosis_risk_low)
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        color =
                        if (diagnosisRisk == DiagnosisRisk.HIGH) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text =
                        if (diagnosisRisk == DiagnosisRisk.HIGH) {
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
            AppTopBar(
                title = { Text(stringResource(Res.string.medical_self_diagnosis_title)) },
                onBackClick = onBack,
                backContentDescription = stringResource(Res.string.medical_self_diagnosis_back_desc),
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

            DiagnosisQuestionCard(
                question = stringResource(Res.string.medical_self_diagnosis_q1),
                answer = unprotectedSex,
                onAnswerChange = { unprotectedSex = it },
            )

            DiagnosisQuestionCard(
                question = stringResource(Res.string.medical_self_diagnosis_q2),
                answer = sharedNeedles,
                onAnswerChange = { sharedNeedles = it },
            )

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

@Composable
private fun DiagnosisQuestionCard(question: String, answer: Boolean?, onAnswerChange: (Boolean) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = question,
                style = MaterialTheme.typography.titleMedium,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DiagnosisAnswerButton(
                    label = stringResource(Res.string.common_yes),
                    selected = answer == true,
                    onClick = { onAnswerChange(true) },
                )
                DiagnosisAnswerButton(
                    label = stringResource(Res.string.common_no),
                    selected = answer == false,
                    onClick = { onAnswerChange(false) },
                )
            }
        }
    }
}

@Composable
private fun DiagnosisAnswerButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        ),
    ) {
        Text(label)
    }
}
