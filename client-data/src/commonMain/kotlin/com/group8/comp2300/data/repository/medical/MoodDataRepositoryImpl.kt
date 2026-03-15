package com.group8.comp2300.data.repository.medical

import com.group8.comp2300.data.local.MoodLocalDataSource
import com.group8.comp2300.data.offline.OutboxEntityType
import com.group8.comp2300.data.offline.QueuedWriteDispatcher
import com.group8.comp2300.domain.model.medical.Mood
import com.group8.comp2300.domain.model.medical.MoodEntryRequest
import com.group8.comp2300.domain.model.medical.MoodType
import com.group8.comp2300.domain.model.session.userOrNull
import com.group8.comp2300.domain.repository.AuthRepository
import com.group8.comp2300.domain.repository.medical.MoodDataRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.uuid.Uuid

class MoodDataRepositoryImpl(
    private val authRepository: AuthRepository,
    private val moodLocal: MoodLocalDataSource,
    private val queuedWriteDispatcher: QueuedWriteDispatcher,
) : MoodDataRepository {
    override suspend fun logMood(request: MoodEntryRequest): Mood {
        val mood = Mood(
            id = Uuid.random().toString(),
            userId = authRepository.session.value.userOrNull?.id.orEmpty(),
            timestamp = Clock.System.now().toEpochMilliseconds(),
            moodType = request.moodScore.toMoodType(),
            feeling = request.tags.joinToString(", "),
            journal = request.notes,
        )

        moodLocal.insert(mood)
        queuedWriteDispatcher.replacePending(
            entityType = OutboxEntityType.MOOD,
            localId = mood.id,
            payload = Json.encodeToString(request),
        )
        return moodLocal.getAll().firstOrNull { it.id == mood.id } ?: mood
    }
}

private fun Int.toMoodType(): MoodType = when {
    this >= 5 -> MoodType.GREAT
    this >= 4 -> MoodType.GOOD
    this >= 3 -> MoodType.NEUTRAL
    this >= 2 -> MoodType.SAD
    else -> MoodType.VERY_SAD
}
