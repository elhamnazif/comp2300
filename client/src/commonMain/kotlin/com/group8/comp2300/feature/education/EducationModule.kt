package com.group8.comp2300.feature.education

import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val educationModule = module {
    viewModelOf(::EducationViewModel)
    viewModel { params ->
        ArticleDetailViewModel(
            repository = get(),
            articleId = params.get(),
        )
    }
    viewModel { params ->
        QuizViewModel(
            repository = get(),
            refreshNotifier = get(),
            quizId = params.get(),
        )
    }
    single { EducationRefreshNotifier() }
}
