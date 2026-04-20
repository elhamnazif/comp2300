package com.group8.comp2300.domain.repository

import com.group8.comp2300.domain.model.education.Badge
import com.group8.comp2300.domain.model.education.EarnedBadge

interface UserBadgeRepository {
    fun awardBadgeToUser(userId: String, badgeId: String, earnedAt: Long)
    fun hasUserEarnedBadge(userId: String, badgeId: String): Boolean
    fun getEarnedBadges(userId: String): List<EarnedBadge>
    fun getLockedBadges(userId: String): List<Badge>
    fun getEarnedBadgeCount(userId: String): Long

    // --- Cleanup & Revocation ---
    fun revokeBadgeFromUser(userId: String, badgeId: String)
    fun clearAllBadgesForUser(userId: String)
}
