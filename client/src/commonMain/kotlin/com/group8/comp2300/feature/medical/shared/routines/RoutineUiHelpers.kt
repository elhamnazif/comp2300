package com.group8.comp2300.feature.medical.shared.routines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.NotificationsW400Outlinedfill1
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource

fun reminderMetaLabel(offsets: List<Int>): String? {
    val unique = offsets.sorted().distinct()
    return when {
        unique.isEmpty() -> null
        unique == listOf(0) -> null
        unique.size == 1 -> "${unique.first()}m"
        else -> "${unique.size}x"
    }
}

@Composable
fun weekdaySummary(daysOfWeek: List<Int>): String {
    val weekdayLabels = mapOf(
        0 to stringResource(Res.string.common_day_sun_short),
        1 to stringResource(Res.string.common_day_mon_short),
        2 to stringResource(Res.string.common_day_tue_short),
        3 to stringResource(Res.string.common_day_wed_short),
        4 to stringResource(Res.string.common_day_thu_short),
        5 to stringResource(Res.string.common_day_fri_short),
        6 to stringResource(Res.string.common_day_sat_short),
    )
    return daysOfWeek.sorted().joinToString { day -> weekdayLabels[day].orEmpty() }
}

@Composable
fun ReminderIndicator(
    reminderOffsetsMins: List<Int>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
    textFontWeight: FontWeight? = null,
) {
    if (reminderOffsetsMins.isEmpty()) return

    val reminderMeta = reminderMetaLabel(reminderOffsetsMins)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.NotificationsW400Outlinedfill1,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
            modifier = iconModifier,
        )
        reminderMeta?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = textFontWeight,
            )
        }
    }
}
