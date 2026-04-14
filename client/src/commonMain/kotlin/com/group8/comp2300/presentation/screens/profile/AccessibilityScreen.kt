package com.group8.comp2300.presentation.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.group8.comp2300.data.local.AccessibilitySettingsDataSource
import com.group8.comp2300.presentation.components.AppTopBar
import comp2300.i18n.generated.resources.Res
import comp2300.i18n.generated.resources.accessibility_grayscale_desc
import comp2300.i18n.generated.resources.accessibility_grayscale_info
import comp2300.i18n.generated.resources.accessibility_grayscale_title
import comp2300.i18n.generated.resources.accessibility_supporting_text
import comp2300.i18n.generated.resources.accessibility_title
import comp2300.i18n.generated.resources.auth_back_desc
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun AccessibilityScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val accessibilitySettingsDataSource: AccessibilitySettingsDataSource = koinInject()
    val accessibilitySettings by accessibilitySettingsDataSource.state.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = { Text(stringResource(Res.string.accessibility_title)) },
                onBackClick = onBack,
                backContentDescription = stringResource(Res.string.auth_back_desc),
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(Res.string.accessibility_supporting_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            AccessibilityToggleItem(
                title = stringResource(Res.string.accessibility_grayscale_title),
                description = stringResource(Res.string.accessibility_grayscale_desc),
                checked = accessibilitySettings.grayscaleEnabled,
                onCheckedChange = accessibilitySettingsDataSource::setGrayscaleEnabled,
            )

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(Res.string.accessibility_grayscale_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(Res.string.accessibility_grayscale_info),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun AccessibilityToggleItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier.fillMaxWidth(),
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
