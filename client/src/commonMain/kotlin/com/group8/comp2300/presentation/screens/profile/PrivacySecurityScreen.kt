package com.group8.comp2300.presentation.screens.profile

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.group8.comp2300.presentation.screens.auth.PinScreen
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.ChevronRightW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.FingerprintW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.InfoW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.LockW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.SendW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.ShieldW400Outlinedfill1
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
    SettingsDetailScaffold(
        title = stringResource(Res.string.privacy_security_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        item {
            val effectivePinEnabled = isPinEnabled || pinTogglePending
            val total = if (isPinEnabled) 4 else 3

            SettingsSection(title = stringResource(Res.string.privacy_security_settings_title)) {
                SettingsToggleRow(
                    icon = Icons.LockW400Outlinedfill1,
                    title = stringResource(Res.string.privacy_security_pin_toggle_title),
                    description = if (effectivePinEnabled) {
                        stringResource(Res.string.privacy_security_pin_toggle_desc_enabled)
                    } else {
                        stringResource(Res.string.privacy_security_pin_toggle_desc_disabled)
                    },
                    checked = effectivePinEnabled,
                    index = 0,
                    total = total,
                    onCheckedChange = { enable ->
                        if (enable && !isPinEnabled) {
                            pinTogglePending = true
                            pinFlow = PinFlow.SetupNew
                        } else if (!enable && isPinEnabled) {
                            pinFlow = PinFlow.VerifyThenClear
                        }
                    },
                )
                if (isPinEnabled) {
                    SettingsNavigationRow(
                        icon = Icons.LockW400Outlinedfill1,
                        title = stringResource(Res.string.privacy_security_change_pin_title),
                        index = 1,
                        total = total,
                        onClick = {
                            pinFlow = PinFlow.VerifyThenSetup
                            pinStep = PinStep.VerifyOld
                            pinErrorMessage = null
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.ChevronRightW400Outlinedfill1,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                            )
                        },
                    )
                }
                SettingsToggleRow(
                    icon = Icons.FingerprintW400Outlinedfill1,
                    title = stringResource(Res.string.privacy_security_biometrics_title),
                    description = stringResource(Res.string.privacy_security_biometrics_desc),
                    checked = biometricsEnabled,
                    index = if (isPinEnabled) 2 else 1,
                    total = total,
                    onCheckedChange = { biometricsEnabled = it },
                )
                SettingsToggleRow(
                    icon = Icons.ShieldW400Outlinedfill1,
                    title = stringResource(Res.string.privacy_security_encryption_title),
                    description = stringResource(Res.string.privacy_security_encryption_desc),
                    checked = dataEncryptionEnabled,
                    index = if (isPinEnabled) 3 else 2,
                    total = total,
                    onCheckedChange = { dataEncryptionEnabled = it },
                )
            }
        }
        item {
            SettingsSection(title = stringResource(Res.string.privacy_security_privacy_settings_title)) {
                SettingsToggleRow(
                    icon = Icons.InfoW400Outlinedfill1,
                    title = stringResource(Res.string.privacy_security_anonymous_reporting_title),
                    description = stringResource(Res.string.privacy_security_anonymous_reporting_desc),
                    checked = anonymousReporting,
                    index = 0,
                    total = 2,
                    onCheckedChange = { anonymousReporting = it },
                )
                SettingsToggleRow(
                    icon = Icons.SendW400Outlinedfill1,
                    title = stringResource(Res.string.privacy_security_share_data_title),
                    description = stringResource(Res.string.privacy_security_share_data_desc),
                    checked = shareDataForResearch,
                    index = 1,
                    total = 2,
                    onCheckedChange = { shareDataForResearch = it },
                )
            }
        }
        item {
            SettingsInfoCard(
                title = stringResource(Res.string.privacy_security_info_title),
                description = stringResource(Res.string.privacy_security_info_body),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}
