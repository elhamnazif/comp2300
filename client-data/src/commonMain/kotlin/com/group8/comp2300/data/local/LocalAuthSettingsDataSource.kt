package com.group8.comp2300.data.local

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class LocalAuthSettings(
    val appLockEnabled: Boolean = false,
    val biometricUnlockEnabled: Boolean = false,
    val onboardingCompleted: Boolean = false,
)

class LocalAuthSettingsDataSource(
    private val settings: Settings,
    pinDataSource: PinDataSource,
) {
    val state: StateFlow<LocalAuthSettings>
        field: MutableStateFlow<LocalAuthSettings> = MutableStateFlow(loadSettings(pinDataSource))

    fun setAppLockEnabled(enabled: Boolean) {
        settings.putBoolean(KeyAppLockEnabled, enabled)
        state.value = state.value.copy(appLockEnabled = enabled)
    }

    fun setBiometricUnlockEnabled(enabled: Boolean) {
        settings.putBoolean(KeyBiometricUnlockEnabled, enabled)
        state.value = state.value.copy(biometricUnlockEnabled = enabled)
    }

    fun markOnboardingCompleted() {
        settings.putBoolean(KeyOnboardingCompleted, true)
        state.value = state.value.copy(onboardingCompleted = true)
    }

    private fun loadSettings(pinDataSource: PinDataSource): LocalAuthSettings {
        val hasLegacyPin = pinDataSource.pinSet.value
        return LocalAuthSettings(
            appLockEnabled = settings.getBooleanOrNull(KeyAppLockEnabled)
                ?: hasLegacyPin.also { settings.putBoolean(KeyAppLockEnabled, it) },
            biometricUnlockEnabled = settings.getBooleanOrNull(KeyBiometricUnlockEnabled)
                ?: hasLegacyPin.also { settings.putBoolean(KeyBiometricUnlockEnabled, it) },
            onboardingCompleted = settings.getBooleanOrNull(KeyOnboardingCompleted)
                ?: hasLegacyPin.also { settings.putBoolean(KeyOnboardingCompleted, it) },
        )
    }

    private companion object {
        const val KeyAppLockEnabled = "auth.app_lock_enabled"
        const val KeyBiometricUnlockEnabled = "auth.biometric_unlock_enabled"
        const val KeyOnboardingCompleted = "auth.onboarding_completed"
    }
}
