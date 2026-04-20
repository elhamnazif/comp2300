package com.group8.comp2300.service.content

import com.group8.comp2300.domain.model.education.Badge
import com.group8.comp2300.domain.repository.BadgeRepository
import com.group8.comp2300.domain.repository.UserBadgeRepository
import com.group8.comp2300.domain.repository.UserQuizRepository
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals

class UserBadgeServiceTest {

    private val badgeRepository = mockk<BadgeRepository>()
    private val userBadgeRepository = mockk<UserBadgeRepository>()
    private val userQuizRepository = mockk<UserQuizRepository>()
    private val service = UserBadgeService(badgeRepository, userBadgeRepository, userQuizRepository)

    @Test
    fun `checkForNewBadges awards seeded speed demon badge`() {
        val badge = Badge(
            id = "badge-speed",
            name = "The_Speed_Demon",
            iconPath = "/badges/speed-demon.png",
            isLocked = false,
        )
        every { userQuizRepository.countPerfectScores("user-1") } returns 1
        every { userQuizRepository.getAverageTimeSpent("user-1") } returns 29_000.0
        every { badgeRepository.getBadgeByName("The_Rookie") } returns null
        every { badgeRepository.getBadgeByName("The_Novice") } returns null
        every { badgeRepository.getBadgeByName("The_Wizard") } returns null
        every { badgeRepository.getBadgeByName("The_Sage") } returns null
        every { badgeRepository.getBadgeByName("The_Speed_Demon") } returns badge
        every { userBadgeRepository.hasUserEarnedBadge("user-1", "badge-speed") } returns false
        every { userBadgeRepository.awardBadgeToUser("user-1", "badge-speed", any()) } returns Unit

        val result = service.checkForNewBadges("user-1")

        assertEquals(listOf("The_Speed_Demon"), result)
        verify(exactly = 1) { userQuizRepository.countPerfectScores("user-1") }
        verify(exactly = 1) { userQuizRepository.getAverageTimeSpent("user-1") }
        verify(exactly = 1) { badgeRepository.getBadgeByName("The_Rookie") }
        verify(exactly = 1) { badgeRepository.getBadgeByName("The_Novice") }
        verify(exactly = 1) { badgeRepository.getBadgeByName("The_Wizard") }
        verify(exactly = 1) { badgeRepository.getBadgeByName("The_Sage") }
        verify(exactly = 1) { badgeRepository.getBadgeByName("The_Speed_Demon") }
        verify(exactly = 1) { userBadgeRepository.hasUserEarnedBadge("user-1", "badge-speed") }
        verify(exactly = 1) { userBadgeRepository.awardBadgeToUser("user-1", "badge-speed", any()) }
        confirmVerified(badgeRepository, userBadgeRepository, userQuizRepository)
    }
}
