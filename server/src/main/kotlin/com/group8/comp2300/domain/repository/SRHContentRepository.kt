package com.group8.comp2300.domain.repository

import com.group8.comp2300.domain.model.content.SRHContent
import com.group8.comp2300.domain.model.content.ContentTopic

interface SRHContentRepository {
    fun search(query: String?): List<SRHContent>
    fun findByTopics(topics: List<ContentTopic>): List<SRHContent>
    fun findByKeywords(keywords: List<String>): List<SRHContent>
    fun getById(id: String): SRHContent?
    fun getAll(): List<SRHContent>
}
