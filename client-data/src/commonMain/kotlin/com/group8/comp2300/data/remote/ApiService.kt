package com.group8.comp2300.data.remote

import com.group8.comp2300.data.remote.dto.AuthResponse
import com.group8.comp2300.data.remote.dto.CompleteProfileRequest
import com.group8.comp2300.data.remote.dto.ForgotPasswordRequest
import com.group8.comp2300.data.remote.dto.LoginRequest
import com.group8.comp2300.data.remote.dto.MessageResponse
import com.group8.comp2300.data.remote.dto.PreregisterRequest
import com.group8.comp2300.data.remote.dto.PreregisterResponse
import com.group8.comp2300.data.remote.dto.ProductDto
import com.group8.comp2300.data.remote.dto.RefreshTokenRequest
import com.group8.comp2300.data.remote.dto.RegisterRequest
import com.group8.comp2300.data.remote.dto.ResendVerificationRequest
import com.group8.comp2300.data.remote.dto.ResetPasswordRequest
import com.group8.comp2300.data.remote.dto.TokenResponse
import com.group8.comp2300.domain.model.medical.Appointment
import com.group8.comp2300.domain.model.medical.AppointmentRequest
import com.group8.comp2300.domain.model.medical.CalendarOverviewResponse
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
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.JsonConvertException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

interface ApiService {
    suspend fun getHealth(): Map<String, String>

    suspend fun getProducts(): List<ProductDto>

    suspend fun getProduct(id: String): ProductDto

    suspend fun register(request: RegisterRequest): AuthResponse

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
    suspend fun getCalendarOverview(year: Int, month: Int): List<CalendarOverviewResponse>

    suspend fun getAppointments(): List<Appointment>

    suspend fun scheduleAppointment(request: AppointmentRequest): Appointment

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

    suspend fun uploadMedicalRecord(fileBytes: ByteArray, fileName: String)

    suspend fun deleteMedicalRecord(id: String)
}

class ApiServiceImpl(private val client: HttpClient) : ApiService {
    override suspend fun getHealth(): Map<String, String> = client.get("/api/health").body()

    override suspend fun getProducts(): List<ProductDto> = client.get("/api/products").body()

    override suspend fun getProduct(id: String): ProductDto = client.get("/api/products/$id").body()

    override suspend fun register(request: RegisterRequest): AuthResponse =
        handleAuthResponse(client.post("/api/auth/register") { setBody(request) })

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
    override suspend fun getCalendarOverview(year: Int, month: Int): List<CalendarOverviewResponse> =
        client.get("/api/calendar/overview?year=$year&month=$month").body()

    override suspend fun getAppointments(): List<Appointment> = client.get("/api/appointments").body()

    override suspend fun scheduleAppointment(request: AppointmentRequest): Appointment =
        client.post("/api/appointments") { setBody(request) }.body()

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
        client.get("/api/medical-records?sort=$sort").body()

    override suspend fun uploadMedicalRecord(fileBytes: ByteArray, fileName: String) {
        // TODO: multipart upload when server endpoint is ready
        client.post("/api/medical-records")
    }

    override suspend fun deleteMedicalRecord(id: String) {
        client.post("/api/medical-records/$id/delete")
    }

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
