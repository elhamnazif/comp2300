package com.group8.comp2300.domain.repository

import com.group8.comp2300.domain.model.education.Badge

interface BadgeRepository {
    // --- Setup ---
    fun saveBadge(badge: Badge)
    fun updateBadge(badge: Badge)
    fun deleteBadge(badgeId: String)

    // --- General Information ---
    fun getAllBadges(): List<Badge>
    fun getBadgeById(badgeId: String): Badge?
    fun getBadgeByName(name: String): Badge?
}
