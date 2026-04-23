package com.group8.comp2300.data.local

import kotlin.test.Test
import kotlin.test.assertEquals

class OnboardingLocalAuthTest {
    @Test
    fun `pending onboarding pin stays local until finish runs`() {
        val events = mutableListOf<String>()
        val pendingPin = "1234"

        assertEquals(emptyList(), events)

        finalizeOnboardingLocalAuth(
            pendingPin = pendingPin,
            markOnboardingCompleted = { events += "markOnboardingCompleted" },
            savePin = { pin -> events += "savePin:$pin" },
            setAppLockEnabled = { enabled -> events += "setAppLockEnabled:$enabled" },
            setBiometricUnlockEnabled = { enabled -> events += "setBiometricUnlockEnabled:$enabled" },
        )

        assertEquals(
            listOf(
                "markOnboardingCompleted",
                "savePin:1234",
                "setAppLockEnabled:true",
                "setBiometricUnlockEnabled:true",
            ),
            events,
        )
    }

    @Test
    fun `finishing onboarding without a pending pin leaves local auth disabled`() {
        val events = mutableListOf<String>()

        finalizeOnboardingLocalAuth(
            pendingPin = null,
            markOnboardingCompleted = { events += "markOnboardingCompleted" },
            savePin = { pin -> events += "savePin:$pin" },
            setAppLockEnabled = { enabled -> events += "setAppLockEnabled:$enabled" },
            setBiometricUnlockEnabled = { enabled -> events += "setBiometricUnlockEnabled:$enabled" },
        )

        assertEquals(listOf("markOnboardingCompleted"), events)
    }
}
