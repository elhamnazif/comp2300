package com.group8.comp2300.domain.repository

import com.group8.comp2300.domain.model.education.ContentItem
import com.group8.comp2300.domain.model.education.Quiz

interface EducationRepository {
    /** Return all education content items. */
    fun getAllContent(): List<ContentItem>

    /** Return a content item by its ID, or null if not found. */
    fun getContentById(id: String): ContentItem?

    /** Return a quiz by its ID, or null if not found. */
    fun getQuizById(id: String): Quiz?
}
