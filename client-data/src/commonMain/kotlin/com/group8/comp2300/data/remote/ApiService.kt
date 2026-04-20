package com.group8.comp2300.data.remote

import com.group8.comp2300.data.remote.dto.ArticleDetailDto
import com.group8.comp2300.data.remote.dto.ArticleSummaryDto
import com.group8.comp2300.data.remote.dto.AuthResponse
import com.group8.comp2300.data.remote.dto.CategoryDto
import com.group8.comp2300.data.remote.dto.CompleteProfileRequest
import com.group8.comp2300.data.remote.dto.EarnedBadgeDto
import com.group8.comp2300.data.remote.dto.ForgotPasswordRequest
import com.group8.comp2300.data.remote.dto.LoginRequest
import com.group8.comp2300.data.remote.dto.MessageResponse
import com.group8.comp2300.data.remote.dto.PreregisterRequest
import com.group8.comp2300.data.remote.dto.PreregisterResponse
import com.group8.comp2300.data.remote.dto.ProductDto
import com.group8.comp2300.data.remote.dto.QuizDto
import com.group8.comp2300.data.remote.dto.QuizSubmissionRequestDto
import com.group8.comp2300.data.remote.dto.QuizSubmissionResultDto
import com.group8.comp2300.data.remote.dto.RefreshTokenRequest
import com.group8.comp2300.data.remote.dto.ResendVerificationRequest
import com.group8.comp2300.data.remote.dto.ResetPasswordRequest
import com.group8.comp2300.data.remote.dto.TokenResponse
import com.group8.comp2300.data.remote.dto.UserQuizStatsDto
import com.group8.comp2300.domain.model.medical.Appointment
import com.group8.comp2300.domain.model.medical.AppointmentSlot
import com.group8.comp2300.domain.model.medical.Clinic
import com.group8.comp2300.domain.model.medical.ClinicBookingRequest
import com.group8.comp2300.domain.model.medical.MedicalRecordCategory
import com.group8.comp2300.domain.model.medical.MedicalRecordResponse
import com.group8.comp2300.domain.model.medical.Medication
import com.group8.comp2300.domain.model.medical.MedicationCreateRequest
import com.group8.comp2300.domain.model.medical.MedicationLog
import com.group8.comp2300.domain.model.medical.MedicationLogRequest
import com.group8.comp2300.domain.model.medical.Mood
import com.group8.comp2300.domain.model.medical.MoodEntryRequest
import com.group8.comp2300.domain.model.medical.Routine
import com.group8.comp2300.domain.model.medical.RoutineCreateRequest
import com.group8.comp2300.domain.model.medical.RoutineDayAgenda
import com.group8.comp2300.domain.model.medical.RoutineOccurrenceOverride
import com.group8.comp2300.domain.model.medical.RoutineOccurrenceOverrideRequest
import com.group8.comp2300.domain.model.user.User
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.delete
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.serialization.JsonConvertException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

interface ApiService {
    suspend fun getHealth(): Map<String, String>

    suspend fun getProducts(): List<ProductDto>

    suspend fun getProduct(id: String): ProductDto

    suspend fun login(request: LoginRequest): AuthResponse

    suspend fun refreshToken(request: RefreshTokenRequest): TokenResponse

    suspend fun logout()

    suspend fun getProfile(): User

    suspend fun activateAccount(token: String): AuthResponse

    suspend fun forgotPassword(email: String): MessageResponse

    suspend fun resetPassword(token: String, newPassword: String): MessageResponse

    suspend fun preregister(request: PreregisterRequest): PreregisterResponse

    suspend fun completeProfile(request: CompleteProfileRequest): User

    suspend fun resendVerificationEmail(email: String): MessageResponse

    // Medical API methods
    suspend fun getClinics(): List<Clinic>

    suspend fun getClinic(id: String): Clinic

    suspend fun getClinicAvailability(clinicId: String): List<AppointmentSlot>

    suspend fun getAppointments(): List<Appointment>

    suspend fun bookClinicAppointment(request: ClinicBookingRequest): Appointment

    suspend fun cancelAppointment(id: String): Appointment

    suspend fun rescheduleAppointment(id: String, request: ClinicBookingRequest): Appointment

    suspend fun logMedication(request: MedicationLogRequest): MedicationLog

    suspend fun getMedicationLogHistory(): List<MedicationLog>

    suspend fun getMedicationAgenda(date: String): List<MedicationLog>

    suspend fun getRoutineAgenda(date: String): List<RoutineDayAgenda>

    suspend fun logMood(request: MoodEntryRequest): Mood

    suspend fun getMoodHistory(): List<Mood>

    suspend fun getUserMedications(): List<Medication>

    suspend fun upsertMedication(id: String, request: MedicationCreateRequest): Medication

    suspend fun deleteMedication(id: String)

    suspend fun getUserRoutines(): List<Routine>

    suspend fun upsertRoutine(id: String, request: RoutineCreateRequest): Routine

    suspend fun deleteRoutine(id: String)

    suspend fun getRoutineOccurrenceOverrides(): List<RoutineOccurrenceOverride>

    suspend fun upsertRoutineOccurrenceOverride(request: RoutineOccurrenceOverrideRequest): RoutineOccurrenceOverride

    suspend fun getMedicalRecords(sort: String): List<MedicalRecordResponse>

    suspend fun uploadMedicalRecord(fileBytes: ByteArray, fileName: String, category: MedicalRecordCategory)

    suspend fun downloadMedicalRecord(id: String): ByteArray

    suspend fun deleteMedicalRecord(id: String)

    suspend fun getEducationCategories(): List<CategoryDto>

    suspend fun getEducationArticles(): List<ArticleSummaryDto>

    suspend fun getEducationArticle(id: String): ArticleDetailDto

    suspend fun getEducationQuiz(id: String): QuizDto

    suspend fun submitEducationQuiz(quizId: String, request: QuizSubmissionRequestDto): QuizSubmissionResultDto

    suspend fun getEducationQuizStats(): UserQuizStatsDto

    suspend fun getEducationEarnedBadges(): List<EarnedBadgeDto>
}

class ApiServiceImpl(private val client: HttpClient) : ApiService {
    override suspend fun getHealth(): Map<String, String> = client.get("/api/health").body()

    override suspend fun getProducts(): List<ProductDto> = client.get("/api/products").body()

    override suspend fun getProduct(id: String): ProductDto = client.get("/api/products/$id").body()

    override suspend fun login(request: LoginRequest): AuthResponse =
        handleAuthResponse(client.post("/api/auth/login") { setBody(request) })

    override suspend fun refreshToken(request: RefreshTokenRequest): TokenResponse =
        client.post("/api/auth/refresh") { setBody(request) }.body()

    override suspend fun logout() {
        client.post("/api/auth/logout")
    }

    override suspend fun getProfile(): User = client.get("/api/auth/profile").body()

    override suspend fun activateAccount(token: String): AuthResponse = handleAuthResponse(
        client.post("/api/auth/activate") {
            setBody(mapOf("token" to token))
        },
    )

    override suspend fun forgotPassword(email: String): MessageResponse = client.post("/api/auth/forgot-password") {
        setBody(ForgotPasswordRequest(email))
    }.body()

    override suspend fun resetPassword(token: String, newPassword: String): MessageResponse =
        client.post("/api/auth/reset-password") {
            setBody(ResetPasswordRequest(token, newPassword))
        }.body()

    override suspend fun preregister(request: PreregisterRequest): PreregisterResponse =
        client.post("/api/auth/preregister") { setBody(request) }.body()

    override suspend fun completeProfile(request: CompleteProfileRequest): User =
        client.post("/api/auth/complete-profile") { setBody(request) }.body()

    override suspend fun resendVerificationEmail(email: String): MessageResponse =
        client.post("/api/auth/resend-verification") {
            setBody(ResendVerificationRequest(email))
        }.body()

    // --- Medical API ---
    override suspend fun getClinics(): List<Clinic> = client.get("/api/clinics").body()

    override suspend fun getClinic(id: String): Clinic = client.get("/api/clinics/$id").body()

    override suspend fun getClinicAvailability(clinicId: String): List<AppointmentSlot> =
        client.get("/api/clinics/$clinicId/slots").body()

    override suspend fun getAppointments(): List<Appointment> = client.get("/api/appointments").body()

    override suspend fun bookClinicAppointment(request: ClinicBookingRequest): Appointment =
        client.post("/api/appointments") { setBody(request) }.body()

    override suspend fun cancelAppointment(id: String): Appointment = client.post("/api/appointments/$id/cancel").body()

    override suspend fun rescheduleAppointment(id: String, request: ClinicBookingRequest): Appointment =
        client.post("/api/appointments/$id/reschedule") { setBody(request) }.body()

    override suspend fun logMedication(request: MedicationLogRequest): MedicationLog =
        client.post("/api/medications/logs") { setBody(request) }.body()

    override suspend fun getMedicationLogHistory(): List<MedicationLog> = client.get("/api/medications/logs").body()

    override suspend fun getMedicationAgenda(date: String): List<MedicationLog> =
        client.get("/api/medications/agenda?date=$date").body()

    override suspend fun getRoutineAgenda(date: String): List<RoutineDayAgenda> =
        client.get("/api/routines/agenda?date=$date").body()

    override suspend fun logMood(request: MoodEntryRequest): Mood =
        client.post("/api/moods") { setBody(request) }.body()

    override suspend fun getMoodHistory(): List<Mood> = client.get("/api/moods").body()

    override suspend fun getUserMedications(): List<Medication> = client.get("/api/medications").body()

    override suspend fun upsertMedication(id: String, request: MedicationCreateRequest): Medication =
        client.put("/api/medications/$id") { setBody(request) }.body()

    override suspend fun deleteMedication(id: String) {
        client.delete("/api/medications/$id")
    }

    override suspend fun getUserRoutines(): List<Routine> = client.get("/api/routines").body()

    override suspend fun upsertRoutine(id: String, request: RoutineCreateRequest): Routine =
        client.put("/api/routines/$id") { setBody(request) }.body()

    override suspend fun deleteRoutine(id: String) {
        client.delete("/api/routines/$id")
    }

    override suspend fun getRoutineOccurrenceOverrides(): List<RoutineOccurrenceOverride> =
        client.get("/api/routines/occurrence-overrides").body()

    override suspend fun upsertRoutineOccurrenceOverride(
        request: RoutineOccurrenceOverrideRequest,
    ): RoutineOccurrenceOverride = client.put("/api/routines/occurrence-overrides") { setBody(request) }.body()

    override suspend fun getMedicalRecords(sort: String): List<MedicalRecordResponse> =
        client.get("/api/medical-records/user?sort=$sort").body()

    override suspend fun uploadMedicalRecord(fileBytes: ByteArray, fileName: String, category: MedicalRecordCategory) {
        client.submitFormWithBinaryData(
            url = "/api/medical-records/upload",
            formData = formData {
                append("category", category.name)
                append(
                    "file",
                    fileBytes,
                    Headers.build {
                        append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                    },
                )
            },
        )
    }

    override suspend fun downloadMedicalRecord(id: String): ByteArray =
        client.get("/api/medical-records/download/$id").body()

    override suspend fun deleteMedicalRecord(id: String) {
        client.delete("/api/medical-records/$id")
    }

    override suspend fun getEducationCategories(): List<CategoryDto> = client.get("/api/categories").body()

    override suspend fun getEducationArticles(): List<ArticleSummaryDto> = client.get("/api/articles").body()

    override suspend fun getEducationArticle(id: String): ArticleDetailDto = client.get("/api/articles/$id").body()

    override suspend fun getEducationQuiz(id: String): QuizDto = client.get("/api/quizzes/$id").body()

    override suspend fun submitEducationQuiz(
        quizId: String,
        request: QuizSubmissionRequestDto,
    ): QuizSubmissionResultDto = client.post("/api/quizzes/$quizId/submit") { setBody(request) }.body()

    override suspend fun getEducationQuizStats(): UserQuizStatsDto = client.get("/api/users/quiz-stats").body()

    override suspend fun getEducationEarnedBadges(): List<EarnedBadgeDto> = client.get("/api/badges/earned").body()

    /**
     * Catches JsonConvertException when server returns an error response like {"error": "..."}
     * instead of the expected AuthResponse format.
     */
    private suspend fun handleAuthResponse(response: io.ktor.client.statement.HttpResponse): AuthResponse = try {
        response.body()
    } catch (e: JsonConvertException) {
        // Extract the actual error message from the response body
        val responseException = e.cause as? ResponseException
        if (responseException != null) {
            val rawBody = responseException.response.bodyAsText()
            val errorMessage = rawBody.extractErrorMessage()
            throw ApiException(
                statusCode = responseException.response.status.value,
                message = errorMessage ?: "Authentication failed",
                cause = e,
            )
        }
        throw ApiException(statusCode = 500, message = "Invalid response from server", cause = e)
    }

    private fun String.extractErrorMessage(): String? = try {
        Json.parseToJsonElement(this)
            .jsonObject["error"]
            ?.jsonPrimitive
            ?.content
    } catch (_: Exception) {
        null
    }
}
