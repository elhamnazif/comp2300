package com.group8.comp2300.data.repository

import com.group8.comp2300.data.mapper.toContentItem
import com.group8.comp2300.domain.model.education.ContentItem
import com.group8.comp2300.domain.model.education.Quiz
import com.group8.comp2300.domain.repository.EducationRepository
import com.group8.comp2300.domain.repository.SRHContentRepository
import com.group8.comp2300.mock.allQuizzes
import com.group8.comp2300.mock.educationContent

class EducationRepositoryImpl(private val srhRepository: SRHContentRepository) : EducationRepository {

    private val mergedContent: List<ContentItem> by lazy {
        srhRepository.getAll().map { it.toContentItem() } + educationContent
    }

    override fun getAllContent(): List<ContentItem> = mergedContent

    override fun getContentById(id: String): ContentItem? = mergedContent.find { it.id == id }

    override fun getQuizById(id: String): Quiz? = allQuizzes.find { it.id == id }

    override fun searchContent(query: String): List<ContentItem> {
        if (query.isBlank()) return mergedContent
        val srhResults = srhRepository.search(query).map { it.toContentItem() }
        val localMatches = educationContent.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true) ||
                it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
        }
        return (srhResults + localMatches).distinctBy { it.id }
    }
}
