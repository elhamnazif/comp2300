package com.group8.comp2300.config

import com.group8.comp2300.data.repository.UserRepositoryImpl
import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.security.PasswordHasher

object DevSeeder {
    fun seedIfDevBypassEnabled(database: ServerDatabase) {
        if (!Environment.devAuthBypass) return
        val repo = UserRepositoryImpl(database)
        if (repo.findById(Environment.DEV_USER_ID) == null) {
            repo.insert(
                id = Environment.DEV_USER_ID,
                email = "dev@example.com",
                passwordHash = PasswordHasher.hash("password123"),
                firstName = "Dev",
                lastName = "User",
                phone = null,
                dateOfBirth = null,
                gender = null,
                sexualOrientation = null,
                preferredLanguage = "en",
            )
            repo.activateUser(Environment.DEV_USER_ID)
        }
    }
}
