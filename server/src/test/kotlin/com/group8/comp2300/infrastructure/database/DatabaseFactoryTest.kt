package com.group8.comp2300.infrastructure.database

import com.group8.comp2300.data.repository.BadgeRepositoryImpl
import com.group8.comp2300.domain.model.education.Badge
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class DatabaseFactoryTest {

    @Test
    fun `badge seed refreshes existing icon paths`() {
        val dbFile = File.createTempFile("comp2300-badges", ".db")
        dbFile.deleteOnExit()
        val dbUrl = "jdbc:sqlite:${dbFile.absolutePath}"

        createServerDatabase(dbUrl)

        val repository = BadgeRepositoryImpl(createServerDatabase(dbUrl))
        repository.updateBadge(
            Badge(
                id = "b1",
                name = "The_Rookie",
                iconPath = "stale-path",
            ),
        )

        val refreshedRepository = BadgeRepositoryImpl(createServerDatabase(dbUrl))
        val refreshed = refreshedRepository.getBadgeById("b1")

        assertEquals("/images/badges/badge_rookie.png", refreshed?.iconPath)
    }
}
