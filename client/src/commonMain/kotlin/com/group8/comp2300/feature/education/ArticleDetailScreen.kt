package com.group8.comp2300.feature.education

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.group8.comp2300.core.format.DateFormatter
import com.group8.comp2300.core.ui.components.AppTopBar
import com.group8.comp2300.domain.model.education.ArticleDetail
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.model.rememberMarkdownState
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ArticleDetailScreen(
    articleId: String,
    onBack: () -> Unit,
    onQuizClick: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArticleDetailViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val article = state.article

    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = {
                    Text(
                        text = article?.title.orEmpty(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                onBackClick = onBack,
                backContentDescription = stringResource(Res.string.education_article_back_desc),
            )
        },
        bottomBar = {
            val quiz = article?.quiz
            if (quiz != null && !state.isLoading && !state.isError) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceBright,
                    tonalElevation = 6.dp,
                ) {
                    Button(
                        onClick = { onQuizClick(quiz.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        Text(stringResource(Res.string.education_article_quiz_cta))
                    }
                }
            }
        },
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                }
            }

            state.isError || article == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(Res.string.education_article_loading_error),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.padding(top = 16.dp),
                    ) {
                        Text(stringResource(Res.string.education_article_retry))
                    }
                }
            }

            else -> {
                ArticleContent(
                    article = article,
                    contentPadding = innerPadding,
                )
            }
        }
    }
}

@Composable
private fun ArticleContent(article: ArticleDetail, contentPadding: PaddingValues) {
    val meta = buildList {
        article.publisher?.takeIf { it.isNotBlank() }?.let(::add)
        article.publishedDate?.let { add(DateFormatter.formatMonthDayYear(it)) }
    }.joinToString(" • ")
    val categoryText = article.categories.joinToString(" • ") { it.title }
    val markdownState = rememberMarkdownState(article.content, retainState = true)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (!article.thumbnailUrl.isNullOrBlank()) {
            item {
                ArticleHero(article = article)
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (categoryText.isNotBlank()) {
                    Text(
                        text = categoryText,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (meta.isNotBlank()) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = article.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Markdown(
                markdownState = markdownState,
                imageTransformer = Coil3ImageTransformerImpl,
                modifier = Modifier.fillMaxWidth(),
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                },
                error = {
                    Text(
                        text = article.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
            )
        }

        article.quiz?.let { quiz ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = quiz.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(Res.string.education_article_quiz_ready),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArticleHero(article: ArticleDetail, modifier: Modifier = Modifier) {
    EducationRemoteImage(
        imageUrl = article.thumbnailUrl,
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp),
        shape = MaterialTheme.shapes.extraLarge,
        contentScale = ContentScale.Crop,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.scrim.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.scrim.copy(alpha = 0.34f),
                        ),
                    ),
                ),
        )

        Text(
            text = article.categories.firstOrNull()?.title.orEmpty(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
        )
    }
}
