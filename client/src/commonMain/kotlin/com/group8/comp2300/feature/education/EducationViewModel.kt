package com.group8.comp2300.feature.education

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group8.comp2300.domain.model.content.ContentTopic
import com.group8.comp2300.domain.model.education.ContentItem
import com.group8.comp2300.domain.model.education.Quiz
import com.group8.comp2300.domain.repository.EducationRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EducationViewModel(private val repository: EducationRepository) : ViewModel() {
    private val selectedCategory = MutableStateFlow<ContentTopic?>(null)

    private val searchQuery: MutableStateFlow<String> = MutableStateFlow("")

    private val refreshTrigger: MutableSharedFlow<Unit> = MutableSharedFlow(replay = 1)

    val state: StateFlow<State> = flow {
        // Initial emit
        emit(fetchData())

        // React to refreshes
        refreshTrigger.collect {
            emit(State(isLoading = true)) // Optimistic loading state
            emit(fetchData())
        }
    }
        .combine(selectedCategory) { currentState, category ->
            currentState.applyFilters(category = category)
        }
        .combine(searchQuery) { currentState, query ->
            currentState.applyFilters(query = query)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = State(isLoading = true),
        )

    private suspend fun fetchData(): State = try {
        val allContent = repository.getAllContent()
        val featuredItem = allContent.firstOrNull { it.isFeatured }
        State(
            isLoading = false,
            allContent = allContent,
            filteredContent = allContent, // Default to showing all
            featuredItem = featuredItem,
        )
    } catch (_: Exception) {
        State(isLoading = false, isError = true)
    }

    // ACTIONS

    fun selectCategory(category: ContentTopic?) {
        // Direct access to the backing field
        selectedCategory.value = category
    }

    fun searchContent(query: String) {
        searchQuery.value = query
    }

    fun refresh() {
        viewModelScope.launch {
            refreshTrigger.emit(Unit)
        }
    }

    fun getContentById(id: String): ContentItem? = repository.getContentById(id)
    fun getQuizById(id: String): Quiz? = repository.getQuizById(id)

    private fun State.applyFilters(category: ContentTopic? = selectedCategory, query: String = searchQuery): State {
        if (isLoading || isError) {
            return copy(
                selectedCategory = category,
                searchQuery = query,
            )
        }

        val baseContent = if (query.isBlank()) allContent else repository.searchContent(query)
        val filteredContent =
            if (category == null) {
                baseContent
            } else {
                baseContent.filter { it.category == category }
            }

        return copy(
            filteredContent = filteredContent,
            selectedCategory = category,
            searchQuery = query,
        )
    }

    @Immutable
    data class State(
        val isLoading: Boolean = false,
        val isError: Boolean = false,
        val allContent: List<ContentItem> = emptyList(),
        val filteredContent: List<ContentItem> = emptyList(),
        val selectedCategory: ContentTopic? = null,
        val featuredItem: ContentItem? = null,
        val searchQuery: String = "",
    )
}
