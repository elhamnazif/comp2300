package com.group8.comp2300.data.repository.medical

import com.group8.comp2300.data.local.MoodLocalDataSource
import com.group8.comp2300.data.offline.MedicalOfflineMutations
import com.group8.comp2300.data.offline.QueuedOfflineStore
import com.group8.comp2300.data.offline.QueuedWriteDispatcher
import com.group8.comp2300.domain.model.medical.Mood
import com.group8.comp2300.domain.model.medical.MoodEntryRequest
import com.group8.comp2300.domain.model.medical.MoodType
import com.group8.comp2300.domain.model.session.userOrNull
import com.group8.comp2300.domain.repository.AuthRepository
import com.group8.comp2300.domain.repository.medical.MoodDataRepository
import kotlin.time.Clock

class MoodDataRepositoryImpl(
    private val authRepository: AuthRepository,
    private val moodLocal: MoodLocalDataSource,
    private val queuedWriteDispatcher: QueuedWriteDispatcher,
) : MoodDataRepository {
    private val moodWrites = QueuedOfflineStore(
        mutation = MedicalOfflineMutations.mood,
        queuedWriteDispatcher = queuedWriteDispatcher,
        buildLocal = { moodId, request ->
            Mood(
                id = moodId,
                userId = authRepository.session.value.userOrNull?.id.orEmpty(),
                timestamp = Clock.System.now().toEpochMilliseconds(),
                moodType = request.moodScore.toMoodType(),
                feeling = request.tags.joinToString(", "),
                journal = request.notes,
            )
        },
        saveLocal = moodLocal::insert,
        readLocal = { moodId -> moodLocal.getAll().firstOrNull { it.id == moodId } },
    )

    override suspend fun logMood(request: MoodEntryRequest): Mood = moodWrites.write(request)
}

private fun Int.toMoodType(): MoodType = when {
    this >= 5 -> MoodType.GREAT
    this >= 4 -> MoodType.GOOD
    this >= 3 -> MoodType.NEUTRAL
    this >= 2 -> MoodType.SAD
    else -> MoodType.VERY_SAD
}
