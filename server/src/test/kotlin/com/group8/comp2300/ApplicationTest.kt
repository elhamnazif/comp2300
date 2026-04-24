package com.group8.comp2300

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.io.path.createTempFile
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    @Test
    fun testRoot() = testApplication {
        val previousDbPath = System.getProperty("DB_PATH")
        val testDbPath = createTempFile("comp2300-server-test", ".db").toFile()
        System.setProperty("DB_PATH", "jdbc:sqlite:${testDbPath.absolutePath}")

        try {
            application {
                module()
            }
            val response = client.get("/")
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("Ktor: ready", response.bodyAsText())
        } finally {
            if (previousDbPath == null) {
                System.clearProperty("DB_PATH")
            } else {
                System.setProperty("DB_PATH", previousDbPath)
            }
            testDbPath.delete()
        }
    }
}
