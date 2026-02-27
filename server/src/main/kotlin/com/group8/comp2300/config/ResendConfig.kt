package com.group8.comp2300.config

import org.slf4j.LoggerFactory

object ResendConfig {
    private val logger = LoggerFactory.getLogger("ResendConfig")

    val apiKey: String = System.getenv("RESEND_API_KEY") ?: ""

    val fromEmail: String = System.getenv("RESEND_FROM_EMAIL") ?: "Vita <noreply@vita.local>"

    val appBaseUrl: String = System.getenv("APP_BASE_URL") ?: "http://localhost:8080"

    val appName: String = System.getenv("APP_NAME") ?: "Vita"

    val isConfigured: Boolean = apiKey.isNotBlank()

    init {
        if (!isConfigured) {
            logger.warn(
                "⚠️ RESEND_API_KEY is not set. Email sending is DISABLED. " +
                    "Users will NOT receive verification or password reset emails. " +
                    "Set RESEND_API_KEY environment variable to enable email service.",
            )
        } else {
            logger.info("✅ Resend email service configured with fromEmail=$fromEmail")
        }
    }
}
