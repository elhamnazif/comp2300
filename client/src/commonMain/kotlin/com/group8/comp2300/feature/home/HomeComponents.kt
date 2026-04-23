package com.group8.comp2300.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.group8.comp2300.app.LocalAppearanceThemeMode
import com.group8.comp2300.core.format.DateFormatter
import com.group8.comp2300.data.local.AppearanceThemeMode
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.CalendarMonthW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.ChevronRightW400Outlined
import com.group8.comp2300.symbols.icons.materialsymbols.icons.DateRangeW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.LocalPharmacyW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.NotificationsW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.ShoppingCartW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.StethoscopeW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.SupportAgentW400Outlinedfill1
import comp2300.client.generated.resources.clinic_photo_waiting_room
import comp2300.i18n.generated.resources.Res
import comp2300.i18n.generated.resources.home_chatbot_description
import comp2300.i18n.generated.resources.home_greeting_afternoon
import comp2300.i18n.generated.resources.home_greeting_evening
import comp2300.i18n.generated.resources.home_greeting_morning
import comp2300.i18n.generated.resources.home_inbox_item_appointment_body
import comp2300.i18n.generated.resources.home_inbox_item_medication_body_many
import comp2300.i18n.generated.resources.home_inbox_item_medication_body_one
import comp2300.i18n.generated.resources.home_inbox_item_notifications_disabled_body
import comp2300.i18n.generated.resources.home_inbox_item_notifications_disabled_title
import comp2300.i18n.generated.resources.home_medication_count_many
import comp2300.i18n.generated.resources.home_medication_count_one
import comp2300.i18n.generated.resources.home_medication_description
import comp2300.i18n.generated.resources.home_menu_chatbot
import comp2300.i18n.generated.resources.home_menu_medication_cabinet
import comp2300.i18n.generated.resources.home_menu_schedules
import comp2300.i18n.generated.resources.home_menu_shop
import comp2300.i18n.generated.resources.home_menu_symptom_check
import comp2300.i18n.generated.resources.home_schedules_description
import comp2300.i18n.generated.resources.home_shop_description
import comp2300.i18n.generated.resources.home_symptom_description
import comp2300.i18n.generated.resources.home_today_adherence
import comp2300.i18n.generated.resources.home_today_medications_due
import comp2300.i18n.generated.resources.home_today_next_appointment
import comp2300.i18n.generated.resources.home_today_title
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import comp2300.client.generated.resources.Res as ClientRes

private val HomeMintAccent = Color(0xFF57C4B3)
private val HomeMintAccentContainer = Color(0xFFE4F6F2)

@Composable
internal fun HomeHero(
    greetingPeriod: GreetingPeriod,
    userFirstName: String?,
    showInboxBadge: Boolean,
    onOpenInbox: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundTint = MaterialTheme.colorScheme.surface.copy(alpha = 0.18f)
    val accentColor = homeAccentColor()
    val headlineName = userFirstName?.trim()?.takeIf(String::isNotBlank)
    val greeting = greetingLabel(greetingPeriod)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(248.dp),
    ) {
        Image(
            painter = painterResource(ClientRes.drawable.clinic_photo_waiting_room),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            backgroundTint,
                            backgroundTint.copy(alpha = 0.06f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.surface,
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "V",
                        color = accentColor,
                        fontSize = 28.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.Light,
                    )
                    Text(
                        text = "Vita",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Surface(
                    onClick = onOpenInbox,
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = Color.White,
                    tonalElevation = 2.dp,
                    shadowElevation = 6.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.NotificationsW400Outlinedfill1,
                            contentDescription = "Inbox",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp),
                        )
                        if (showInboxBadge) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 9.dp, end = 9.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(accentColor),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (headlineName != null) "$greeting," else greeting,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                headlineName?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
internal fun TodaySummaryCard(summary: TodaySummary, modifier: Modifier = Modifier) {
    val accentColor = homeAccentColor()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(Res.string.home_today_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Surface(
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shadowElevation = 6.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                TodayMetricCell(
                    label = stringResource(Res.string.home_today_next_appointment),
                    value = summary.nextAppointment?.let { DateFormatter.formatTime(it.appointmentTime) } ?: "--",
                    supporting = summary.nextAppointment?.title,
                )
                TodayMetricDivider()
                TodayMetricCell(
                    label = stringResource(Res.string.home_today_medications_due),
                    value = summary.medicationsDueCount.toString(),
                )
                TodayMetricDivider()
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.home_today_adherence),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = adherencePercentLabel(summary),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor,
                    )
                    LinearProgressIndicator(
                        progress = { summary.adherenceProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = accentColor,
                        trackColor = accentColor.copy(alpha = 0.2f),
                    )
                }
            }
        }
    }
}

@Composable
internal fun AskVitaRow(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accentColor = homeAccentColor()
    val accentContainerColor = homeAccentContainerColor()
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.55f)),
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActionIcon(
                icon = Icons.SupportAgentW400Outlinedfill1,
                iconTint = accentColor,
                accentColor = accentContainerColor,
                modifier = Modifier.size(44.dp),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(Res.string.home_menu_chatbot),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(Res.string.home_chatbot_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.ChevronRightW400Outlined,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
internal fun ActionList(
    activeMedicationCount: Int,
    onNavigateToSymptomChecker: () -> Unit,
    onNavigateToMedication: () -> Unit,
    onNavigateToRoutines: () -> Unit,
    onNavigateToShop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HomeActionRow(
            icon = Icons.StethoscopeW400Outlinedfill1,
            title = stringResource(Res.string.home_menu_symptom_check),
            description = stringResource(Res.string.home_symptom_description),
            onClick = onNavigateToSymptomChecker,
        )
        HomeActionRow(
            icon = Icons.LocalPharmacyW400Outlinedfill1,
            title = stringResource(Res.string.home_menu_medication_cabinet),
            description = medicationDescription(activeMedicationCount),
            onClick = onNavigateToMedication,
        )
        HomeActionRow(
            icon = Icons.DateRangeW400Outlinedfill1,
            title = stringResource(Res.string.home_menu_schedules),
            description = stringResource(Res.string.home_schedules_description),
            onClick = onNavigateToRoutines,
        )
        HomeActionRow(
            icon = Icons.ShoppingCartW400Outlinedfill1,
            title = stringResource(Res.string.home_menu_shop),
            description = stringResource(Res.string.home_shop_description),
            onClick = onNavigateToShop,
        )
    }
}

@Composable
internal fun InboxSection(title: String, items: List<HomeInboxItem>, onItemClick: (HomeInboxAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    InboxRow(
                        item = item,
                        onClick = { onItemClick(item.action) },
                    )
                    if (index != items.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 18.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun HomeInlineMessage(
    title: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(12.dp))
            TextButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
internal fun HomeCenteredMessage(
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    buttonLabel: String? = null,
    onAction: (() -> Unit)? = null,
    showLoading: Boolean = false,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 360.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (showLoading) {
                CircularProgressIndicator()
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            body?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            if (buttonLabel != null && onAction != null) {
                Button(onClick = onAction) {
                    Text(buttonLabel)
                }
            }
        }
    }
}

@Composable
private fun HomeActionRow(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentColor = homeAccentColor()
    val accentContainerColor = homeAccentContainerColor()
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActionIcon(
                icon = icon,
                iconTint = accentColor,
                accentColor = accentContainerColor,
                modifier = Modifier.size(50.dp),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.ChevronRightW400Outlined,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun InboxRow(item: HomeInboxItem, onClick: () -> Unit) {
    val icon = when (item) {
        is HomeInboxItem.NotificationAlert -> Icons.NotificationsW400Outlinedfill1
        is HomeInboxItem.MedicationAttention -> Icons.LocalPharmacyW400Outlinedfill1
        is HomeInboxItem.AppointmentUpdate -> Icons.CalendarMonthW400Outlinedfill1
    }
    val title = when (item) {
        is HomeInboxItem.NotificationAlert -> stringResource(Res.string.home_inbox_item_notifications_disabled_title)
        is HomeInboxItem.MedicationAttention -> item.routineName
        is HomeInboxItem.AppointmentUpdate -> item.title
    }
    val body = when (item) {
        is HomeInboxItem.NotificationAlert -> stringResource(Res.string.home_inbox_item_notifications_disabled_body)

        is HomeInboxItem.MedicationAttention -> {
            if (item.dueCount == 1) {
                stringResource(Res.string.home_inbox_item_medication_body_one)
            } else {
                stringResource(Res.string.home_inbox_item_medication_body_many, item.dueCount)
            }
        }

        is HomeInboxItem.AppointmentUpdate -> stringResource(
            Res.string.home_inbox_item_appointment_body,
            DateFormatter.formatTime(item.timestampMs),
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActionIcon(
            icon = icon,
            iconTint = MaterialTheme.colorScheme.primary,
            accentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            modifier = Modifier.size(42.dp),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.ChevronRightW400Outlined,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun RowScope.TodayMetricCell(label: String, value: String, supporting: String? = null) {
    val accentColor = homeAccentColor()
    Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (supporting == null) accentColor else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        supporting?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TodayMetricDivider() {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
    )
}

@Composable
private fun ActionIcon(icon: ImageVector, iconTint: Color, accentColor: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = CircleShape,
        color = accentColor,
        modifier = modifier,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun greetingLabel(period: GreetingPeriod): String = when (period) {
    GreetingPeriod.MORNING -> stringResource(Res.string.home_greeting_morning)
    GreetingPeriod.AFTERNOON -> stringResource(Res.string.home_greeting_afternoon)
    GreetingPeriod.EVENING -> stringResource(Res.string.home_greeting_evening)
}

private fun adherencePercentLabel(summary: TodaySummary): String = "${(summary.adherenceProgress * 100).toInt()}%"

@Composable
private fun medicationDescription(activeMedicationCount: Int): String = when {
    activeMedicationCount <= 0 -> stringResource(Res.string.home_medication_description)
    activeMedicationCount == 1 -> stringResource(Res.string.home_medication_count_one)
    else -> stringResource(Res.string.home_medication_count_many, activeMedicationCount)
}

@Composable
private fun homeAccentColor(): Color = when (LocalAppearanceThemeMode.current) {
    AppearanceThemeMode.MINT -> HomeMintAccent
    AppearanceThemeMode.WALLPAPER -> MaterialTheme.colorScheme.primary
}

@Composable
private fun homeAccentContainerColor(): Color = when (LocalAppearanceThemeMode.current) {
    AppearanceThemeMode.MINT -> HomeMintAccentContainer
    AppearanceThemeMode.WALLPAPER -> MaterialTheme.colorScheme.primaryContainer
}
