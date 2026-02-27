package com.group8.comp2300.domain.model.education

import kotlinx.serialization.Serializable

@Serializable
data class ContentItem(
    val id: String,
    val title: String,
    val description: String,
    val category: ContentCategory,
    val type: ContentType,
    val durationMinutes: Int,
    val isFeatured: Boolean = false,
    val videoUrl: String? = null,
    val transcript: String = "",
    val tags: List<String> = emptyList(),
    val relatedAction: String? = null,
    val thumbnailUrl: String? = null,
    val publishedAt: Long = 0L,
) {
    /** Formatted duration for display */
    val formattedDuration: String
        get() =
            if (durationMinutes < 60) {
                "${durationMinutes}min"
            } else {
                "${durationMinutes / 60}h ${durationMinutes % 60}min"
            }
}
