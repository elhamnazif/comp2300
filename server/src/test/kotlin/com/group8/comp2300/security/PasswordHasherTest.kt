package com.group8.comp2300.security

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PasswordHasherTest {

    @Test
    fun `hash produces valid bcrypt string`() {
        val password = "Password123"
        val hash = PasswordHasher.hash(password)

        assertTrue(hash.startsWith("\$2"))
        assertTrue(hash.length >= 60)
    }

    @Test
    fun `verify returns true for correct password`() {
        val password = "Password123"
        val hash = PasswordHasher.hash(password)

        assertTrue(PasswordHasher.verify(password, hash))
    }

    @Test
    fun `verify returns false for wrong password`() {
        val password = "Password123"
        val hash = PasswordHasher.hash(password)

        assertFalse(PasswordHasher.verify("WrongPassword", hash))
    }

    @Test
    fun `verify handles malformed hash gracefully`() {
        val password = "Password123"
        val malformedHash = "not-a-valid-hash"

        assertFalse(PasswordHasher.verify(password, malformedHash))
    }

    @Test
    fun `different passwords produce different hashes`() {
        val password1 = "Password123"
        val password2 = "DifferentPassword"

        val hash1 = PasswordHasher.hash(password1)
        val hash2 = PasswordHasher.hash(password2)

        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `same password produces different hashes due to salt`() {
        val password = "Password123"

        val hash1 = PasswordHasher.hash(password)
        val hash2 = PasswordHasher.hash(password)

        // Different hashes due to bcrypt salt
        assertNotEquals(hash1, hash2)
        // But both verify correctly
        assertTrue(PasswordHasher.verify(password, hash1))
        assertTrue(PasswordHasher.verify(password, hash2))
    }
}
