package com.group8.comp2300.presentation.screens.education

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

    val selectedCategory: StateFlow<ContentTopic?>
        field = MutableStateFlow<ContentTopic?>(null)

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
            if (currentState.isLoading || currentState.isError) {
                currentState
            } else {
                val filtered = if (category == null) {
                    currentState.allContent
                } else {
                    currentState.allContent.filter { it.category == category }
                }
                currentState.copy(
                    selectedCategory = category,
                    filteredContent = filtered,
                )
            }
        }
        .combine(searchQuery) { currentState, query ->
            if (currentState.isLoading || currentState.isError) {
                currentState
            } else if (query.isBlank()) {
                currentState
            } else {
                val searchResults = repository.searchContent(query)
                val filtered = if (currentState.selectedCategory == null) {
                    searchResults
                } else {
                    searchResults.filter { it.category == currentState.selectedCategory }
                }
                currentState.copy(
                    filteredContent = filtered,
                    searchQuery = query,
                )
            }
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
