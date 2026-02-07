package com.group8.comp2300.data.repository

import com.group8.comp2300.domain.model.education.ContentItem
import com.group8.comp2300.domain.model.education.Quiz
import com.group8.comp2300.domain.repository.EducationRepository
import com.group8.comp2300.mock.allQuizzes
import com.group8.comp2300.mock.educationContent

class EducationRepositoryImpl : EducationRepository {
    override fun getAllContent(): List<ContentItem> = educationContent

    override fun getContentById(id: String): ContentItem? = educationContent.find { it.id == id }

    override fun getQuizById(id: String): Quiz? = allQuizzes.find { it.id == id }
}
