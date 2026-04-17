package com.group8.comp2300.feature.calendar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.group8.comp2300.core.format.DateFormatter
import com.group8.comp2300.core.ui.accessibility.IndicatorLegendItem
import com.group8.comp2300.core.ui.accessibility.IndicatorPattern
import com.group8.comp2300.core.ui.accessibility.PatternSwatch
import com.group8.comp2300.core.ui.accessibility.StatusIcon
import com.group8.comp2300.domain.model.medical.CalendarOverviewResponse
import com.group8.comp2300.feature.calendar.AdherenceStatus
import com.group8.comp2300.feature.calendar.CalendarDay
import com.group8.comp2300.feature.calendar.StatusVisual
import com.group8.comp2300.feature.calendar.generateCalendarDays
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.ArrowBackW400Outlinedfill1
import comp2300.i18n.generated.resources.*
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CalendarCard(
    baseDate: LocalDate,
    overview: List<CalendarOverviewResponse>,
    selectedDate: CalendarDay?,
    onDayClick: (CalendarDay) -> Unit,
    onMonthChange: (Int, Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    var monthOffset by remember { mutableIntStateOf(0) }
    val displayDate = remember(baseDate, monthOffset) { baseDate.plus(monthOffset, DateTimeUnit.MONTH) }
    val currentMonthChange by rememberUpdatedState(onMonthChange)
    LaunchedEffect(displayDate) {
        currentMonthChange(displayDate.year, displayDate.month.number)
    }
    val overviewMap = remember(overview) { overview.associate { it.date to it.status } }
    val calendarDays =
        remember(displayDate, overviewMap) { generateCalendarDays(displayDate.year, displayDate.month, overviewMap) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { monthOffset-- }) {
                    Icon(
                        Icons.ArrowBackW400Outlinedfill1,
                        contentDescription = stringResource(Res.string.calendar_prev_month_desc),
                    )
                }
                Text(
                    DateFormatter.formatMonthYear(displayDate),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = { monthOffset++ }) {
                    Icon(
                        Icons.ArrowBackW400Outlinedfill1,
                        contentDescription = stringResource(Res.string.calendar_next_month_desc),
                        modifier = Modifier.rotate(180f),
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf(
                    stringResource(Res.string.day_initial_sun),
                    stringResource(Res.string.day_initial_mon),
                    stringResource(Res.string.day_initial_tue),
                    stringResource(Res.string.day_initial_wed),
                    stringResource(Res.string.day_initial_thu),
                    stringResource(Res.string.day_initial_fri),
                    stringResource(Res.string.day_initial_sat),
                ).forEach { day ->
                    Text(
                        day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            calendarDays.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    week.forEach { day ->
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            DayCell(
                                day = day,
                                isSelected = selectedDate?.date == day.date,
                                onClick = { onDayClick(day) },
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            CalendarLegend()
        }
    }
}

@Composable
private fun CalendarLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(
            calendarDayVisual(AdherenceStatus.TAKEN),
            calendarDayVisual(AdherenceStatus.MISSED),
            calendarDayVisual(AdherenceStatus.APPOINTMENT),
        ).forEach { visual ->
            IndicatorLegendItem(
                label = visual.label,
                color = visual.color,
                pattern = visual.pattern,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
internal fun DayCell(
    day: CalendarDay,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val alpha = if (day.isCurrentMonth) 1f else 0.3f
    val borderColor = when {
        isSelected -> Color.Transparent
        day.isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        else -> Color.Transparent
    }
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        day.isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        else -> Color.Transparent
    }
    val contentColor =
        if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    val statusVisual = calendarDayVisual(day.status)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .alpha(alpha),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                day.day.toString(),
                fontWeight = if (day.isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = contentColor,
            )
            if (day.status != AdherenceStatus.NONE && day.isCurrentMonth) {
                Spacer(Modifier.height(4.dp))
                PatternSwatch(
                    color = statusVisual.color,
                    pattern = statusVisual.pattern,
                    modifier = Modifier.size(width = 16.dp, height = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun calendarDayVisual(status: AdherenceStatus): StatusVisual = when (status) {
    AdherenceStatus.TAKEN -> StatusVisual(
        label = stringResource(Res.string.calendar_taken_action),
        color = Color(0xFF4CAF50),
        pattern = IndicatorPattern.DIAGONAL,
        icon = StatusIcon.SUCCESS,
    )
    AdherenceStatus.MISSED -> StatusVisual(
        label = stringResource(Res.string.calendar_skip_action),
        color = MaterialTheme.colorScheme.error,
        pattern = IndicatorPattern.GRID,
        icon = StatusIcon.DANGER,
    )
    AdherenceStatus.APPOINTMENT -> StatusVisual(
        label = stringResource(Res.string.calendar_appointments),
        color = Color(0xFFD4AF37),
        pattern = IndicatorPattern.VERTICAL,
        icon = StatusIcon.DATE,
    )
    AdherenceStatus.NONE -> StatusVisual(
        label = "",
        color = Color.Transparent,
        pattern = IndicatorPattern.SOLID,
        icon = StatusIcon.WARNING,
    )
}
