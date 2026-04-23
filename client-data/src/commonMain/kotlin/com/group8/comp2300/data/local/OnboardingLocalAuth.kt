package com.group8.comp2300.data.local

fun finalizeOnboardingLocalAuth(
    pendingPin: String?,
    markOnboardingCompleted: () -> Unit,
    savePin: (String) -> Unit,
    setAppLockEnabled: (Boolean) -> Unit,
    setBiometricUnlockEnabled: (Boolean) -> Unit,
) {
    markOnboardingCompleted()
    pendingPin ?: return

    savePin(pendingPin)
    setAppLockEnabled(true)
    setBiometricUnlockEnabled(true)
}
