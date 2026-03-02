package com.group8.comp2300.routes

import com.group8.comp2300.domain.model.medical.Mood
import com.group8.comp2300.domain.model.medical.MoodEntryRequest
import com.group8.comp2300.domain.model.medical.MoodType
import com.group8.comp2300.domain.repository.MoodRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import java.util.*

fun Route.moodRoutes() {
    val moodRepository: MoodRepository by inject()

    route("/api/moods") {
        post {
            withUserId { userId ->
                val request = call.receive<MoodEntryRequest>()

                val moodType = when (request.moodScore) {
                    1 -> MoodType.VERY_SAD
                    2 -> MoodType.SAD
                    3 -> MoodType.NEUTRAL
                    4 -> MoodType.GOOD
                    5 -> MoodType.GREAT
                    else -> MoodType.NEUTRAL
                }

                val mood = Mood(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    timestamp = System.currentTimeMillis(),
                    moodType = moodType,
                    feeling = request.tags.joinToString(","),
                    journal = request.notes,
                )

                moodRepository.insert(mood)
                call.respond(HttpStatusCode.Created, mood)
            }
        }
    }
}
