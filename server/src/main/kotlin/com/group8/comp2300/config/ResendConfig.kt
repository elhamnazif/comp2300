package com.group8.comp2300.config

object ResendConfig {
    val apiKey: String = System.getenv("RESEND_API_KEY") ?: ""

    val fromEmail: String = System.getenv("RESEND_FROM_EMAIL") ?: "Vita <noreply@vita.local>"

    val appBaseUrl: String = System.getenv("APP_BASE_URL") ?: "http://localhost:8080"
}
