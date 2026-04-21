package com.group8.comp2300.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.group8.comp2300.data.database.AppDatabase
import com.russhwolf.settings.Settings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalAuthSettingsDataSourceTest {
    @Test
    fun defaultsToDisabledLockAndIncompleteOnboardingWithoutLegacyPin() {
        val settings = newSettings()
        val dataSource = LocalAuthSettingsDataSource(settings, newPinDataSource())

        assertFalse(dataSource.state.value.appLockEnabled)
        assertFalse(dataSource.state.value.biometricUnlockEnabled)
        assertFalse(dataSource.state.value.onboardingCompleted)
    }

    @Test
    fun migratesLegacyPinStateIntoLocalAuthSettings() {
        val settings = newSettings()
        val pinDataSource = newPinDataSource().apply { savePin("1234") }

        val dataSource = LocalAuthSettingsDataSource(settings, pinDataSource)

        assertTrue(dataSource.state.value.appLockEnabled)
        assertTrue(dataSource.state.value.biometricUnlockEnabled)
        assertTrue(dataSource.state.value.onboardingCompleted)
    }

    @Test
    fun persistsChangesAcrossDatasourceRecreation() {
        val settings = newSettings()
        val pinDataSource = newPinDataSource()
        val dataSource = LocalAuthSettingsDataSource(settings, pinDataSource)

        dataSource.markOnboardingCompleted()
        dataSource.setAppLockEnabled(true)
        dataSource.setBiometricUnlockEnabled(false)

        val restored = LocalAuthSettingsDataSource(settings, pinDataSource).state.value
        assertTrue(restored.onboardingCompleted)
        assertTrue(restored.appLockEnabled)
        assertFalse(restored.biometricUnlockEnabled)
    }

    private fun newPinDataSource(): PinDataSource {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        return PinDataSource(AppDatabase(driver))
    }

    private fun newSettings(): Settings = Settings().also {
        it.remove("auth.app_lock_enabled")
        it.remove("auth.biometric_unlock_enabled")
        it.remove("auth.onboarding_completed")
    }
}
