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
import com.group8.comp2300.presentation.screens.auth.PinScreen
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.ChevronRightW400Outlinedfill1
import comp2300.i18n.generated.resources.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private enum class PinFlow { None, SetupNew, VerifyThenClear, VerifyThenSetup }
private enum class PinStep { VerifyOld, SetNew }

@Composable
fun PrivacySecurityScreen(
    onBack: () -> Unit,
    isPinEnabled: Boolean,
    onVerifyPin: suspend (String) -> Boolean,
    onSavePin: (String) -> Unit,
    onClearPin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var biometricsEnabled by remember { mutableStateOf(true) }
    var dataEncryptionEnabled by remember { mutableStateOf(true) }
    var anonymousReporting by remember { mutableStateOf(false) }
    var shareDataForResearch by remember { mutableStateOf(false) }

    var pinFlow by remember { mutableStateOf(PinFlow.None) }
    var pinStep by remember { mutableStateOf(PinStep.VerifyOld) }
    var pinErrorMessage by remember { mutableStateOf<String?>(null) }
    var pinTogglePending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val incorrectPinText = stringResource(Res.string.privacy_security_incorrect_pin)

    // --- PIN sub-flows ---
    when (pinFlow) {
        PinFlow.SetupNew -> {
            PinScreen(
                onComplete = { pin ->
                    onSavePin(pin)
                    pinTogglePending = false
                    pinFlow = PinFlow.None
                },
                isSetup = true,
                onDismiss = {
                    pinTogglePending = false
                    pinFlow = PinFlow.None
                },
            )
            return
        }

        PinFlow.VerifyThenClear -> {
            PinScreen(
                onComplete = { pin ->
                    scope.launch {
                        if (onVerifyPin(pin)) {
                            onClearPin()
                            pinFlow = PinFlow.None
                        } else {
                            pinErrorMessage = incorrectPinText
                        }
                    }
                },
                isSetup = false,
                title = stringResource(Res.string.privacy_security_verify_disable_title),
                description = stringResource(Res.string.privacy_security_verify_disable_desc),
                errorMessage = pinErrorMessage,
                onErrorMessageCleared = { pinErrorMessage = null },
                onDismiss = { pinFlow = PinFlow.None },
            )
            return
        }

        PinFlow.VerifyThenSetup -> {
            when (pinStep) {
                PinStep.VerifyOld -> {
                    PinScreen(
                        onComplete = { pin ->
                            scope.launch {
                                if (onVerifyPin(pin)) {
                                    pinStep = PinStep.SetNew
                                    pinErrorMessage = null
                                } else {
                                    pinErrorMessage = incorrectPinText
                                }
                            }
                        },
                        isSetup = false,
                        title = stringResource(Res.string.privacy_security_verify_pin_title),
                        description = stringResource(Res.string.privacy_security_verify_pin_desc),
                        errorMessage = pinErrorMessage,
                        onErrorMessageCleared = { pinErrorMessage = null },
                        onDismiss = { pinFlow = PinFlow.None },
                    )
                    return
                }

                PinStep.SetNew -> {
                    PinScreen(
                        onComplete = { pin ->
                            onSavePin(pin)
                            pinFlow = PinFlow.None
                        },
                        isSetup = true,
                        onDismiss = {
                            pinStep = PinStep.VerifyOld
                        },
                    )
                    return
                }
            }
        }

        PinFlow.None -> { /* fall through to normal settings */ }
    }

    // --- Normal settings view ---
    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = { Text(stringResource(Res.string.privacy_security_title)) },
                onBackClick = onBack,
                backContentDescription = stringResource(Res.string.auth_back_desc),
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
            Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            Text(
                stringResource(Res.string.privacy_security_settings_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            val effectivePinEnabled = isPinEnabled || pinTogglePending
            SettingToggleItem(
                title = stringResource(Res.string.privacy_security_pin_toggle_title),
                description = if (effectivePinEnabled) {
                    stringResource(Res.string.privacy_security_pin_toggle_desc_enabled)
                } else {
                    stringResource(Res.string.privacy_security_pin_toggle_desc_disabled)
                },
                checked = effectivePinEnabled,
                onCheckedChange = { enable ->
                    if (enable && !isPinEnabled) {
                        pinTogglePending = true
                        pinFlow = PinFlow.SetupNew
                    } else if (!enable && isPinEnabled) {
                        pinFlow = PinFlow.VerifyThenClear
                    }
                },
            )

            Spacer(Modifier.height(8.dp))

            if (isPinEnabled) {
                Card(
                    onClick = {
                        pinFlow = PinFlow.VerifyThenSetup
                        pinStep = PinStep.VerifyOld
                        pinErrorMessage = null
                    },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(Res.string.privacy_security_change_pin_title),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Icon(
                            Icons.ChevronRightW400Outlinedfill1,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
            }

            SettingToggleItem(
                title = stringResource(Res.string.privacy_security_biometrics_title),
                description = stringResource(Res.string.privacy_security_biometrics_desc),
                checked = biometricsEnabled,
                onCheckedChange = { biometricsEnabled = it },
            )

            Spacer(Modifier.height(8.dp))

            SettingToggleItem(
                title = stringResource(Res.string.privacy_security_encryption_title),
                description = stringResource(Res.string.privacy_security_encryption_desc),
                checked = dataEncryptionEnabled,
                onCheckedChange = { dataEncryptionEnabled = it },
            )

            Spacer(Modifier.height(24.dp))

            Text(
                stringResource(Res.string.privacy_security_privacy_settings_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            SettingToggleItem(
                title = stringResource(Res.string.privacy_security_anonymous_reporting_title),
                description = stringResource(Res.string.privacy_security_anonymous_reporting_desc),
                checked = anonymousReporting,
                onCheckedChange = { anonymousReporting = it },
            )

            Spacer(Modifier.height(8.dp))

            SettingToggleItem(
                title = stringResource(Res.string.privacy_security_share_data_title),
                description = stringResource(Res.string.privacy_security_share_data_desc),
                checked = shareDataForResearch,
                onCheckedChange = { shareDataForResearch = it },
            )

            Spacer(Modifier.height(24.dp))

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(Res.string.privacy_security_info_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(Res.string.privacy_security_info_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
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
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
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
            Spacer(Modifier.width(16.dp))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
