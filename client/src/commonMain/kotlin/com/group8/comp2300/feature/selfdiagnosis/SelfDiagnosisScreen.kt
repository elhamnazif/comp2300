package com.group8.comp2300.feature.selfdiagnosis

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.group8.comp2300.core.ui.accessibility.AccessibleStatusChip
import com.group8.comp2300.core.ui.accessibility.StatusIcon
import com.group8.comp2300.core.ui.components.AppTopBar
import comp2300.i18n.generated.resources.Res
import comp2300.i18n.generated.resources.common_no
import comp2300.i18n.generated.resources.common_yes
import comp2300.i18n.generated.resources.medical_self_diagnosis_back_desc
import comp2300.i18n.generated.resources.medical_self_diagnosis_book_button
import comp2300.i18n.generated.resources.medical_self_diagnosis_desc
import comp2300.i18n.generated.resources.medical_self_diagnosis_disclaimer
import comp2300.i18n.generated.resources.medical_self_diagnosis_done_button
import comp2300.i18n.generated.resources.medical_self_diagnosis_feedback_high
import comp2300.i18n.generated.resources.medical_self_diagnosis_feedback_low
import comp2300.i18n.generated.resources.medical_self_diagnosis_feedback_medium
import comp2300.i18n.generated.resources.medical_self_diagnosis_find_care_button
import comp2300.i18n.generated.resources.medical_self_diagnosis_footer_note
import comp2300.i18n.generated.resources.medical_self_diagnosis_header
import comp2300.i18n.generated.resources.medical_self_diagnosis_q1
import comp2300.i18n.generated.resources.medical_self_diagnosis_q2
import comp2300.i18n.generated.resources.medical_self_diagnosis_q3
import comp2300.i18n.generated.resources.medical_self_diagnosis_q4
import comp2300.i18n.generated.resources.medical_self_diagnosis_reason_heading
import comp2300.i18n.generated.resources.medical_self_diagnosis_reason_needle_risk
import comp2300.i18n.generated.resources.medical_self_diagnosis_reason_overdue_test
import comp2300.i18n.generated.resources.medical_self_diagnosis_reason_recent_exposure
import comp2300.i18n.generated.resources.medical_self_diagnosis_reason_recent_sex_risk
import comp2300.i18n.generated.resources.medical_self_diagnosis_reason_recent_test
import comp2300.i18n.generated.resources.medical_self_diagnosis_reset_button
import comp2300.i18n.generated.resources.medical_self_diagnosis_result_dialog_title
import comp2300.i18n.generated.resources.medical_self_diagnosis_risk_high
import comp2300.i18n.generated.resources.medical_self_diagnosis_risk_low
import comp2300.i18n.generated.resources.medical_self_diagnosis_risk_medium
import comp2300.i18n.generated.resources.medical_self_diagnosis_submit_button
import comp2300.i18n.generated.resources.medical_self_diagnosis_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private enum class AssessmentOutcome {
    URGENT,
    TEST_SOON,
    ROUTINE,
}

private enum class AssessmentReason(val labelRes: StringResource) {
    RECENT_EXPOSURE(Res.string.medical_self_diagnosis_reason_recent_exposure),
    RECENT_SEX_RISK(Res.string.medical_self_diagnosis_reason_recent_sex_risk),
    NEEDLE_RISK(Res.string.medical_self_diagnosis_reason_needle_risk),
    OVERDUE_TEST(Res.string.medical_self_diagnosis_reason_overdue_test),
    RECENT_TEST(Res.string.medical_self_diagnosis_reason_recent_test),
}

private data class AssessmentResult(
    val outcome: AssessmentOutcome,
    val reasons: List<AssessmentReason>,
)

@Composable
fun SelfDiagnosisScreen(onBack: () -> Unit, onNavigateToBooking: () -> Unit, modifier: Modifier = Modifier) {
    var recentExposure by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var recentSexRisk by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var sharedNeedles by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var testedRecently by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var showResult by rememberSaveable { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val answers = listOf(recentExposure, recentSexRisk, sharedNeedles, testedRecently)
    val answeredCount = answers.count { it != null }
    val canReview = recentExposure == true || answers.all { it != null }
    val result = if (showResult && canReview) {
        buildAssessmentResult(
            recentExposure = recentExposure,
            recentSexRisk = recentSexRisk,
            sharedNeedles = sharedNeedles,
            testedRecently = testedRecently,
        )
    } else {
        null
    }

    fun updateAnswer(onUpdate: () -> Unit) {
        onUpdate()
        showResult = false
    }

    fun resetAssessment() {
        recentExposure = null
        recentSexRisk = null
        sharedNeedles = null
        testedRecently = null
        showResult = false
    }

    LaunchedEffect(result?.outcome) {
        if (result != null) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        topBar = {
            AppTopBar(
                title = { Text(stringResource(Res.string.medical_self_diagnosis_title)) },
                onBackClick = onBack,
                backContentDescription = stringResource(Res.string.medical_self_diagnosis_back_desc),
            )
        },
        bottomBar = {
            AssessmentBottomBar(
                result = result,
                canReview = canReview,
                onReview = { showResult = true },
                onReset = ::resetAssessment,
                onDone = onBack,
                onNavigateToBooking = onNavigateToBooking,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AssessmentIntroCard(
                answeredCount = answeredCount,
                totalQuestions = 4,
                canReviewEarly = recentExposure == true,
            )

            DiagnosisQuestionPanel(
                index = 1,
                question = stringResource(Res.string.medical_self_diagnosis_q1),
                answer = recentExposure,
                onAnswerChange = { value ->
                    updateAnswer { recentExposure = value }
                },
            )

            DiagnosisQuestionPanel(
                index = 2,
                question = stringResource(Res.string.medical_self_diagnosis_q2),
                answer = recentSexRisk,
                onAnswerChange = { value ->
                    updateAnswer { recentSexRisk = value }
                },
            )

            DiagnosisQuestionPanel(
                index = 3,
                question = stringResource(Res.string.medical_self_diagnosis_q3),
                answer = sharedNeedles,
                onAnswerChange = { value ->
                    updateAnswer { sharedNeedles = value }
                },
            )

            DiagnosisQuestionPanel(
                index = 4,
                question = stringResource(Res.string.medical_self_diagnosis_q4),
                answer = testedRecently,
                onAnswerChange = { value ->
                    updateAnswer { testedRecently = value }
                },
            )

            AnimatedVisibility(visible = result != null) {
                result?.let { assessment ->
                    DiagnosisResultPanel(
                        result = assessment,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            Text(
                text = stringResource(Res.string.medical_self_diagnosis_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun buildAssessmentResult(
    recentExposure: Boolean?,
    recentSexRisk: Boolean?,
    sharedNeedles: Boolean?,
    testedRecently: Boolean?,
): AssessmentResult {
    val reasons = buildList {
        if (recentExposure == true) add(AssessmentReason.RECENT_EXPOSURE)
        if (recentSexRisk == true) add(AssessmentReason.RECENT_SEX_RISK)
        if (sharedNeedles == true) add(AssessmentReason.NEEDLE_RISK)
        if (testedRecently == false) add(AssessmentReason.OVERDUE_TEST)
        if (testedRecently == true && recentExposure != true && recentSexRisk != true && sharedNeedles != true) {
            add(AssessmentReason.RECENT_TEST)
        }
    }

    val outcome = when {
        recentExposure == true -> AssessmentOutcome.URGENT
        recentSexRisk == true || sharedNeedles == true || testedRecently == false -> AssessmentOutcome.TEST_SOON
        else -> AssessmentOutcome.ROUTINE
    }

    return AssessmentResult(
        outcome = outcome,
        reasons = reasons,
    )
}

@Composable
private fun AssessmentBottomBar(
    result: AssessmentResult?,
    canReview: Boolean,
    onReview: () -> Unit,
    onReset: () -> Unit,
    onDone: () -> Unit,
    onNavigateToBooking: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (result == null) {
                Button(
                    onClick = onReview,
                    enabled = canReview,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                ) {
                    Text(stringResource(Res.string.medical_self_diagnosis_submit_button))
                }
            } else {
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp),
                ) {
                    Text(stringResource(Res.string.medical_self_diagnosis_reset_button))
                }

                Button(
                    onClick = {
                        when (result.outcome) {
                            AssessmentOutcome.ROUTINE -> onDone()
                            AssessmentOutcome.URGENT,
                            AssessmentOutcome.TEST_SOON,
                            -> onNavigateToBooking()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp),
                ) {
                    Text(
                        text = when (result.outcome) {
                            AssessmentOutcome.URGENT -> stringResource(Res.string.medical_self_diagnosis_find_care_button)
                            AssessmentOutcome.TEST_SOON -> stringResource(Res.string.medical_self_diagnosis_book_button)
                            AssessmentOutcome.ROUTINE -> stringResource(Res.string.medical_self_diagnosis_done_button)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AssessmentIntroCard(
    answeredCount: Int,
    totalQuestions: Int,
    canReviewEarly: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.medical_self_diagnosis_header),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(Res.string.medical_self_diagnosis_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = CircleShape,
                    modifier = Modifier.padding(start = 12.dp),
                ) {
                    Text(
                        text = "$answeredCount/$totalQuestions",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                }
            }

            LinearProgressIndicator(
                progress = { answeredCount / totalQuestions.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )

            if (canReviewEarly) {
                Text(
                    text = stringResource(Res.string.medical_self_diagnosis_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DiagnosisQuestionPanel(
    index: Int,
    question: String,
    answer: Boolean?,
    onAnswerChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = index.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Text(
                    text = question,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DiagnosisAnswerChip(
                    label = stringResource(Res.string.common_yes),
                    selected = answer == true,
                    onClick = { onAnswerChange(true) },
                    modifier = Modifier.weight(1f),
                )
                DiagnosisAnswerChip(
                    label = stringResource(Res.string.common_no),
                    selected = answer == false,
                    onClick = { onAnswerChange(false) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DiagnosisAnswerChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        label = {
            Text(
                text = label,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        },
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            selectedBorderColor = MaterialTheme.colorScheme.primary,
        ),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    )
}

@Composable
private fun DiagnosisResultPanel(result: AssessmentResult, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(Res.string.medical_self_diagnosis_result_dialog_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            AccessibleStatusChip(
                label = result.statusLabel(),
                icon = result.statusIcon(),
                containerColor = result.statusContainerColor(),
                contentColor = result.statusContentColor(),
            )

            Text(
                text = result.feedbackText(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (result.reasons.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Text(
                    text = stringResource(Res.string.medical_self_diagnosis_reason_heading),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    result.reasons.forEach { reason ->
                        AssessmentReasonRow(reason = reason)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Text(
                text = stringResource(Res.string.medical_self_diagnosis_footer_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AssessmentReasonRow(reason: AssessmentReason, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = CircleShape,
            modifier = Modifier.padding(top = 3.dp),
        ) {
            Box(modifier = Modifier.size(8.dp))
        }
        Text(
            text = stringResource(reason.labelRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AssessmentResult.statusLabel(): String = when (outcome) {
    AssessmentOutcome.URGENT -> stringResource(Res.string.medical_self_diagnosis_risk_high)
    AssessmentOutcome.TEST_SOON -> stringResource(Res.string.medical_self_diagnosis_risk_medium)
    AssessmentOutcome.ROUTINE -> stringResource(Res.string.medical_self_diagnosis_risk_low)
}

@Composable
private fun AssessmentResult.feedbackText(): String = when (outcome) {
    AssessmentOutcome.URGENT -> stringResource(Res.string.medical_self_diagnosis_feedback_high)
    AssessmentOutcome.TEST_SOON -> stringResource(Res.string.medical_self_diagnosis_feedback_medium)
    AssessmentOutcome.ROUTINE -> stringResource(Res.string.medical_self_diagnosis_feedback_low)
}

@Composable
private fun AssessmentResult.statusIcon(): StatusIcon = when (outcome) {
    AssessmentOutcome.URGENT -> StatusIcon.DANGER
    AssessmentOutcome.TEST_SOON -> StatusIcon.WARNING
    AssessmentOutcome.ROUTINE -> StatusIcon.SUCCESS
}

@Composable
private fun AssessmentResult.statusContainerColor() = when (outcome) {
    AssessmentOutcome.URGENT -> MaterialTheme.colorScheme.errorContainer
    AssessmentOutcome.TEST_SOON -> MaterialTheme.colorScheme.tertiaryContainer
    AssessmentOutcome.ROUTINE -> MaterialTheme.colorScheme.secondaryContainer
}

@Composable
private fun AssessmentResult.statusContentColor() = when (outcome) {
    AssessmentOutcome.URGENT -> MaterialTheme.colorScheme.onErrorContainer
    AssessmentOutcome.TEST_SOON -> MaterialTheme.colorScheme.onTertiaryContainer
    AssessmentOutcome.ROUTINE -> MaterialTheme.colorScheme.onSecondaryContainer
}
