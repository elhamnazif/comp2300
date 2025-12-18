package com.group8.comp2300.domain.model.education

import kotlinx.serialization.Serializable

@Serializable
enum class ContentType {
    VIDEO,
    ARTICLE,
    QUIZ
}