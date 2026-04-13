package com.group8.comp2300.data.repository

import com.group8.comp2300.data.local.AppointmentLocalDataSource
import com.group8.comp2300.data.local.CalendarOverviewLocalDataSource
import com.group8.comp2300.data.local.MedicationLogLocalDataSource
import com.group8.comp2300.data.local.MoodLocalDataSource
import com.group8.comp2300.data.remote.ApiService
import com.group8.comp2300.domain.model.medical.Appointment
import com.group8.comp2300.domain.model.medical.AppointmentRequest
import com.group8.comp2300.domain.model.medical.CalendarOverviewResponse
import com.group8.comp2300.domain.model.medical.LabResult
import com.group8.comp2300.domain.model.medical.MedicalRecordCategory
import com.group8.comp2300.domain.model.medical.MedicalRecordResponse
import com.group8.comp2300.domain.model.medical.MedicationLog
import com.group8.comp2300.domain.model.medical.MedicationLogRequest
import com.group8.comp2300.domain.model.medical.Mood
import com.group8.comp2300.domain.model.medical.MoodEntryRequest
import com.group8.comp2300.domain.repository.MedicalRepository
import com.group8.comp2300.mock.sampleResults

class MedicalRepositoryImpl(
    private val apiService: ApiService,
    private val appointmentLocal: AppointmentLocalDataSource,
    private val moodLocal: MoodLocalDataSource,
    private val medicationLogLocal: MedicationLogLocalDataSource,
    private val calendarOverviewLocal: CalendarOverviewLocalDataSource,
) : MedicalRepository {

    override fun getRecentLabResults(): List<LabResult> = sampleResults

    // ---------- READS: cache-first, then refresh from network ----------

    override suspend fun getCalendarOverview(year: Int, month: Int): List<CalendarOverviewResponse> {
        // Return cached data first
        val cached = calendarOverviewLocal.getByYearMonth(year, month)

        return try {
            val remote = apiService.getCalendarOverview(year, month)
            calendarOverviewLocal.replaceForYearMonth(year, month, remote)
            remote
        } catch (_: Exception) {
            // Offline — return stale cache
            cached
        }
    }

    override suspend fun getAppointments(): List<Appointment> {
        val cached = appointmentLocal.getAll()

        return try {
            val remote = apiService.getAppointments()
            appointmentLocal.replaceAll(remote)
            remote
        } catch (_: Exception) {
            cached
        }
    }

    override suspend fun getMedicationAgenda(date: String): List<MedicationLog> {
        val cached = medicationLogLocal.getAll()

        return try {
            val remote = apiService.getMedicationAgenda(date)
            medicationLogLocal.replaceAll(remote)
            remote
        } catch (_: Exception) {
            cached
        }
    }

    // ---------- WRITES: save locally + queue for sync on failure ----------

    override suspend fun scheduleAppointment(request: AppointmentRequest): Appointment = try {
        val remote = apiService.scheduleAppointment(request)
        appointmentLocal.insert(remote)
        remote
    } catch (_: Exception) {
        // Return an optimistic local placeholder
        val placeholder = Appointment(
            id = "local-${kotlin.uuid.Uuid.random()}",
            userId = "",
            title = request.title,
            appointmentTime = request.appointmentTime,
            appointmentType = request.appointmentType,
            clinicId = null,
            bookingId = null,
            status = "PENDING_SYNC",
            notes = request.notes,
            hasReminder = true,
            paymentStatus = "PENDING",
        )
        appointmentLocal.insert(placeholder)
        placeholder
    }

    override suspend fun logMedication(request: MedicationLogRequest): MedicationLog = try {
        val remote = apiService.logMedication(request)
        medicationLogLocal.insert(remote)
        remote
    } catch (_: Exception) {
        // Return optimistic local placeholder
        val placeholder = MedicationLog(
            id = "local-${kotlin.uuid.Uuid.random()}",
            medicationId = request.medicationId,
            medicationTime = request.timestampMs ?: kotlin.time.Clock.System.now().toEpochMilliseconds(),
            status = com.group8.comp2300.domain.model.medical.MedicationLogStatus.valueOf(request.status),
            medicationName = null,
        )
        medicationLogLocal.insert(placeholder)
        placeholder
    }

    override suspend fun logMood(request: MoodEntryRequest): Mood = try {
        val remote = apiService.logMood(request)
        moodLocal.insert(remote)
        remote
    } catch (_: Exception) {
        // Return optimistic local placeholder
        val moodType = when {
            request.moodScore >= 5 -> com.group8.comp2300.domain.model.medical.MoodType.GREAT
            request.moodScore >= 4 -> com.group8.comp2300.domain.model.medical.MoodType.GOOD
            request.moodScore >= 3 -> com.group8.comp2300.domain.model.medical.MoodType.NEUTRAL
            request.moodScore >= 2 -> com.group8.comp2300.domain.model.medical.MoodType.SAD
            else -> com.group8.comp2300.domain.model.medical.MoodType.VERY_SAD
        }
        val placeholder = Mood(
            id = "local-${kotlin.uuid.Uuid.random()}",
            userId = "",
            timestamp = kotlin.time.Clock.System.now().toEpochMilliseconds(),
            moodType = moodType,
            feeling = request.tags.joinToString(", "),
            journal = request.notes,
        )
        moodLocal.insert(placeholder)
        placeholder
    }

    override suspend fun getMedicalRecords(sort: String): List<MedicalRecordResponse> = try {
        apiService.getMedicalRecords(sort)
    } catch (_: Exception) {
        emptyList()
    }

    override suspend fun uploadMedicalRecord(
        fileBytes: ByteArray,
        fileName: String,
        category: MedicalRecordCategory,
    ): Boolean = try {
        apiService.uploadMedicalRecord(fileBytes, fileName, category)
        true
    } catch (_: Exception) {
        false
    }

    override suspend fun downloadMedicalRecord(id: String): ByteArray =
        apiService.downloadMedicalRecord(id)

    override suspend fun deleteMedicalRecord(id: String) {
        try {
            apiService.deleteMedicalRecord(id)
        } catch (_: Exception) {
            // TODO: queue for sync
        }
    }
}
