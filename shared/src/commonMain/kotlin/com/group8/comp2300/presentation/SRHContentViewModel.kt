package com.group8.comp2300.presentation

import com.group8.comp2300.domain.model.content.*
import com.group8.comp2300.service.content.MockSRHContentRepository
import com.group8.comp2300.service.content.SRHContentService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SRHContentViewModel {

    private val repository = MockSRHContentRepository()
    private val contentService = SRHContentService(repository)

    // UI State
    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    private val _selectedContent = MutableStateFlow<SRHContent?>(null)
    val selectedContent: StateFlow<SRHContent?> = _selectedContent.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTopics = MutableStateFlow<List<ContentTopic>>(emptyList())
    val selectedTopics: StateFlow<List<ContentTopic>> = _selectedTopics.asStateFlow()

    private val _selectedContentType = MutableStateFlow<ContentType?>(null)
    val selectedContentType: StateFlow<ContentType?> = _selectedContentType.asStateFlow()

    val availableTopics: List<ContentTopic> = contentService.getAllTopics()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        performSearch()
    }

    fun toggleTopic(topic: ContentTopic) {
        _selectedTopics.value = if (_selectedTopics.value.contains(topic)) {
            _selectedTopics.value - topic
        } else {
            _selectedTopics.value + topic
        }
        performSearch()
    }

    fun updateContentType(contentType: ContentType?) {
        _selectedContentType.value = contentType
        performSearch()
    }

    fun clearFilters() {
        _selectedTopics.value = emptyList()
        _selectedContentType.value = null
        _searchQuery.value = ""
        performSearch()
    }

    fun selectContent(contentId: String) {
        _selectedContent.value = contentService.getContentById(contentId)
    }

    fun clearSelectedContent() {
        _selectedContent.value = null
    }

    private fun performSearch() {
        _isLoading.value = true

        val request = SearchRequest(
            query = _searchQuery.value.takeIf { it.isNotBlank() },
            topics = _selectedTopics.value,
            contentType = _selectedContentType.value
        )

        _searchResults.value = contentService.searchContent(request)
        _isLoading.value = false
    }
}