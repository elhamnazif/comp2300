package com.group8.comp2300.mock

import com.group8.comp2300.domain.model.content.ContentTopic
import com.group8.comp2300.domain.model.education.ContentItem
import com.group8.comp2300.domain.model.education.ContentType

val educationContent =
    listOf(
        ContentItem(
            id = "conception-basics",
            title = "Conception Basics Quiz",
            description =
            "Test your knowledge about fertility, conception, and contraception",
            category = ContentTopic.CONTRACEPTION,
            type = ContentType.QUIZ,
            durationMinutes = 5,
            tags = listOf("Conception", "Fertility", "Contraception", "Education"),
            relatedAction = "Start Quiz",
        ),
    )
