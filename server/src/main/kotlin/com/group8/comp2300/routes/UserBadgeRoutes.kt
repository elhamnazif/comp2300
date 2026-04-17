package com.group8.comp2300.routes

import com.group8.comp2300.dto.toDto
import com.group8.comp2300.service.content.UserBadgeService
import org.koin.ktor.ext.inject
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.badgeRoutes() {
    val service by inject<UserBadgeService>()

    route("/api/badges") {
        get("/earned") {
            withUserId { userId ->
                val earned = service.getFullAchievementProfile(userId)
                call.respond(HttpStatusCode.OK, earned.map { it.toDto() })
            }
        }

        get("/locked") {
            withUserId { userId ->
                val locked = service.getLockedAchievements(userId)
                call.respond(HttpStatusCode.OK, locked.map { it.toDto() })
            }
        }

        post("/check") {
            withUserId { userId ->
                val newlyAwarded = service.checkForNewBadges(userId)
                call.respond(HttpStatusCode.OK, newlyAwarded)
            }
        }
    }
}
