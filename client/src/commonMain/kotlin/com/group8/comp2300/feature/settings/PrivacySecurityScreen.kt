package com.group8.comp2300.feature.settings

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.group8.comp2300.core.security.pin.PinScreen
import com.group8.comp2300.core.ui.settings.*
import com.group8.comp2300.data.local.PrivacySettingsDataSource
import com.group8.comp2300.platform.biometrics.isBiometricAvailable
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.*
import comp2300.i18n.generated.resources.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

private enum class PinFlow { None, SetupNew, VerifyThenClear, VerifyThenSetup }
private enum class PinStep { VerifyOld, SetNew }

@Composable
fun PrivacySecurityScreen(
    onBack: () -> Unit,
    appLockEnabled: Boolean,
    biometricsEnabled: Boolean,
    onVerifyPin: suspend (String) -> Boolean,
    onSavePin: (String) -> Unit,
    onDisableAppLock: () -> Unit,
    onBiometricsEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val privacySettingsDataSource: PrivacySettingsDataSource = koinInject()
    val privacySettings by privacySettingsDataSource.state.collectAsState()
    var dataEncryptionEnabled by remember { mutableStateOf(true) }
    var anonymousReporting by remember { mutableStateOf(false) }
    var shareDataForResearch by remember { mutableStateOf(false) }

    var pinFlow by remember { mutableStateOf(PinFlow.None) }
    var pinStep by remember { mutableStateOf(PinStep.VerifyOld) }
    var pinErrorMessage by remember { mutableStateOf<String?>(null) }
    var pinTogglePending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val biometricsAvailable = isBiometricAvailable()

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
                            onDisableAppLock()
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
            val effectiveAppLockEnabled = appLockEnabled || pinTogglePending
            val total = when {
                effectiveAppLockEnabled && biometricsAvailable -> 4
                effectiveAppLockEnabled -> 3
                else -> 2
            }

            SettingsSection(title = stringResource(Res.string.privacy_security_settings_title)) {
                SettingsToggleRow(
                    icon = Icons.LockW400Outlinedfill1,
                    title = stringResource(Res.string.privacy_security_pin_toggle_title),
                    description = if (effectiveAppLockEnabled) {
                        stringResource(Res.string.privacy_security_pin_toggle_desc_enabled)
                    } else {
                        stringResource(Res.string.privacy_security_pin_toggle_desc_disabled)
                    },
                    checked = effectiveAppLockEnabled,
                    index = 0,
                    total = total,
                    onCheckedChange = { enable ->
                        if (enable && !appLockEnabled) {
                            pinTogglePending = true
                            pinFlow = PinFlow.SetupNew
                        } else if (!enable && appLockEnabled) {
                            pinFlow = PinFlow.VerifyThenClear
                        }
                    },
                )
                if (appLockEnabled) {
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
                if (appLockEnabled && biometricsAvailable) {
                    SettingsToggleRow(
                        icon = Icons.FingerprintW400Outlinedfill1,
                        title = stringResource(Res.string.privacy_security_biometrics_title),
                        description = stringResource(Res.string.privacy_security_biometrics_desc),
                        checked = biometricsEnabled,
                        index = 2,
                        total = total,
                        onCheckedChange = onBiometricsEnabledChange,
                    )
                }
                SettingsToggleRow(
                    icon = Icons.ShieldW400Outlinedfill1,
                    title = stringResource(Res.string.privacy_security_encryption_title),
                    description = stringResource(Res.string.privacy_security_encryption_desc),
                    checked = dataEncryptionEnabled,
                    index = total - 1,
                    total = total,
                    onCheckedChange = { dataEncryptionEnabled = it },
                )
            }
        }
        item {
            SettingsSection(title = stringResource(Res.string.privacy_security_privacy_settings_title)) {
                SettingsToggleRow(
                    icon = Icons.VisibilityOffW400Outlinedfill1,
                    title = stringResource(Res.string.privacy_security_background_blur_title),
                    description = stringResource(Res.string.privacy_security_background_blur_desc),
                    checked = privacySettings.blurAppWhenBackgrounded,
                    index = 0,
                    total = 3,
                    onCheckedChange = privacySettingsDataSource::setBlurAppWhenBackgrounded,
                )
                SettingsToggleRow(
                    icon = Icons.InfoW400Outlinedfill1,
                    title = stringResource(Res.string.privacy_security_anonymous_reporting_title),
                    description = stringResource(Res.string.privacy_security_anonymous_reporting_desc),
                    checked = anonymousReporting,
                    index = 1,
                    total = 3,
                    onCheckedChange = { anonymousReporting = it },
                )
                SettingsToggleRow(
                    icon = Icons.SendW400Outlinedfill1,
                    title = stringResource(Res.string.privacy_security_share_data_title),
                    description = stringResource(Res.string.privacy_security_share_data_desc),
                    checked = shareDataForResearch,
                    index = 2,
                    total = 3,
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
