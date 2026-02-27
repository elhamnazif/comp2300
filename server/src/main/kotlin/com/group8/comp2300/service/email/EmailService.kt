package com.group8.comp2300.service.email

import com.resend.Resend
import com.resend.services.emails.model.CreateEmailOptions
import com.resend.services.emails.model.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EmailService(apiKey: String, private val fromEmail: String, private val appBaseUrl: String) {

    private val resend = Resend(apiKey)

    suspend fun sendActivationEmail(toEmail: String, token: String) {
        val activationUrl = "$appBaseUrl/api/auth/activate?token=$token"
        withContext(Dispatchers.IO) {
            val params = CreateEmailOptions.builder()
                .from(fromEmail)
                .to(toEmail)
                .subject("Activate your Vita account")
                .html(
                    """
                    <h2>Welcome to Vita!</h2>
                    <p>Thank you for registering. Please activate your account by clicking the link below:</p>
                    <p><a href="$activationUrl">Activate Account</a></p>
                    <p>If you didn't create this account, you can safely ignore this email.</p>
                    """.trimIndent()
                )
                .addTag(Tag.builder().name("email_type").value("activation").build())
                .build()

            resend.emails().send(params)
        }
    }

    suspend fun sendPasswordResetEmail(toEmail: String, token: String) {
        val resetUrl = "$appBaseUrl/api/auth/reset-password?token=$token"
        withContext(Dispatchers.IO) {
            val params = CreateEmailOptions.builder()
                .from(fromEmail)
                .to(toEmail)
                .subject("Reset your Vita password")
                .html(
                    """
                    <h2>Password Reset</h2>
                    <p>You requested a password reset. Click the link below to set a new password:</p>
                    <p><a href="$resetUrl">Reset Password</a></p>
                    <p>This link expires in 1 hour. If you didn't request this, you can safely ignore this email.</p>
                    """.trimIndent()
                )
                .addTag(Tag.builder().name("email_type").value("password_reset").build())
                .build()

            resend.emails().send(params)
        }
    }
}
