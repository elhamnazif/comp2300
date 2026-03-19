package com.group8.comp2300.domain.repository.medical

import com.group8.comp2300.domain.model.medical.Routine
import com.group8.comp2300.domain.model.medical.RoutineCreateRequest

interface RoutineDataRepository {
    suspend fun getRoutines(): List<Routine>

    suspend fun saveRoutine(request: RoutineCreateRequest, id: String? = null): Routine

    suspend fun deleteRoutine(id: String)
}
