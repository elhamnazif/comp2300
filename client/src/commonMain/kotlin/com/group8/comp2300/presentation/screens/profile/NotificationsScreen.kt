@file:Suppress("FunctionName")

package com.group8.comp2300.presentation.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.group8.comp2300.presentation.components.AppTopBar
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    var discreetMode by remember { mutableStateOf(true) }
    var appointmentReminders by remember { mutableStateOf(true) }
    var testResults by remember { mutableStateOf(true) }
    var testReminders by remember { mutableStateOf(true) }
    var educationContent by remember { mutableStateOf(false) }
    var productDeals by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = { Text(stringResource(Res.string.notifications_title)) },
                onBackClick = onBack,
                backContentDescription = stringResource(Res.string.auth_back_desc)
            )
        }
    ) { paddingValues ->
        Column(
            modifier =
                Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            Text(
                stringResource(Res.string.notifications_general_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            SettingToggleItem(
                title = stringResource(Res.string.notifications_discreet_mode_title),
                description = stringResource(Res.string.notifications_discreet_mode_desc),
                checked = discreetMode,
                onCheckedChange = { discreetMode = it }
            )

            Spacer(Modifier.height(24.dp))

            Text(
                stringResource(Res.string.notifications_health_reminders_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            SettingToggleItem(
                title = stringResource(Res.string.notifications_appointment_reminders_title),
                description = stringResource(Res.string.notifications_appointment_reminders_desc),
                checked = appointmentReminders,
                onCheckedChange = { appointmentReminders = it }
            )

            Spacer(Modifier.height(8.dp))

            SettingToggleItem(
                title = stringResource(Res.string.notifications_test_results_title),
                description = stringResource(Res.string.notifications_test_results_desc),
                checked = testResults,
                onCheckedChange = { testResults = it }
            )

            Spacer(Modifier.height(8.dp))

            SettingToggleItem(
                title = stringResource(Res.string.notifications_testing_reminders_title),
                description = stringResource(Res.string.notifications_testing_reminders_desc),
                checked = testReminders,
                onCheckedChange = { testReminders = it }
            )

            Spacer(Modifier.height(24.dp))

            Text(
                stringResource(Res.string.notifications_content_updates_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            SettingToggleItem(
                title = stringResource(Res.string.notifications_educational_content_title),
                description = stringResource(Res.string.notifications_educational_content_desc),
                checked = educationContent,
                onCheckedChange = { educationContent = it }
            )

            Spacer(Modifier.height(8.dp))

            SettingToggleItem(
                title = stringResource(Res.string.notifications_product_deals_title),
                description = stringResource(Res.string.notifications_product_deals_desc),
                checked = productDeals,
                onCheckedChange = { productDeals = it }
            )

            Spacer(Modifier.height(24.dp))

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(Res.string.notifications_discreet_mode_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(Res.string.notifications_discreet_mode_info),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingToggleItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(Modifier.width(16.dp))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
