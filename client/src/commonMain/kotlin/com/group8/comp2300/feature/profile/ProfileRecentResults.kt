package com.group8.comp2300.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.group8.comp2300.core.format.DateFormatter
import com.group8.comp2300.core.ui.components.shimmerEffect
import com.group8.comp2300.domain.model.medical.LabResult
import com.group8.comp2300.feature.medical.shared.labresults.LabResultStatusChip
import comp2300.i18n.generated.resources.Res
import comp2300.i18n.generated.resources.profile_no_results
import comp2300.i18n.generated.resources.profile_recent_results_title
import comp2300.i18n.generated.resources.profile_schedule_next_screening
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun RecentResultsCard(
    results: List<LabResult>,
    isLoading: Boolean,
    onNavigateToLabResults: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(28.dp),
        modifier = modifier.fillMaxWidth(),
        onClick = onNavigateToLabResults,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(Res.string.profile_recent_results_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))
            if (results.isEmpty()) {
                if (isLoading) {
                    repeat(2) {
                        ResultShimmer()
                        Spacer(Modifier.height(12.dp))
                    }
                } else {
                    Text(
                        stringResource(Res.string.profile_no_results),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            } else {
                results.forEachIndexed { index, result ->
                    ResultRow(result)
                    if (index < results.size - 1) {
                        HorizontalDivider(
                            Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text(stringResource(Res.string.profile_schedule_next_screening))
            }
        }
    }
}

@Composable
private fun ResultRow(result: LabResult, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(result.testName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(
                DateFormatter.formatMonthDayYear(result.testDate),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        LabResultStatusChip(result)
    }
}

@Composable
private fun ResultShimmer() {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Box(Modifier.fillMaxWidth(0.6f).height(16.dp).shimmerEffect())
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth(0.3f).height(12.dp).shimmerEffect())
        }
        Box(Modifier.width(80.dp).height(24.dp).clip(RoundedCornerShape(8.dp)).shimmerEffect())
    }
}
