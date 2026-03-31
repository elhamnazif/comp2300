package com.group8.comp2300.data.mapper

import com.group8.comp2300.domain.model.content.ContentTopic
import com.group8.comp2300.domain.model.content.SRHContent
import com.group8.comp2300.domain.model.content.SRHContentType
import com.group8.comp2300.domain.model.education.ContentItem
import com.group8.comp2300.domain.model.education.ContentType

fun SRHContent.toContentItem(): ContentItem = ContentItem(
    id = id,
    title = title,
    description = description,
    category = topics.firstOrNull() ?: ContentTopic.GENERAL_HEALTH,
    type = when (contentType) {
        SRHContentType.ARTICLE -> ContentType.ARTICLE
        SRHContentType.VIDEO -> ContentType.VIDEO
    },
    durationMinutes = estimatedReadTime ?: 5,
    isFeatured = id == "2", // Feature the STI prevention video
    videoUrl = if (contentType == SRHContentType.VIDEO) contentUrl else null,
    transcript = "",
    tags = keywords,
    thumbnailUrl = thumbnailUrl,
    publishedAt = 0L,
)
