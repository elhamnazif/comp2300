package com.group8.comp2300.presentation.ui.screens.education

import androidx.lifecycle.ViewModel
import com.group8.comp2300.domain.model.education.ContentCategory
import com.group8.comp2300.domain.model.education.ContentItem
import com.group8.comp2300.domain.model.education.Quiz
import com.group8.comp2300.domain.repository.EducationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class EducationUiState(
        val allContent: List<ContentItem> = emptyList(),
        val filteredContent: List<ContentItem> = emptyList(),
        val selectedCategory: ContentCategory? = null,
        val featuredItem: ContentItem? = null
)

class EducationViewModel(private val repository: EducationRepository) : ViewModel() {
    private val _uiState: MutableStateFlow<EducationUiState>

    init {
        val allContent = repository.getAllContent()
        val featuredItem = allContent.firstOrNull { it.isFeatured }
        _uiState =
                MutableStateFlow(
                        EducationUiState(
                                allContent = allContent,
                                filteredContent = allContent,
                                featuredItem = featuredItem
                        )
                )
    }

    val uiState: StateFlow<EducationUiState> = _uiState.asStateFlow()

    fun selectCategory(category: ContentCategory?) {
        _uiState.update { state ->
            val filtered =
                    if (category == null) {
                        state.allContent
                    } else {
                        state.allContent.filter { it.category == category }
                    }
            state.copy(selectedCategory = category, filteredContent = filtered)
        }
    }

    fun getContentById(id: String): ContentItem? = repository.getContentById(id)

    fun getQuizById(id: String): Quiz? = repository.getQuizById(id)
}
