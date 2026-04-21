package com.group8.comp2300.app.navigation

import androidx.lifecycle.SavedStateHandle
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class RealNavigatorTest {

    @Test
    fun `tab start destinations resolve to MainShell`() {
        val navigator = RealNavigator(SavedStateHandle())

        navigator.clearAndGoTo(Screen.Home)

        assertEquals(listOf(Screen.MainShell), navigator.backStack)
        assertEquals(listOf(Screen.Home), navigator.mainShellBackStack)
        assertEquals(Screen.Home, navigator.currentTab)
    }

    @Test
    fun `navigating to a tab updates shell state instead of pushing a root destination`() {
        val navigator = RealNavigator(SavedStateHandle())
        navigator.clearAndGoTo(Screen.Home)

        navigator.navigate(Screen.Booking)

        assertEquals(listOf(Screen.MainShell), navigator.backStack)
        assertEquals(listOf(Screen.Home, Screen.Booking), navigator.mainShellBackStack)
        assertEquals(Screen.Booking, navigator.currentTab)
    }

    @Test
    fun `overlay destinations push above MainShell`() {
        val navigator = RealNavigator(SavedStateHandle())
        navigator.clearAndGoTo(Screen.Booking)

        navigator.navigate(Screen.ClinicDetail("clinic-1"))

        assertEquals(listOf(Screen.MainShell, Screen.ClinicDetail("clinic-1")), navigator.backStack)
        assertEquals(Screen.Booking, navigator.currentTab)
    }

    @Test
    fun `shell destinations stay in the shell stack`() {
        val navigator = RealNavigator(SavedStateHandle())
        navigator.clearAndGoTo(Screen.Booking)

        navigator.navigateWithinShell(Screen.ClinicDetail("clinic-1"))

        assertEquals(listOf(Screen.MainShell), navigator.backStack)
        assertEquals(listOf(Screen.Booking, Screen.ClinicDetail("clinic-1")), navigator.mainShellBackStack)
        assertEquals(Screen.Booking, navigator.currentTab)
    }

    @Test
    fun `auth success to a tab restores MainShell and selects the target tab`() {
        val navigator = RealNavigator(SavedStateHandle())
        navigator.clearAndGoTo(Screen.Home)

        navigator.requireAuth(Screen.Profile)
        navigator.onAuthSuccess()

        assertEquals(listOf(Screen.MainShell), navigator.backStack)
        assertEquals(listOf(Screen.Profile), navigator.mainShellBackStack)
        assertEquals(Screen.Profile, navigator.currentTab)
    }

    @Test
    fun `restored pre shell tab stacks normalize into MainShell`() {
        val navigator =
            RealNavigator(
                SavedStateHandle(
                    mapOf(
                        "nav_stack" to listOf(
                            Json.encodeToString<Screen>(Screen.Booking),
                            Json.encodeToString<Screen>(Screen.ClinicDetail("clinic-1")),
                        ),
                    ),
                ),
            )

        assertEquals(listOf(Screen.MainShell, Screen.ClinicDetail("clinic-1")), navigator.backStack)
        assertEquals(listOf(Screen.Booking), navigator.mainShellBackStack)
        assertEquals(Screen.Booking, navigator.currentTab)
    }

    @Test
    fun `restored pre shell stacks keep the last visible tab inside MainShell`() {
        val navigator =
            RealNavigator(
                SavedStateHandle(
                    mapOf(
                        "nav_stack" to listOf(
                            Json.encodeToString<Screen>(Screen.Home),
                            Json.encodeToString<Screen>(Screen.Booking),
                            Json.encodeToString<Screen>(Screen.ClinicDetail("clinic-1")),
                        ),
                    ),
                ),
            )

        assertEquals(listOf(Screen.MainShell, Screen.ClinicDetail("clinic-1")), navigator.backStack)
        assertEquals(listOf(Screen.Booking), navigator.mainShellBackStack)
        assertEquals(Screen.Booking, navigator.currentTab)
    }
}
