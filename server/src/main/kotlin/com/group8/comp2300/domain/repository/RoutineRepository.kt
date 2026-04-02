package com.group8.comp2300.domain.repository

import com.group8.comp2300.domain.model.medical.Routine

interface RoutineRepository {
    fun getAllByUserId(userId: String): List<Routine>

    fun getById(id: String): Routine?

    fun insert(routine: Routine)

    fun update(routine: Routine)

    fun delete(id: String)
}
