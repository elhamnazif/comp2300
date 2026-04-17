package com.group8.comp2300.feature.education.navigation

import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.group8.comp2300.app.navigation.LocalNavigator
import com.group8.comp2300.app.navigation.Screen
import com.group8.comp2300.domain.model.education.ContentType
import com.group8.comp2300.feature.education.EducationScreen
import com.group8.comp2300.feature.education.EducationViewModel
import com.group8.comp2300.feature.education.QuizScreen
import com.group8.comp2300.feature.education.VideoDetailScreen
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

val educationGraphModule = module {
    navigation<Screen.Education>(metadata = ListDetailSceneStrategy.listPane()) {
        val navigator = LocalNavigator.current
        val viewModel = koinViewModel<EducationViewModel>()
        val uiState by viewModel.state.collectAsState()

        EducationScreen(
            filteredContent = uiState.filteredContent,
            featuredItem = uiState.featuredItem,
            selectedCategory = uiState.selectedCategory,
            searchQuery = uiState.searchQuery,
            onContentClick = { id ->
                val content = viewModel.getContentById(id)
                if (content?.type == ContentType.QUIZ) {
                    navigator.navigate(Screen.QuizScreen(id))
                } else {
                    navigator.navigate(Screen.VideoDetail(id))
                }
            },
            onCategorySelect = viewModel::selectCategory,
            onSearchQueryChange = viewModel::searchContent,
        )
    }

    navigation<Screen.VideoDetail>(metadata = ListDetailSceneStrategy.detailPane()) { route ->
        val navigator = LocalNavigator.current
        val viewModel =
            koinViewModel<EducationViewModel> {
                parametersOf(route.videoId)
            }

        VideoDetailScreen(
            viewModel = viewModel,
            videoId = route.videoId,
            onBack = navigator::goBack,
            onActionClick = { action ->
                if (action.contains("Clinic")) {
                    navigator.clearAndGoTo(Screen.Booking)
                }
            },
        )
    }

    navigation<Screen.QuizScreen>(metadata = ListDetailSceneStrategy.detailPane()) { route ->
        val navigator = LocalNavigator.current
        val viewModel =
            koinViewModel<EducationViewModel> {
                parametersOf(route.quizId)
            }
        QuizScreen(
            viewModel = viewModel,
            quizId = route.quizId,
            onBack = navigator::goBack,
        )
    }
}
