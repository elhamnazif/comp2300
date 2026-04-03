package com.group8.comp2300.domain.model.content

import kotlinx.serialization.Serializable

@Serializable
enum class SRHContentType {
    ARTICLE,
    VIDEO,
}

@Serializable
enum class ContentTopic {
    CONTRACEPTION,
    STI_PREVENTION,
    PREGNANCY,
    MENSTRUAL_HEALTH,
    CONSENT,
    RELATIONSHIPS,
    GENERAL_HEALTH,
}

@Serializable
data class SRHContent(
    val id: String,
    val title: String,
    val description: String,
    val contentType: SRHContentType,
    val topics: List<ContentTopic>,
    val keywords: List<String>,
    val contentUrl: String,
    val thumbnailUrl: String? = null,
    val author: String? = null,
    val publishedDate: String,
    val estimatedReadTime: Int? = null,
)

@Serializable
data class SearchRequest(
    val query: String? = null,
    val topics: List<ContentTopic> = emptyList(),
    val keywords: List<String> = emptyList(),
    val contentType: SRHContentType? = null,
)

@Serializable
data class SearchResult(val content: SRHContent, val relevanceScore: Double)
