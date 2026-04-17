package com.group8.comp2300.data.repository

import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.domain.model.education.Badge
import com.group8.comp2300.domain.model.education.EarnedBadge
import com.group8.comp2300.domain.repository.UserBadgeRepository
import java.util.UUID

class UserBadgeRepositoryImpl(
    private val database: ServerDatabase
) : UserBadgeRepository {

    private val badgeQueries = database.badgeQueries
    private val userBadgeQueries = database.userBadgeQueries

    override fun awardBadgeToUser(userId: String, badgeId: String, earnedAt: Long) {
        userBadgeQueries.insertUserBadge(
            id = UUID.randomUUID().toString(),
            userId = userId,
            badgeId = badgeId,
            earnedAt = earnedAt
        )
    }

    override fun hasUserEarnedBadge(userId: String, badgeId: String): Boolean {
        return userBadgeQueries.hasUserEarnedBadge(userId, badgeId).executeAsOne()
    }

    override fun getEarnedBadges(userId: String): List<EarnedBadge> {
        return badgeQueries.getEarnedBadgesByUserId(userId).executeAsList().map { row ->
            EarnedBadge(
                badge = Badge(row.id, row.badge_name, row.badge_path),
                earnedAt = row.earned_at
            )
        }
    }

    override fun getLockedBadges(userId: String): List<Badge> {
        return badgeQueries.getLockedBadgesByUserId(userId).executeAsList().map { row ->
            Badge(row.id, row.badge_name, row.badge_path)
        }
    }

    override fun getEarnedBadgeCount(userId: String): Long {
        return userBadgeQueries.getBadgeUserBadgeCount(userId).executeAsOne()
    }

    override fun revokeBadgeFromUser(userId: String, badgeId: String) {
        userBadgeQueries.deleteUserBadge(userId, badgeId)
    }

    override fun clearAllBadgesForUser(userId: String) {
        userBadgeQueries.deleteAllBadgesForUser(userId)
    }
}
