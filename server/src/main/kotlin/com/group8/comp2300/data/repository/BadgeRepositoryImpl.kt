package com.group8.comp2300.data.repository

import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.domain.model.education.Badge
import com.group8.comp2300.domain.repository.BadgeRepository

class BadgeRepositoryImpl(
    private val database: ServerDatabase
) : BadgeRepository {
    private val badgeQueries = database.badgeQueries

    override fun saveBadge(badge: Badge) {
        badgeQueries.insertBadge(
            id = badge.id,
            name = badge.name,
            path = badge.iconPath
        )
    }

    override fun updateBadge(badge: Badge) {
        badgeQueries.updateBadge(
            id = badge.id,
            name = badge.name,
            path = badge.iconPath
        )
    }

    override fun deleteBadge(badgeId: String) {
        badgeQueries.deleteBadge(badgeId)
    }

    override fun getAllBadges(): List<Badge> {
        return badgeQueries.getAllBadges().executeAsList().map { row ->
            Badge(row.id, row.badge_name, row.badge_path)
        }
    }

    override fun getBadgeById(badgeId: String): Badge? {
        return badgeQueries.getBadgeById(badgeId).executeAsOneOrNull()?.let { row ->
            Badge(row.id, row.badge_name, row.badge_path)
        }
    }

    override fun getBadgeByName(name: String): Badge? {
        return badgeQueries.getBadgeByName(name).executeAsOneOrNull()?.let { row ->
            Badge(row.id, row.badge_name, row.badge_path)
        }
    }
}
