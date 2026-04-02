package com.group8.comp2300.service.content

import com.group8.comp2300.domain.model.content.*
import com.group8.comp2300.domain.repository.SRHContentRepository

class SRHContentService(private val repository: SRHContentRepository) {

    /**
     * Search and filter content based on user request
     * This is the main method your teammate's UI will call
     */
    fun searchContent(request: SearchRequest): List<SearchResult> {
        // Step 1: Get initial results based on query
        var results = when {
            !request.query.isNullOrBlank() -> {
                repository.search(request.query)
            }

            request.topics.isNotEmpty() -> {
                repository.findByTopics(request.topics)
            }

            request.keywords.isNotEmpty() -> {
                repository.findByKeywords(request.keywords)
            }

            else -> {
                repository.getAll()
            }
        }.toMutableList()

        // Step 2: Apply additional filters
        results = results.filter { content ->
            var matches = true

            // Filter by topics if specified
            if (request.topics.isNotEmpty()) {
                matches = matches && content.topics.intersect(request.topics.toSet()).isNotEmpty()
            }

            // Filter by keywords if specified
            if (request.keywords.isNotEmpty()) {
                matches = matches && content.keywords.intersect(request.keywords.toSet()).isNotEmpty()
            }

            // Filter by content type if specified
            if (request.contentType != null) {
                matches = matches && content.contentType == request.contentType
            }

            matches
        }.toMutableList()

        // Step 3: Calculate relevance and sort
        return results.map { content ->
            SearchResult(
                content = content,
                relevanceScore = calculateRelevance(content, request),
            )
        }.sortedByDescending { it.relevanceScore }
    }

    /**
     * Get full content by ID (for viewing after clicking)
     */
    fun getContentById(id: String): SRHContent? = repository.getById(id)

    /**
     * Get all available topics (for filter UI)
     */
    fun getAllTopics(): List<ContentTopic> = ContentTopic.entries

    /**
     * Get popular keywords (for suggestions)
     */
    fun getPopularKeywords(): List<String> {
        val allKeywords = repository.getAll().flatMap { it.keywords }
        return allKeywords.groupBy { it }
            .mapValues { it.value.size }
            .entries
            .sortedByDescending { it.value }
            .take(10)
            .map { it.key }
    }

    /**
     * Calculate how relevant content is to search request
     */
    private fun calculateRelevance(content: SRHContent, request: SearchRequest): Double {
        var score = 0.0

        // Title matches are most important
        if (!request.query.isNullOrEmpty() &&
            content.title.contains(request.query, ignoreCase = true)
        ) {
            score += 3.0
        }

        // Description matches are second most important
        if (!request.query.isNullOrEmpty() &&
            content.description.contains(request.query, ignoreCase = true)
        ) {
            score += 2.0
        }

        // Keyword matches
        if (!request.query.isNullOrEmpty()) {
            val keywordMatches = content.keywords.count {
                it.contains(request.query, ignoreCase = true)
            }
            score += keywordMatches * 1.0
        }

        // Topic matches
        if (request.topics.isNotEmpty()) {
            val topicMatches = content.topics.intersect(request.topics.toSet()).size
            score += topicMatches * 1.5
        }

        // Keyword filter matches
        if (request.keywords.isNotEmpty()) {
            val keywordFilterMatches = content.keywords.intersect(request.keywords.toSet()).size
            score += keywordFilterMatches * 1.0
        }

        return score
    }
}
