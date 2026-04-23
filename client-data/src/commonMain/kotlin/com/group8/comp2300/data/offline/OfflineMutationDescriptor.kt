package com.group8.comp2300.data.offline

import com.group8.comp2300.domain.model.medical.MedicationCreateRequest
import com.group8.comp2300.domain.model.medical.MedicationLogRequest
import com.group8.comp2300.domain.model.medical.MoodEntryRequest
import com.group8.comp2300.domain.model.medical.RoutineCreateRequest
import com.group8.comp2300.domain.model.medical.RoutineOccurrenceOverrideRequest
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer

data class OfflineMutationDescriptor<T>(val type: String, val serializer: KSerializer<T>)

object MedicalOfflineMutations {
    val medicationUpsert = OfflineMutationDescriptor("MEDICATION_UPSERT", MedicationCreateRequest.serializer())
    val medicationDelete = OfflineMutationDescriptor("MEDICATION_DELETE", Unit.serializer())
    val routineUpsert = OfflineMutationDescriptor("ROUTINE_UPSERT", RoutineCreateRequest.serializer())
    val routineDelete = OfflineMutationDescriptor("ROUTINE_DELETE", Unit.serializer())
    val routineOccurrenceOverrideUpsert = OfflineMutationDescriptor(
        "ROUTINE_OCCURRENCE_OVERRIDE_UPSERT",
        RoutineOccurrenceOverrideRequest.serializer(),
    )
    val medicationLog = OfflineMutationDescriptor("MEDICATION_LOG", MedicationLogRequest.serializer())
    val mood = OfflineMutationDescriptor("MOOD", MoodEntryRequest.serializer())
}
