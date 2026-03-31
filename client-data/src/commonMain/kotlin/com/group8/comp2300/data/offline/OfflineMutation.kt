package com.group8.comp2300.data.offline

import com.group8.comp2300.domain.model.medical.AppointmentRequest
import com.group8.comp2300.domain.model.medical.MedicationCreateRequest
import com.group8.comp2300.domain.model.medical.MedicationLogRequest
import com.group8.comp2300.domain.model.medical.MoodEntryRequest
import com.group8.comp2300.domain.model.medical.RoutineCreateRequest
import com.group8.comp2300.domain.model.medical.RoutineOccurrenceOverrideRequest
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer

data class OfflineMutationSpec<T>(
    val type: String,
    val serializer: KSerializer<T>,
)

object MedicalOfflineMutations {
    val appointment = OfflineMutationSpec("APPOINTMENT", AppointmentRequest.serializer())
    val medicationUpsert = OfflineMutationSpec("MEDICATION_UPSERT", MedicationCreateRequest.serializer())
    val medicationDelete = OfflineMutationSpec("MEDICATION_DELETE", Unit.serializer())
    val routineUpsert = OfflineMutationSpec("ROUTINE_UPSERT", RoutineCreateRequest.serializer())
    val routineDelete = OfflineMutationSpec("ROUTINE_DELETE", Unit.serializer())
    val routineOccurrenceOverrideUpsert = OfflineMutationSpec(
        "ROUTINE_OCCURRENCE_OVERRIDE_UPSERT",
        RoutineOccurrenceOverrideRequest.serializer(),
    )
    val medicationLog = OfflineMutationSpec("MEDICATION_LOG", MedicationLogRequest.serializer())
    val mood = OfflineMutationSpec("MOOD", MoodEntryRequest.serializer())
}
