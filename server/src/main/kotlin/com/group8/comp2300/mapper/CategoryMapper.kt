package com.group8.comp2300.mapper

import com.group8.comp2300.domain.model.education.Category
import com.group8.comp2300.domain.repository.ContentCategoryRepository
import com.group8.comp2300.dto.ContentCategoryResponse

class CategoryMapper(private val categoryRepository: ContentCategoryRepository) {

    fun toResponse(category: Category): ContentCategoryResponse = ContentCategoryResponse(
        id = category.id,
        title = category.title,
        articleCount = categoryRepository.getArticleCountForCategory(category.id),
    )
}
