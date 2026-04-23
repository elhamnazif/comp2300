package com.group8.comp2300.domain.model.education

import com.group8.comp2300.domain.model.content.ContentTopic
import kotlinx.serialization.Serializable

@Serializable
data class ContentItem(
    val id: String,
    val title: String,
    val description: String,
    val category: ContentTopic,
    val type: ContentType,
    val durationMinutes: Int,
    val isFeatured: Boolean = false,
    val videoUrl: String? = null,
    val transcript: String = "",
    val tags: List<String> = emptyList(),
    val relatedAction: String? = null,
    val thumbnailUrl: String? = null,
    val publishedAt: Long = 0L,
)
