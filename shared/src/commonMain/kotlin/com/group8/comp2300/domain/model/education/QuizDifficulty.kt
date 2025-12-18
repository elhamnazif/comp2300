package com.group8.comp2300.domain.model.education

import kotlinx.serialization.Serializable

@Serializable
enum class QuizDifficulty(val displayName: String) {
        EASY("Easy"),
        MEDIUM("Medium"),
        HARD("Hard")
}