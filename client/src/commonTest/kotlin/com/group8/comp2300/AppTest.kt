package com.group8.comp2300

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppTest {

    @Test
    fun testScreenProperties() {
        val home = com.group8.comp2300.model.Screen.Home
        assertEquals(home.label, "Home")
        assertTrue(true)

        val detail = com.group8.comp2300.model.Screen.DoctorDetail("123")
        assertEquals(detail.doctorId, "123")
    }
}
