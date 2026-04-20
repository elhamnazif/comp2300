package com.group8.comp2300.service.medication

import com.group8.comp2300.domain.model.medical.Medication
import com.group8.comp2300.domain.model.medical.MedicationCreateRequest
import com.group8.comp2300.domain.model.medical.MedicationLog
import com.group8.comp2300.domain.model.medical.MedicationLogLinkMode
import com.group8.comp2300.domain.model.medical.MedicationLogRequest
import com.group8.comp2300.domain.model.medical.MedicationLogStatus
import com.group8.comp2300.domain.model.medical.MedicationStatus
import com.group8.comp2300.domain.model.medical.MedicationUnit
import com.group8.comp2300.domain.model.medical.Routine
import com.group8.comp2300.domain.model.medical.RoutineCreateRequest
import com.group8.comp2300.domain.model.medical.RoutineOccurrenceOverride
import com.group8.comp2300.domain.model.medical.RoutineOccurrenceOverrideRequest
import com.group8.comp2300.domain.model.medical.RoutineRepeatType
import com.group8.comp2300.domain.model.medical.RoutineStatus
import com.group8.comp2300.domain.model.medical.buildRoutineDayAgenda
import com.group8.comp2300.domain.repository.MedicationLogRepository
import com.group8.comp2300.domain.repository.MedicationRepository
import com.group8.comp2300.domain.repository.RoutineOccurrenceOverrideRepository
import com.group8.comp2300.domain.repository.RoutineRepository
import io.ktor.http.HttpStatusCode
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime
import java.util.UUID
import kotlin.time.Instant

sealed class MedicationResult<out T> {
    data class Success<T>(val data: T) : MedicationResult<T>()
    data class Error(val status: HttpStatusCode, val message: String) : MedicationResult<Nothing>()
}

class MedicationService(
    private val medicationRepository: MedicationRepository,
    private val routineRepository: RoutineRepository,
    private val routineOccurrenceOverrideRepository: RoutineOccurrenceOverrideRepository,
    private val medicationLogRepository: MedicationLogRepository
) {

    // --- Medication Logic ---

    fun getAllMedications(userId: String): List<Medication> {
        return medicationRepository.getAllByUserId(userId)
    }

    fun putMedication(userId: String, id: String, request: MedicationCreateRequest): MedicationResult<Medication> {
        val medicationResult = toMedication(userId, id, request)
        if (medicationResult is MedicationResult.Error) return medicationResult

        val medication = (medicationResult as MedicationResult.Success).data
        val existing = medicationRepository.getById(id)

        if (existing != null && existing.userId != userId) {
            return MedicationResult.Error(HttpStatusCode.NotFound, "Medication not found")
        }

        if (existing == null) {
            medicationRepository.insert(medication)
        } else {
            medicationRepository.update(medication)
        }
        return MedicationResult.Success(medication)
    }

    fun deleteMedication(userId: String, id: String): MedicationResult<Unit> {
        val medication = medicationRepository.getById(id)
        if (medication == null || medication.userId != userId) {
            return MedicationResult.Error(HttpStatusCode.NotFound, "Medication not found")
        }
        medicationRepository.delete(id)
        return MedicationResult.Success(Unit)
    }

    // --- Medication Log Logic ---

    fun getMedicationLogs(userId: String): List<MedicationLog> {
        return medicationLogRepository.getHistory(userId)
    }

    fun logMedication(userId: String, request: MedicationLogRequest): MedicationResult<MedicationLog> {
        val medication = medicationRepository.getById(request.medicationId)
        if (medication == null || medication.userId != userId) {
            return MedicationResult.Error(HttpStatusCode.NotFound, "Medication not found")
        }

        val status = runCatching { MedicationLogStatus.valueOf(request.status) }.getOrElse {
            return MedicationResult.Error(HttpStatusCode.BadRequest, "Invalid medication log status")
        }

        val linkMode = request.linkMode ?: if (request.routineId != null && request.occurrenceTimeMs != null) {
            MedicationLogLinkMode.ATTACH_TO_OCCURRENCE
        } else {
            MedicationLogLinkMode.EXTRA_DOSE
        }

        if (linkMode == MedicationLogLinkMode.ATTACH_TO_OCCURRENCE &&
            (request.routineId == null || request.occurrenceTimeMs == null)
        ) {
            return MedicationResult.Error(
                HttpStatusCode.BadRequest,
                "routineId and occurrenceTimeMs are required when attaching to a scheduled dose"
            )
        }

        val routineId = request.routineId
        if (linkMode == MedicationLogLinkMode.ATTACH_TO_OCCURRENCE && routineId != null) {
            val routine = routineRepository.getById(routineId)
            if (routine == null || routine.userId != userId || request.medicationId !in routine.medicationIds) {
                return MedicationResult.Error(HttpStatusCode.BadRequest, "Medication is not linked to the routine")
            }
        }

        val timestamp = request.timestampMs ?: System.currentTimeMillis()
        val logId = if (
            linkMode == MedicationLogLinkMode.ATTACH_TO_OCCURRENCE &&
            request.routineId != null &&
            request.occurrenceTimeMs != null
        ) {
            "${request.routineId}:${request.medicationId}:${request.occurrenceTimeMs}"

        } else {
            UUID.randomUUID().toString()
        }

        val log = MedicationLog(
            id = logId,
            medicationId = request.medicationId,
            medicationTime = timestamp,
            status = status,
            routineId = request.routineId,
            occurrenceTimeMs = request.occurrenceTimeMs,
            medicationName = medication.name,
            routineName = request.routineId?.let { routineRepository.getById(it)?.name },
        )
        medicationLogRepository.insert(log)
        return MedicationResult.Success(log)
    }

    // --- Routine Logic ---

    fun getAllRoutines(userId: String): List<Routine> {
        return routineRepository.getAllByUserId(userId)
    }

    fun putRoutine(userId: String, id: String, request: RoutineCreateRequest): MedicationResult<Routine> {
        val routineResult = toRoutine(userId, id, request)
        if (routineResult is MedicationResult.Error) return routineResult

        val routine = (routineResult as MedicationResult.Success).data
        val existing = routineRepository.getById(id)

        if (existing != null && existing.userId != userId) {
            return MedicationResult.Error(HttpStatusCode.NotFound, "Routine not found")
        }

        if (existing == null) {
            routineRepository.insert(routine)
        } else {
            routineRepository.update(routine)
        }
        return MedicationResult.Success(routine)
    }

    fun deleteRoutine(userId: String, id: String): MedicationResult<Unit> {
        val routine = routineRepository.getById(id)
        if (routine == null || routine.userId != userId) {
            return MedicationResult.Error(HttpStatusCode.NotFound, "Routine not found")
        }
        routineRepository.delete(id)
        return MedicationResult.Success(Unit)
    }

    // --- Routine Override Logic ---

    fun getRoutineOverrides(userId: String): List<RoutineOccurrenceOverride> {
        return routineOccurrenceOverrideRepository.getAllByUserId(userId)
    }

    fun putRoutineOverride(
        userId: String,
        request: RoutineOccurrenceOverrideRequest
    ): MedicationResult<RoutineOccurrenceOverride> {
        val overrideResult = toRoutineOccurrenceOverride(userId, request)
        if (overrideResult is MedicationResult.Error) return overrideResult

        val override = (overrideResult as MedicationResult.Success).data
        routineOccurrenceOverrideRepository.insert(override)
        return MedicationResult.Success(override)
    }

    // --- Agenda Logic ---

    fun getAgenda(userId: String, dateString: String): MedicationResult<Any> {
        val date = runCatching { LocalDate.parse(dateString) }.getOrElse {
            return MedicationResult.Error(HttpStatusCode.BadRequest, "date must be in YYYY-MM-DD format")
        }
        val medications = medicationRepository.getAllByUserId(userId)
        val routines = routineRepository.getAllByUserId(userId)
        val overrides = routineOccurrenceOverrideRepository.getAllByUserId(userId)
        val logs = medicationLogRepository.getDailyAgenda(userId, dateString)
        val agenda = buildRoutineDayAgenda(
            routines = routines,
            medications = medications,
            logs = logs,
            overrides = overrides,
            date = date,
            nowMs = System.currentTimeMillis(),
            timeZone = TimeZone.currentSystemDefault(),
        )
        return MedicationResult.Success(agenda)
    }

    // --- Private mapping and validation functions ---

    private fun toMedication(
        userId: String,
        id: String,
        request: MedicationCreateRequest,
    ): MedicationResult<Medication> {
        if (request.name.isBlank()) {
            return MedicationResult.Error(HttpStatusCode.BadRequest, "Medication name is required")
        }
        val doseAmount = request.doseAmount.trim()
        val doseAmountValue = doseAmount.toDoubleOrNull()
        if (doseAmountValue == null || doseAmountValue <= 0.0) {
            return MedicationResult.Error(HttpStatusCode.BadRequest, "Dose amount must be a positive number")
        }
        val doseUnit = runCatching { MedicationUnit.valueOf(request.doseUnit) }.getOrElse {
            return MedicationResult.Error(HttpStatusCode.BadRequest, "Invalid dose unit")
        }
        val customDoseUnit = request.customDoseUnit?.trim()?.takeIf(String::isNotBlank)
        if (doseUnit == MedicationUnit.OTHER && customDoseUnit == null) {
            return MedicationResult.Error(HttpStatusCode.BadRequest, "Custom dose unit is required")
        }
        val stockAmount = request.stockAmount.trim()
        val stockAmountValue = stockAmount.toDoubleOrNull()
        if (stockAmountValue == null || stockAmountValue <= 0.0) {
            return MedicationResult.Error(HttpStatusCode.BadRequest, "Stock amount must be a positive number")
        }
        val stockUnit = runCatching { MedicationUnit.valueOf(request.stockUnit) }.getOrElse {
            return MedicationResult.Error(HttpStatusCode.BadRequest, "Invalid stock unit")
        }
        val customStockUnit = request.customStockUnit?.trim()?.takeIf(String::isNotBlank)
        if (stockUnit == MedicationUnit.OTHER && customStockUnit == null) {
            return MedicationResult.Error(HttpStatusCode.BadRequest, "Custom stock unit is required")
        }
        val status = runCatching { MedicationStatus.valueOf(request.status) }.getOrElse {
            return MedicationResult.Error(HttpStatusCode.BadRequest, "Invalid medication status")
        }
        return MedicationResult.Success(Medication(
            id = id,
            userId = userId,
            name = request.name.trim(),
            doseAmount = doseAmount,
            doseUnit = doseUnit,
            customDoseUnit = customDoseUnit.takeIf { doseUnit == MedicationUnit.OTHER },
            stockAmount = stockAmount,
            stockUnit = stockUnit,
            customStockUnit = customStockUnit.takeIf { stockUnit == MedicationUnit.OTHER },
            instruction = request.instruction?.trim()?.takeIf(String::isNotEmpty),
            colorHex = request.colorHex ?: Medication.PRESET_COLORS.random(),
            status = status,
        ))
    }

    private fun toRoutine(
        userId: String,
        id: String,
        request: RoutineCreateRequest,
    ): MedicationResult<Routine> {
        if (request.name.isBlank()) {
            return MedicationResult.Error(HttpStatusCode.BadRequest, "Routine name is required")
        }
        if (request.startDate.isBlank()) {
            return MedicationResult.Error(HttpStatusCode.BadRequest, "Start date is required")
        }
        val repeatType = runCatching { RoutineRepeatType.valueOf(request.repeatType) }.getOrElse {
            return MedicationResult.Error(HttpStatusCode.BadRequest, "Invalid routine repeat type")
        }
        val status = runCatching { RoutineStatus.valueOf(request.status) }.getOrElse {
            return MedicationResult.Error(HttpStatusCode.BadRequest, "Invalid routine status")
        }
        val dateRegex = Regex("^\\d{4}-\\d{2}-\\d{2}$")
        val endDate = request.endDate
        if (!dateRegex.matches(request.startDate) || (endDate != null && !dateRegex.matches(endDate))) {
            return MedicationResult.Error(HttpStatusCode.BadRequest, "Dates must be in YYYY-MM-DD format")
        }
        if (repeatType == RoutineRepeatType.WEEKLY && request.daysOfWeek.isEmpty()) {
            return MedicationResult.Error(HttpStatusCode.BadRequest, "Select at least one day of week")
        }
        if (repeatType == RoutineRepeatType.DAILY && request.daysOfWeek.isNotEmpty()) {
            return MedicationResult.Error(HttpStatusCode.BadRequest, "Daily routines cannot declare day selections")
        }
        val normalizedTimes = request.timesOfDayMs.sorted().distinct()
        if (normalizedTimes.isEmpty()) {
            return MedicationResult.Error(HttpStatusCode.BadRequest, "Add at least one schedule time")
        }
        val dayMs = 24L * 60L * 60L * 1000L
        if (normalizedTimes.any { it < 0L || it >= dayMs }) {
            return MedicationResult.Error(HttpStatusCode.BadRequest, "Schedule times must be within the day")
        }
        val allowedOffsets = setOf(0, 5, 10, 15, 30, 60)
        if (request.reminderOffsetsMins.any { it !in allowedOffsets }) {
            return MedicationResult.Error(HttpStatusCode.BadRequest, "Unsupported reminder offset")
        }
        val linkedMedications = medicationRepository.getAllByUserId(userId).associateBy(Medication::id)
        val invalidLink =
            request.medicationIds.any { medicationId ->
                val medication = linkedMedications[medicationId]
                medication == null || medication.status != MedicationStatus.ACTIVE
            }
        if (invalidLink) {
            return MedicationResult.Error(HttpStatusCode.BadRequest, "Schedules can only link active medications")
        }
        return MedicationResult.Success(Routine(
            id = id,
            userId = userId,
            name = request.name.trim(),
            timesOfDayMs = normalizedTimes,
            repeatType = repeatType,
            daysOfWeek = request.daysOfWeek.sorted().distinct(),
            startDate = request.startDate,
            endDate = request.endDate?.takeIf(String::isNotBlank),
            hasReminder = request.hasReminder,
            reminderOffsetsMins = if (request.hasReminder) {
                request.reminderOffsetsMins.sorted().distinct()
            } else {
                emptyList()
            },
            status = status,
            medicationIds = request.medicationIds.distinct(),
        ))
    }

    private fun toRoutineOccurrenceOverride(
        userId: String,
        request: RoutineOccurrenceOverrideRequest,
    ): MedicationResult<RoutineOccurrenceOverride> {
        val routine = routineRepository.getById(request.routineId)
        if (routine == null || routine.userId != userId) {
            return MedicationResult.Error(HttpStatusCode.NotFound, "Schedule not found")
        }
        if (routine.status != RoutineStatus.ACTIVE) {
            return MedicationResult.Error(HttpStatusCode.BadRequest, "Only active schedules can be moved")
        }
        if (request.rescheduledOccurrenceTimeMs <= 0L) {
            return MedicationResult.Error(HttpStatusCode.BadRequest, "Invalid rescheduled occurrence time")
        }
        val timeZone = TimeZone.currentSystemDefault()
        val originalDateTime = Instant.fromEpochMilliseconds(request.originalOccurrenceTimeMs).toLocalDateTime(timeZone)
        val originalDate = originalDateTime.date
        val originalTimeOfDayMs = originalDateTime.toTimeOfDayMs()
        val validTimes = routine.timesOfDayMs.sorted().distinct()
        if (!routine.isDueOn(originalDate) || originalTimeOfDayMs !in validTimes) {
            return MedicationResult.Error(
                HttpStatusCode.BadRequest,
                "Original occurrence does not belong to this schedule"
            )
        }
        return MedicationResult.Success(RoutineOccurrenceOverride(
            id = "${routine.id}:${request.originalOccurrenceTimeMs}",
            routineId = routine.id,
            originalOccurrenceTimeMs = request.originalOccurrenceTimeMs,
            rescheduledOccurrenceTimeMs = request.rescheduledOccurrenceTimeMs,
        ))
    }

    private fun Routine.isDueOn(date: LocalDate): Boolean {
        val start = LocalDate.parse(startDate)
        val end = endDate?.takeIf(String::isNotBlank)?.let(LocalDate::parse)
        if (date < start || (end != null && date > end)) return false
        return when (repeatType) {
            RoutineRepeatType.DAILY -> true
            RoutineRepeatType.WEEKLY -> {
                val selected = daysOfWeek.toSet()
                selected.isNotEmpty() && date.dayOfWeek.toStorageDayOfWeek() in selected
            }
        }
    }

    private fun kotlinx.datetime.LocalDateTime.toTimeOfDayMs(): Long =
        ((hour * 60L + minute.toLong()) * 60L * 1000L) + (second * 1000L)

    private fun kotlinx.datetime.DayOfWeek.toStorageDayOfWeek(): Int = isoDayNumber % 7
}
