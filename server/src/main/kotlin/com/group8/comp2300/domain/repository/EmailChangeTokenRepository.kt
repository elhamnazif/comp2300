package com.group8.comp2300.domain.repository

data class EmailChangeToken(val userId: String, val newEmail: String)

interface EmailChangeTokenRepository {
    fun insert(tokenHash: String, userId: String, newEmail: String)
    fun findValid(tokenHash: String): EmailChangeToken?
    fun markUsed(tokenHash: String)
    fun deleteExpired(cutoffMillis: Long)
    fun deleteByUserId(userId: String)
}
