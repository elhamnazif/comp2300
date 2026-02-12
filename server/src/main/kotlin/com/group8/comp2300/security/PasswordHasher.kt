package com.group8.comp2300.security

import at.favre.lib.crypto.bcrypt.BCrypt
import org.slf4j.LoggerFactory

object PasswordHasher {
    private val logger = LoggerFactory.getLogger(PasswordHasher::class.java)
    private const val COST = 12

    fun hash(password: String): String = BCrypt.withDefaults().hashToString(COST, password.toCharArray())

    fun verify(password: String, hash: String): Boolean = try {
        BCrypt.verifyer().verify(password.toCharArray(), hash).verified
    } catch (e: Exception) {
        logger.warn("Password verification failed for hash: ${hash.take(10)}...", e)
        false
    }
}
