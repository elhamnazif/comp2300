package com.group8.comp2300.feature.settings

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.group8.comp2300.core.ui.settings.*
import com.group8.comp2300.data.local.NotificationPrivacyMode
import com.group8.comp2300.data.local.PrivacySettingsDataSource
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.*
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun NotificationsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val privacySettingsDataSource: PrivacySettingsDataSource = koinInject()
    val privacySettings by privacySettingsDataSource.state.collectAsState()
    var notificationAliasDraft by rememberSaveable(privacySettings.notificationPrivacyMode) {
        mutableStateOf(privacySettings.notificationAlias)
    }
    var isNotificationAliasFocused by remember { mutableStateOf(false) }
    var appointmentReminders by remember { mutableStateOf(true) }
    var testResults by remember { mutableStateOf(true) }
    var testReminders by remember { mutableStateOf(true) }
    var educationContent by remember { mutableStateOf(false) }
    var productDeals by remember { mutableStateOf(false) }

    val commitNotificationAlias = {
        if (notificationAliasDraft != privacySettings.notificationAlias) {
            privacySettingsDataSource.setNotificationAlias(notificationAliasDraft)
        }
    }
    val currentNotificationPrivacyMode by rememberUpdatedState(privacySettings.notificationPrivacyMode)
    val currentNotificationAliasDraft by rememberUpdatedState(notificationAliasDraft)
    val currentNotificationAlias by rememberUpdatedState(privacySettings.notificationAlias)

    LaunchedEffect(privacySettings.notificationAlias, isNotificationAliasFocused) {
        if (!isNotificationAliasFocused && notificationAliasDraft != privacySettings.notificationAlias) {
            notificationAliasDraft = privacySettings.notificationAlias
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (
                currentNotificationPrivacyMode == NotificationPrivacyMode.ALIAS_BASED &&
                currentNotificationAliasDraft != currentNotificationAlias
            ) {
                privacySettingsDataSource.setNotificationAlias(currentNotificationAliasDraft)
            }
        }
    }

    SettingsDetailScaffold(
        title = stringResource(Res.string.notifications_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        item {
            SettingsSection(title = stringResource(Res.string.notifications_general_title)) {
                SettingsChoiceRow(
                    icon = Icons.NotificationsW400Outlinedfill1,
                    title = stringResource(Res.string.notifications_privacy_title),
                    description = stringResource(Res.string.notifications_privacy_desc),
                    options = listOf(
                        SettingsChoiceOption(
                            key = NotificationPrivacyMode.NEUTRAL.name,
                            label = stringResource(Res.string.notifications_privacy_neutral_label),
                        ),
                        SettingsChoiceOption(
                            key = NotificationPrivacyMode.ALIAS_BASED.name,
                            label = stringResource(Res.string.notifications_privacy_alias_label),
                        ),
                    ),
                    selectedKey = privacySettings.notificationPrivacyMode.name,
                    index = 0,
                    total = if (privacySettings.notificationPrivacyMode ==
                        NotificationPrivacyMode.ALIAS_BASED
                    ) {
                        2
                    } else {
                        1
                    },
                    onOptionSelected = { selectedMode ->
                        privacySettingsDataSource.setNotificationPrivacyMode(
                            NotificationPrivacyMode.entries.first { it.name == selectedMode },
                        )
                    },
                )
                if (privacySettings.notificationPrivacyMode == NotificationPrivacyMode.ALIAS_BASED) {
                    SettingsTextFieldRow(
                        icon = Icons.EditW400Outlinedfill1,
                        title = stringResource(Res.string.notifications_alias_title),
                        description = stringResource(Res.string.notifications_alias_desc),
                        value = notificationAliasDraft,
                        placeholder = stringResource(Res.string.notifications_alias_placeholder),
                        index = 1,
                        total = 2,
                        onValueChange = { notificationAliasDraft = it },
                        onFocusChanged = { isFocused ->
                            if (isNotificationAliasFocused && !isFocused) {
                                commitNotificationAlias()
                            }
                            isNotificationAliasFocused = isFocused
                        },
                        onValueCommit = commitNotificationAlias,
                    )
                }
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
                title = stringResource(Res.string.notifications_privacy_title),
                description = stringResource(Res.string.notifications_privacy_info),
            )
        }
    }
}
