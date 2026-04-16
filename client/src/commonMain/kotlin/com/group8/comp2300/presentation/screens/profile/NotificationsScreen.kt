package com.group8.comp2300.presentation.screens.profile

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.*
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun NotificationsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    var discreetMode by remember { mutableStateOf(true) }
    var appointmentReminders by remember { mutableStateOf(true) }
    var testResults by remember { mutableStateOf(true) }
    var testReminders by remember { mutableStateOf(true) }
    var educationContent by remember { mutableStateOf(false) }
    var productDeals by remember { mutableStateOf(false) }

    SettingsDetailScaffold(
        title = stringResource(Res.string.notifications_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        item {
            SettingsSection(title = stringResource(Res.string.notifications_general_title)) {
                SettingsToggleRow(
                    icon = Icons.NotificationsW400Outlinedfill1,
                    title = stringResource(Res.string.notifications_discreet_mode_title),
                    description = stringResource(Res.string.notifications_discreet_mode_desc),
                    checked = discreetMode,
                    index = 0,
                    total = 1,
                    onCheckedChange = { discreetMode = it },
                )
            }
        }
        item {
            SettingsSection(title = stringResource(Res.string.notifications_health_reminders_title)) {
                SettingsToggleRow(
                    icon = Icons.CalendarMonthW400Outlinedfill1,
                    title = stringResource(Res.string.notifications_appointment_reminders_title),
                    description = stringResource(Res.string.notifications_appointment_reminders_desc),
                    checked = appointmentReminders,
                    index = 0,
                    total = 3,
                    onCheckedChange = { appointmentReminders = it },
                )
                SettingsToggleRow(
                    icon = Icons.CheckCircleW400Outlinedfill1,
                    title = stringResource(Res.string.notifications_test_results_title),
                    description = stringResource(Res.string.notifications_test_results_desc),
                    checked = testResults,
                    index = 1,
                    total = 3,
                    onCheckedChange = { testResults = it },
                )
                SettingsToggleRow(
                    icon = Icons.DateRangeW400Outlinedfill1,
                    title = stringResource(Res.string.notifications_testing_reminders_title),
                    description = stringResource(Res.string.notifications_testing_reminders_desc),
                    checked = testReminders,
                    index = 2,
                    total = 3,
                    onCheckedChange = { testReminders = it },
                )
            }
        }
        item {
            SettingsSection(title = stringResource(Res.string.notifications_content_updates_title)) {
                SettingsToggleRow(
                    icon = Icons.ArticleW400Outlinedfill1,
                    title = stringResource(Res.string.notifications_educational_content_title),
                    description = stringResource(Res.string.notifications_educational_content_desc),
                    checked = educationContent,
                    index = 0,
                    total = 2,
                    onCheckedChange = { educationContent = it },
                )
                SettingsToggleRow(
                    icon = Icons.SendW400Outlinedfill1,
                    title = stringResource(Res.string.notifications_product_deals_title),
                    description = stringResource(Res.string.notifications_product_deals_desc),
                    checked = productDeals,
                    index = 1,
                    total = 2,
                    onCheckedChange = { productDeals = it },
                )
            }
        }
        item {
            SettingsInfoCard(
                title = stringResource(Res.string.notifications_discreet_mode_title),
                description = stringResource(Res.string.notifications_discreet_mode_info),
            )
        }
    }
}
