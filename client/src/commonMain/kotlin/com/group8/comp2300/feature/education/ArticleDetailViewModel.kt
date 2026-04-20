package com.group8.comp2300.feature.education

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group8.comp2300.domain.model.education.ArticleDetail
import com.group8.comp2300.domain.repository.EducationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ArticleDetailViewModel(private val repository: EducationRepository, private val articleId: String) :
    ViewModel() {
    private val mutableState = MutableStateFlow(State(isLoading = true))
    val state: StateFlow<State> = mutableState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            mutableState.value = State(isLoading = true)
            val article = repository.getArticleDetail(articleId)
            mutableState.value = if (article != null) {
                State(article = article)
            } else {
                State(isError = true)
            }
        }
    }

    @Immutable
    data class State(val isLoading: Boolean = false, val isError: Boolean = false, val article: ArticleDetail? = null)
}
