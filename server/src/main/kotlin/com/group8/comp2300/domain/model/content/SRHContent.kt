package com.group8.comp2300.domain.model.content

enum class ContentType {
    ARTICLE,
    VIDEO
}

enum class ContentTopic {
    CONTRACEPTION,
    STI_PREVENTION,
    PREGNANCY,
    MENSTRUAL_HEALTH,
    CONSENT,
    RELATIONSHIPS,
    GENERAL_HEALTH
}

data class SRHContent(
    val id: String,
    val title: String,
    val description: String,
    val contentType: ContentType,
    val topics: List<ContentTopic>,
    val keywords: List<String>,
    val contentUrl: String,           // URL to full article or video
    val thumbnailUrl: String?,
    val author: String?,
    val publishedDate: String,
    val estimatedReadTime: Int?       // Minutes, null for videos
)

data class SearchRequest(
    val query: String? = null,
    val topics: List<ContentTopic> = emptyList(),
    val keywords: List<String> = emptyList(),
    val contentType: ContentType? = null
)

data class SearchResult(
    val content: SRHContent,
    val relevanceScore: Double
)
