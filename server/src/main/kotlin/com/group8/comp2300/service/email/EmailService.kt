package com.group8.comp2300.service.email

import com.resend.Resend
import com.resend.services.emails.model.CreateEmailOptions
import com.resend.services.emails.model.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * Result of an email send operation.
 */
sealed class EmailResult {
    /**
     * Email was sent successfully.
     * @param messageId The message ID returned by the email service, if available.
     */
    data class Success(val messageId: String?) : EmailResult()

    /**
     * Email failed to send.
     * @param error The exception that caused the failure.
     */
    data class Failure(val error: Throwable) : EmailResult()
}

class EmailService(apiKey: String, private val fromEmail: String, private val appName: String) {

    private val resend = Resend(apiKey)

    private val activationTemplate: String by lazy { loadTemplate("activation-email.html") }
    private val passwordResetTemplate: String by lazy { loadTemplate("password-reset-email.html") }

    /**
     * Sends an activation/verification email to the user.
     *
     * @param toEmail The recipient's email address.
     * @param token The verification token/code to include in the email.
     * @return EmailResult indicating success or failure.
     */
    suspend fun sendActivationEmail(toEmail: String, token: String): EmailResult = try {
        withContext(Dispatchers.IO) {
            val htmlContent = renderTemplate(activationTemplate, mapOf("token" to token))

            val params = CreateEmailOptions.builder()
                .from(fromEmail)
                .to(toEmail)
                .subject("Verify your email address")
                .html(htmlContent)
                .addTag(Tag.builder().name("email_type").value("activation").build())
                .build()

            val response = resend.emails().send(params)
            EmailResult.Success(response.id)
        }
    } catch (e: Exception) {
        EmailResult.Failure(e)
    }

    /**
     * Sends a password reset email to the user.
     *
     * @param toEmail The recipient's email address.
     * @param token The reset token/code to include in the email.
     * @return EmailResult indicating success or failure.
     */
    suspend fun sendPasswordResetEmail(toEmail: String, token: String): EmailResult = try {
        withContext(Dispatchers.IO) {
            val htmlContent = renderTemplate(passwordResetTemplate, mapOf("token" to token))

            val params = CreateEmailOptions.builder()
                .from(fromEmail)
                .to(toEmail)
                .subject("Reset your password")
                .html(htmlContent)
                .addTag(Tag.builder().name("email_type").value("password_reset").build())
                .build()

            val response = resend.emails().send(params)
            EmailResult.Success(response.id)
        }
    } catch (e: Exception) {
        EmailResult.Failure(e)
    }

    /**
     * Loads an email template from the resources/email directory.
     *
     * @param templateName The name of the template file.
     * @return The template content as a string.
     * @throws IllegalStateException if the template cannot be found.
     */
    private fun loadTemplate(templateName: String): String {
        val resourcePath = "email/$templateName"
        val inputStream: InputStream? = javaClass.classLoader.getResourceAsStream(resourcePath)

        return inputStream?.bufferedReader().use { it?.readText() }
            ?: throw IllegalStateException("Email template not found: $resourcePath")
    }

    /**
     * Renders a template by replacing placeholders with actual values.
     *
     * @param template The template content with {{placeholder}} syntax.
     * @param variables A map of placeholder names to their replacement values.
     * @return The rendered template with all placeholders replaced.
     */
    private fun renderTemplate(template: String, variables: Map<String, String>): String {
        var result = template

        // Add app_name to all templates
        val allVariables = variables + ("app_name" to appName)

        // Replace all {{placeholder}} patterns
        for ((key, value) in allVariables) {
            result = result.replace("{{{$key}}}", value)
            result = result.replace("{{$key}}", value)
        }

        return result
    }
}
