package com.group8.comp2300.data.repository

import com.group8.comp2300.domain.model.education.ContentType
import kotlin.test.Test
import kotlin.test.assertTrue

class EducationRepositoryImplTest {
    private val repository = EducationRepositoryImpl(SRHContentRepositoryImpl())

    @Test
    fun searchContentReturnsSrhArticles() {
        val results = repository.searchContent("contraception")

        assertTrue(results.any { it.type == ContentType.ARTICLE })
    }

    @Test
    fun searchContentReturnsSrhVideos() {
        val results = repository.searchContent("STI")

        assertTrue(results.any { it.type == ContentType.VIDEO })
    }
}
