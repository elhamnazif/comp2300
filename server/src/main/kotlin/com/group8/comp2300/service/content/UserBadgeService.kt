package com.group8.comp2300.service.content

import com.group8.comp2300.domain.model.education.Badge
import com.group8.comp2300.domain.model.education.EarnedBadge
import com.group8.comp2300.domain.repository.BadgeRepository
import com.group8.comp2300.domain.repository.UserBadgeRepository
import com.group8.comp2300.domain.repository.UserQuizRepository
import java.time.Instant

class UserBadgeService(
    private val badgeRepo: BadgeRepository,
    private val userBadgeRepo: UserBadgeRepository,
    private val quizRepo: UserQuizRepository,
) {

    /**
     * Call at the end of every successful quiz submission.
     */
    fun checkForNewBadges(userId: String): List<String> {
        val newlyAwarded = mutableListOf<String>()
        val now = Instant.now().toEpochMilli()
        val perfectCount = quizRepo.countPerfectScores(userId)
        val avgTimeMs = quizRepo.getAverageTimeSpent(userId) ?: Double.MAX_VALUE
        val avgSeconds = avgTimeMs / 1000.0

        val rules = listOf(
            BadgeRule("The_Rookie", condition = { perfectCount >= 1 }),
            BadgeRule("The_Novice", condition = { perfectCount >= 2 }),
            BadgeRule("The_Wizard", condition = { perfectCount >= 3 }),
            BadgeRule("The_Sage", condition = { perfectCount >= 5 }),
            BadgeRule("The_Speed_Demon", condition = { avgSeconds > 0 && avgSeconds < 30.0 && perfectCount >= 1 }),
        )

        for (rule in rules) {
            val badge = badgeRepo.getBadgeByName(rule.name) ?: continue
            val alreadyHasIt = userBadgeRepo.hasUserEarnedBadge(userId, badge.id)
            if (!alreadyHasIt && rule.condition()) {
                userBadgeRepo.awardBadgeToUser(userId, badge.id, now)
                newlyAwarded.add(badge.name)
            }
        }
        return newlyAwarded
    }

    fun getFullAchievementProfile(userId: String): List<EarnedBadge> = userBadgeRepo.getEarnedBadges(userId)
    fun getLockedAchievements(userId: String): List<Badge> = userBadgeRepo.getLockedBadges(userId)

    private data class BadgeRule(val name: String, val condition: () -> Boolean)
}
