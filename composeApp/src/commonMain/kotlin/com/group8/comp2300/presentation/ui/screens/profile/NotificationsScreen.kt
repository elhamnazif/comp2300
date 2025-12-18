package com.group8.comp2300.presentation.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    var discreetMode by remember { mutableStateOf(true) }
    var appointmentReminders by remember { mutableStateOf(true) }
    var testResults by remember { mutableStateOf(true) }
    var testReminders by remember { mutableStateOf(true) }
    var educationContent by remember { mutableStateOf(false) }
    var productDeals by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
        ) {
            Text(
                "General",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            SettingToggleItem(
                title = "Discreet Mode",
                description = "Hide sensitive information in notifications",
                checked = discreetMode,
                onCheckedChange = { discreetMode = it }
            )

            Spacer(Modifier.height(24.dp))

            Text(
                "Health Reminders",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            SettingToggleItem(
                title = "Appointment Reminders",
                description = "Get notified 24 hours before scheduled appointments",
                checked = appointmentReminders,
                onCheckedChange = { appointmentReminders = it }
            )

            Spacer(Modifier.height(8.dp))

            SettingToggleItem(
                title = "Test Results",
                description = "Get notified when lab results are available",
                checked = testResults,
                onCheckedChange = { testResults = it }
            )

            Spacer(Modifier.height(8.dp))

            SettingToggleItem(
                title = "Regular Testing Reminders",
                description = "Remind me to schedule screenings every 3 months",
                checked = testReminders,
                onCheckedChange = { testReminders = it }
            )

            Spacer(Modifier.height(24.dp))

            Text(
                "Content & Updates",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            SettingToggleItem(
                title = "Educational Content",
                description = "Weekly tips and articles about sexual health",
                checked = educationContent,
                onCheckedChange = { educationContent = it }
            )

            Spacer(Modifier.height(8.dp))

            SettingToggleItem(
                title = "Product Deals",
                description = "Special offers on protection and testing kits",
                checked = productDeals,
                onCheckedChange = { productDeals = it }
            )

            Spacer(Modifier.height(24.dp))

            Card(
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Discreet Mode",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "When enabled, notifications will not show specific details about appointments or test results. You'll receive a generic reminder to check the app.",
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
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
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
