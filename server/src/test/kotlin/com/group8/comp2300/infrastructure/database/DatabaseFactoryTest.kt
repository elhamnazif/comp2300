package com.group8.comp2300.infrastructure.database

import com.group8.comp2300.data.repository.BadgeRepositoryImpl
import com.group8.comp2300.data.repository.ProductRepositoryImpl
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

    @Test
    fun `product seed refreshes existing catalog fields`() {
        val dbFile = File.createTempFile("comp2300-products", ".db")
        dbFile.deleteOnExit()
        val dbUrl = "jdbc:sqlite:${dbFile.absolutePath}"

        createServerDatabase(dbUrl)

        val repository = ProductRepositoryImpl(createServerDatabase(dbUrl))
        val staleProduct = repository.getById("1") ?: error("expected seeded product")
        repository.update(staleProduct.copy(price = 20.0, insuranceCovered = true))

        val refreshedRepository = ProductRepositoryImpl(createServerDatabase(dbUrl))
        val refreshed = refreshedRepository.getById("1")

        assertEquals(49.9, refreshed?.price)
        assertEquals(false, refreshed?.insuranceCovered)
    }
}
