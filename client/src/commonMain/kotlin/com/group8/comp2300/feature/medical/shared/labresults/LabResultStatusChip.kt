package com.group8.comp2300.feature.medical.shared.labresults

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.group8.comp2300.core.ui.accessibility.AccessibleStatusChip
import com.group8.comp2300.core.ui.accessibility.StatusIcon
import com.group8.comp2300.domain.model.medical.LabResult
import com.group8.comp2300.domain.model.medical.LabStatus
import comp2300.i18n.generated.resources.Res
import comp2300.i18n.generated.resources.lab_status_inconclusive
import comp2300.i18n.generated.resources.lab_status_negative
import comp2300.i18n.generated.resources.lab_status_pending
import comp2300.i18n.generated.resources.lab_status_positive
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private data class LabResultStatusPresentation(
    val containerColor: Color,
    val contentColor: Color,
    val statusRes: StringResource,
    val icon: StatusIcon,
)

@Composable
private fun labResultStatusPresentation(result: LabResult): LabResultStatusPresentation {
    val containerColor = if (result.isPositive) MaterialTheme.colorScheme.errorContainer else Color(0xFFE8F5E9)
    val contentColor = if (result.isPositive) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
    val (statusRes, icon) = when (result.status) {
        LabStatus.PENDING -> Res.string.lab_status_pending to StatusIcon.DATE
        LabStatus.NEGATIVE -> Res.string.lab_status_negative to StatusIcon.SUCCESS
        LabStatus.POSITIVE -> Res.string.lab_status_positive to StatusIcon.DANGER
        LabStatus.INCONCLUSIVE -> Res.string.lab_status_inconclusive to StatusIcon.WARNING
    }
    return LabResultStatusPresentation(containerColor, contentColor, statusRes, icon)
}

@Composable
fun LabResultStatusChip(result: LabResult) {
    val (containerColor, contentColor, statusRes, icon) = labResultStatusPresentation(result)
    AccessibleStatusChip(
        label = stringResource(statusRes),
        icon = icon,
        containerColor = containerColor,
        contentColor = contentColor,
    )
}
