package com.group8.comp2300.di

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.group8.comp2300.domain.model.education.ContentType
import com.group8.comp2300.presentation.navigation.LocalNavigator
import com.group8.comp2300.presentation.navigation.Screen
import com.group8.comp2300.presentation.screens.education.EducationScreen
import com.group8.comp2300.presentation.screens.education.EducationViewModel
import com.group8.comp2300.presentation.screens.education.QuizScreen
import com.group8.comp2300.presentation.screens.education.VideoDetailScreen
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class, ExperimentalMaterial3AdaptiveApi::class)
val educationNavigationModule = module {
    navigation<Screen.Education>(metadata = ListDetailSceneStrategy.listPane()) {
        val navigator = LocalNavigator.current
        val viewModel = koinViewModel<com.group8.comp2300.presentation.screens.education.EducationViewModel>()
        val uiState by viewModel.state.collectAsState()

        _root_ide_package_.com.group8.comp2300.presentation.screens.education.EducationScreen(
            filteredContent = uiState.filteredContent,
            featuredItem = uiState.featuredItem,
            selectedCategory = uiState.selectedCategory,
            onContentClick = { id ->
                val content = viewModel.getContentById(id)
                if (content?.type == ContentType.QUIZ) {
                    navigator.navigate(Screen.QuizScreen(id))
                } else {
                    navigator.navigate(Screen.VideoDetail(id))
                }
            },
            onCategorySelect = viewModel::selectCategory
        )
    }

    navigation<Screen.VideoDetail>(metadata = ListDetailSceneStrategy.detailPane()) { route ->
        val navigator = LocalNavigator.current
        val viewModel =
            koinViewModel<com.group8.comp2300.presentation.screens.education.EducationViewModel> {
                parametersOf(route.videoId)
            }

        _root_ide_package_.com.group8.comp2300.presentation.screens.education.VideoDetailScreen(
            viewModel = viewModel,
            videoId = route.videoId,
            onBack = navigator::goBack,
            onActionClick = { action ->
                if (action.contains("Clinic")) {
                    navigator.clearAndGoTo(Screen.Booking)
                }
            }
        )
    }

    navigation<Screen.QuizScreen>(metadata = ListDetailSceneStrategy.detailPane()) { route ->
        val navigator = LocalNavigator.current
        val viewModel =
            koinViewModel<com.group8.comp2300.presentation.screens.education.EducationViewModel> {
                parametersOf(route.quizId)
            }
        _root_ide_package_.com.group8.comp2300.presentation.screens.education.QuizScreen(
            viewModel = viewModel,
            quizId = route.quizId,
            onBack = navigator::goBack
        )
    }
}
