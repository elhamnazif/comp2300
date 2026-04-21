package com.group8.comp2300.feature.education.navigation

import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.group8.comp2300.app.navigation.LocalNavigator
import com.group8.comp2300.app.navigation.LocalUseRootOverlayForShellChildren
import com.group8.comp2300.app.navigation.Screen
import com.group8.comp2300.app.navigation.overlayNavigationMetadata
import com.group8.comp2300.feature.education.ArticleDetailScreen
import com.group8.comp2300.feature.education.EducationScreen
import com.group8.comp2300.feature.education.EducationViewModel
import com.group8.comp2300.feature.education.QuizScreen
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

val educationGraphModule = module {
    navigation<Screen.Education>(metadata = ListDetailSceneStrategy.listPane()) {
        val navigator = LocalNavigator.current
        val useRootOverlayForShellChildren = LocalUseRootOverlayForShellChildren.current
        val viewModel = koinViewModel<EducationViewModel>()
        val uiState by viewModel.state.collectAsState()

        EducationScreen(
            categories = uiState.categories,
            articles = uiState.filteredArticles,
            featuredArticle = uiState.featuredArticle,
            selectedCategoryId = uiState.selectedCategoryId,
            searchQuery = uiState.searchQuery,
            stats = uiState.stats,
            earnedBadges = uiState.earnedBadges,
            isLoading = uiState.isLoading,
            isError = uiState.isError,
            onArticleClick = { id ->
                val destination = Screen.ArticleDetail(id)
                if (useRootOverlayForShellChildren) {
                    navigator.navigate(
                        destination,
                    )
                } else {
                    navigator.navigateWithinShell(destination)
                }
            },
            onCategorySelect = viewModel::selectCategory,
            onSearchQueryChange = viewModel::searchArticles,
            onRetry = viewModel::refresh,
        )
    }

    navigation<Screen.ArticleDetail>(
        metadata = overlayNavigationMetadata(ListDetailSceneStrategy.detailPane()),
    ) { route ->
        val navigator = LocalNavigator.current
        val useRootOverlayForShellChildren = LocalUseRootOverlayForShellChildren.current
        val viewModel =
            koinViewModel<com.group8.comp2300.feature.education.ArticleDetailViewModel> {
                parametersOf(route.articleId)
            }

        ArticleDetailScreen(
            viewModel = viewModel,
            articleId = route.articleId,
            onBack = if (useRootOverlayForShellChildren) navigator::goBack else navigator::goBackWithinShell,
            onQuizClick = { quizId -> navigator.navigate(Screen.QuizScreen(quizId)) },
            onRetry = viewModel::refresh,
        )
    }

    navigation<Screen.QuizScreen>(metadata = overlayNavigationMetadata()) { route ->
        val navigator = LocalNavigator.current
        val viewModel =
            koinViewModel<com.group8.comp2300.feature.education.QuizViewModel> {
                parametersOf(route.quizId)
            }
        QuizScreen(
            viewModel = viewModel,
            quizId = route.quizId,
            onBack = navigator::goBack,
        )
    }
}
