package com.group8.comp2300.feature.calendar.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.group8.comp2300.core.format.DateFormatter
import com.group8.comp2300.core.ui.accessibility.IndicatorPattern
import com.group8.comp2300.core.ui.accessibility.PatternSwatch
import com.group8.comp2300.core.ui.accessibility.drawIndicatorPattern
import com.group8.comp2300.domain.model.medical.Mood
import com.group8.comp2300.domain.model.medical.MoodType
import comp2300.i18n.generated.resources.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

@Composable
internal fun DailyMoodSummaryCard(moods: List<Mood>) {
    val averageEmoji = moods.averageMoodEmoji()
    val summaryText = if (moods.size == 1) {
        stringResource(Res.string.calendar_mood_one_entry)
    } else {
        stringResource(Res.string.calendar_mood_entry_count, moods.size)
    }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(averageEmoji, style = MaterialTheme.typography.headlineMedium)
                    Column {
                        Text(
                            stringResource(Res.string.calendar_mood_section_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            summaryText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            moods.sortedByDescending { it.timestamp }.forEach { mood ->
                MoodEntryRow(mood = mood)
            }
        }
    }
}

@Composable
private fun MoodEntryRow(mood: Mood) {
    val timeZone = remember { TimeZone.currentSystemDefault() }
    val dateTime = remember(mood.timestamp) {
        Instant.fromEpochMilliseconds(mood.timestamp).toLocalDateTime(timeZone)
    }
    val emoji = mood.moodType.toEmoji()

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Text(emoji, style = MaterialTheme.typography.titleLarge)
                Column {
                    Text(
                        mood.moodType.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    mood.journal?.takeIf(String::isNotBlank)?.let { journal ->
                        Text(
                            journal.take(80) + if (journal.length > 80) "…" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Text(
                DateFormatter.formatTime(dateTime.hour, dateTime.minute),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun MonthlyMoodChart(moodCounts: Map<MoodType, Int>) {
    if (moodCounts.isEmpty()) return

    val total = moodCounts.values.sum().toFloat()
    val moodTypes = MoodType.entries
    val moodColors = listOf(
        Color(0xFFE57373),
        Color(0xFFFFB74D),
        Color(0xFFFFF176),
        Color(0xFFAED581),
        Color(0xFF66BB6A),
    )
    val moodPatterns = listOf(
        IndicatorPattern.GRID,
        IndicatorPattern.DIAGONAL,
        IndicatorPattern.HORIZONTAL,
        IndicatorPattern.DOTS,
        IndicatorPattern.VERTICAL,
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(Res.string.calendar_mood_monthly_trends),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Canvas(
            modifier = Modifier.fillMaxWidth().height(24.dp).clip(RoundedCornerShape(12.dp)),
        ) {
            var currentX = 0f
            moodTypes.forEachIndexed { index, type ->
                val count = moodCounts[type] ?: 0
                if (count > 0) {
                    val width = (count / total) * size.width
                    drawRect(
                        color = moodColors[index],
                        topLeft = Offset(currentX, 0f),
                        size = Size(width, size.height),
                    )
                    drawIndicatorPattern(
                        pattern = moodPatterns[index],
                        color = chartPatternOverlay(moodColors[index]),
                        topLeft = Offset(currentX, 0f),
                        size = Size(width, size.height),
                    )
                    currentX += width
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            moodTypes.forEachIndexed { index, type ->
                val count = moodCounts[type] ?: 0
                if (count > 0) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PatternSwatch(
                            color = moodColors[index],
                            pattern = moodPatterns[index],
                            modifier = Modifier.size(width = 16.dp, height = 10.dp),
                        )
                        Text(
                            "${type.toEmoji()} ${type.displayName}: $count",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun chartPatternOverlay(color: Color): Color = if (relativeBrightness(color) > 0.5f) {
    Color.Black.copy(alpha = 0.22f)
} else {
    Color.White.copy(alpha = 0.4f)
}

private fun relativeBrightness(color: Color): Float =
    (0.299f * color.red) + (0.587f * color.green) + (0.114f * color.blue)

private fun MoodType.toEmoji(): String = when (this) {
    MoodType.VERY_SAD -> "😢"
    MoodType.SAD -> "😕"
    MoodType.NEUTRAL -> "😐"
    MoodType.GOOD -> "🙂"
    MoodType.GREAT -> "🤩"
}

private fun List<Mood>.averageMoodEmoji(): String {
    if (isEmpty()) return "😐"
    val avg = map { it.moodType.toScore() }.average()
    return when {
        avg >= 4.5 -> "🤩"
        avg >= 3.5 -> "🙂"
        avg >= 2.5 -> "😐"
        avg >= 1.5 -> "😕"
        else -> "😢"
    }
}

private fun MoodType.toScore(): Int = when (this) {
    MoodType.VERY_SAD -> 1
    MoodType.SAD -> 2
    MoodType.NEUTRAL -> 3
    MoodType.GOOD -> 4
    MoodType.GREAT -> 5
}
