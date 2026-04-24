package com.group8.comp2300.feature.settings

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.group8.comp2300.core.ui.settings.*
import com.group8.comp2300.data.local.NotificationPrivacyMode
import com.group8.comp2300.data.local.NotificationSettingsDataSource
import com.group8.comp2300.data.local.PrivacySettingsDataSource
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.CalendarMonthW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.EditW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.LocalPharmacyW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.NotificationsW400Outlinedfill1
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun NotificationsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val privacySettingsDataSource: PrivacySettingsDataSource = koinInject()
    val notificationSettingsDataSource: NotificationSettingsDataSource = koinInject()
    val privacySettings by privacySettingsDataSource.state.collectAsState()
    val notificationSettings by notificationSettingsDataSource.state.collectAsState()
    var notificationAliasDraft by rememberSaveable(privacySettings.notificationPrivacyMode) {
        mutableStateOf(privacySettings.notificationAlias)
    }
    var isNotificationAliasFocused by remember { mutableStateOf(false) }

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
                    icon = Icons.LocalPharmacyW400Outlinedfill1,
                    title = stringResource(Res.string.notifications_medication_reminders_title),
                    description = stringResource(Res.string.notifications_medication_reminders_desc),
                    checked = notificationSettings.routineRemindersEnabled,
                    index = 0,
                    total = 2,
                    onCheckedChange = notificationSettingsDataSource::setRoutineRemindersEnabled,
                )
                SettingsToggleRow(
                    icon = Icons.CalendarMonthW400Outlinedfill1,
                    title = stringResource(Res.string.notifications_appointment_reminders_title),
                    description = stringResource(Res.string.notifications_appointment_reminders_desc),
                    index = 1,
                    checked = notificationSettings.appointmentRemindersEnabled,
                    total = 2,
                    onCheckedChange = notificationSettingsDataSource::setAppointmentRemindersEnabled,
                )
            }
        }
    }
}
