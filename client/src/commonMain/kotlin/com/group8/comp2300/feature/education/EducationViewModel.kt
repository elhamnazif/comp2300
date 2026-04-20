package com.group8.comp2300.feature.education

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group8.comp2300.domain.model.education.ArticleSummary
import com.group8.comp2300.domain.model.education.Category
import com.group8.comp2300.domain.model.education.EarnedBadge
import com.group8.comp2300.domain.model.education.UserQuizStats
import com.group8.comp2300.domain.repository.EducationRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EducationViewModel(private val repository: EducationRepository, refreshNotifier: EducationRefreshNotifier) :
    ViewModel() {
    private val baseState = MutableStateFlow(State(isLoading = true))
    private val selectedCategoryId = MutableStateFlow<String?>(null)
    private val searchQuery = MutableStateFlow("")
    private val manualRefresh = MutableStateFlow(0)

    val state: StateFlow<State> = combine(
        baseState,
        selectedCategoryId,
        searchQuery,
    ) { base, categoryId, query ->
        val filteredArticles = base.articles.filter { article ->
            val matchesCategory = categoryId == null || article.categories.any { it.id == categoryId }
            val matchesQuery = query.isBlank() || article.matchesQuery(query)
            matchesCategory && matchesQuery
        }
        base.copy(
            selectedCategoryId = categoryId,
            searchQuery = query,
            filteredArticles = filteredArticles,
            featuredArticle = if (categoryId == null && query.isBlank()) {
                base.articles.firstOrNull()
            } else {
                filteredArticles.firstOrNull()
            },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = State(isLoading = true),
    )

    init {
        viewModelScope.launch {
            merge(
                manualRefresh,
                refreshNotifier.refreshes,
            ).collect {
                load()
            }
        }
    }

    fun selectCategory(categoryId: String?) {
        selectedCategoryId.value = categoryId
    }

    fun searchArticles(query: String) {
        searchQuery.value = query
    }

    fun refresh() {
        manualRefresh.update { it + 1 }
    }

    private suspend fun load() {
        baseState.update { it.copy(isLoading = true, isError = false) }
        runCatching {
            kotlinx.coroutines.coroutineScope {
                val categoriesDeferred = async { repository.getCategories() }
                val articlesDeferred = async { repository.getArticles() }
                val progressDeferred = async { repository.getProgress() }
                Triple(categoriesDeferred.await(), articlesDeferred.await(), progressDeferred.await())
            }
        }.onSuccess { (categories, articles, progress) ->
            baseState.value = State(
                isLoading = false,
                categories = categories,
                articles = articles,
                filteredArticles = articles,
                featuredArticle = articles.firstOrNull(),
                stats = progress.stats,
                earnedBadges = progress.earnedBadges,
            )
        }.onFailure {
            baseState.value = State(isLoading = false, isError = true)
        }
    }

    @Immutable
    data class State(
        val isLoading: Boolean = false,
        val isError: Boolean = false,
        val categories: List<Category> = emptyList(),
        val articles: List<ArticleSummary> = emptyList(),
        val filteredArticles: List<ArticleSummary> = emptyList(),
        val featuredArticle: ArticleSummary? = null,
        val selectedCategoryId: String? = null,
        val searchQuery: String = "",
        val stats: UserQuizStats = UserQuizStats(0, 0.0, emptyList()),
        val earnedBadges: List<EarnedBadge> = emptyList(),
    )
}

private fun ArticleSummary.matchesQuery(query: String): Boolean {
    val normalizedQuery = query.trim()
    return title.contains(normalizedQuery, ignoreCase = true) ||
        description.contains(normalizedQuery, ignoreCase = true) ||
        publisher.orEmpty().contains(normalizedQuery, ignoreCase = true) ||
        categories.any { it.title.contains(normalizedQuery, ignoreCase = true) }
}
