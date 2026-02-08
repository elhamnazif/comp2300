@file:Suppress("FunctionName")

package com.group8.comp2300.presentation.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.symbols.icons.materialsymbols.Icons
import com.app.symbols.icons.materialsymbols.icons.*
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySecurityScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    var biometricsEnabled by remember { mutableStateOf(true) }
    var dataEncryptionEnabled by remember { mutableStateOf(true) }
    var anonymousReporting by remember { mutableStateOf(false) }
    var shareDataForResearch by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.privacy_security_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.ArrowBackW400Outlinedfill1, stringResource(Res.string.auth_back_desc))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier =
                Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            Text(
                stringResource(Res.string.privacy_security_settings_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            SettingToggleItem(
                title = stringResource(Res.string.privacy_security_biometrics_title),
                description = stringResource(Res.string.privacy_security_biometrics_desc),
                checked = biometricsEnabled,
                onCheckedChange = { biometricsEnabled = it }
            )

            Spacer(Modifier.height(8.dp))

            SettingToggleItem(
                title = stringResource(Res.string.privacy_security_encryption_title),
                description = stringResource(Res.string.privacy_security_encryption_desc),
                checked = dataEncryptionEnabled,
                onCheckedChange = { dataEncryptionEnabled = it }
            )

            Spacer(Modifier.height(24.dp))

            Text(
                stringResource(Res.string.privacy_security_privacy_settings_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            SettingToggleItem(
                title = stringResource(Res.string.privacy_security_anonymous_reporting_title),
                description = stringResource(Res.string.privacy_security_anonymous_reporting_desc),
                checked = anonymousReporting,
                onCheckedChange = { anonymousReporting = it }
            )

            Spacer(Modifier.height(8.dp))

            SettingToggleItem(
                title = stringResource(Res.string.privacy_security_share_data_title),
                description = stringResource(Res.string.privacy_security_share_data_desc),
                checked = shareDataForResearch,
                onCheckedChange = { shareDataForResearch = it }
            )

            Spacer(Modifier.height(24.dp))

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(Res.string.privacy_security_info_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(Res.string.privacy_security_info_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
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
