package com.group8.comp2300.feature.education

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.group8.comp2300.core.format.DateFormatter
import com.group8.comp2300.core.ui.components.ScreenHeader
import com.group8.comp2300.domain.model.education.ArticleSummary
import com.group8.comp2300.domain.model.education.Category
import com.group8.comp2300.domain.model.education.EarnedBadge
import com.group8.comp2300.domain.model.education.UserQuizStats
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.ArrowForwardW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.EmojiEventsW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.SearchW400Outlinedfill1
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
fun EducationScreen(
    categories: List<Category>,
    articles: List<ArticleSummary>,
    featuredArticle: ArticleSummary?,
    selectedCategoryId: String?,
    searchQuery: String,
    stats: UserQuizStats,
    earnedBadges: List<EarnedBadge>,
    isLoading: Boolean,
    isError: Boolean,
    onArticleClick: (String) -> Unit,
    onCategorySelect: (String?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        ScreenHeader(
            horizontalPadding = 16.dp,
            topPadding = 8.dp,
        ) {
            SearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        when {
            isLoading -> {
                ScreenStatePanel(
                    title = stringResource(Res.string.education_library_loading),
                    actionLabel = null,
                    onAction = null,
                ) {
                    CircularProgressIndicator()
                }
            }

            isError -> {
                ScreenStatePanel(
                    title = stringResource(Res.string.education_library_loading_error),
                    actionLabel = stringResource(Res.string.education_library_retry),
                    onAction = onRetry,
                )
            }

            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        CompactProgressHeader(
                            stats = stats,
                            earnedBadges = earnedBadges,
                        )
                    }

                    item {
                        CategoryRow(
                            categories = categories,
                            selectedCategoryId = selectedCategoryId,
                            onCategorySelect = onCategorySelect,
                        )
                    }

                    if (featuredArticle != null && selectedCategoryId == null && searchQuery.isBlank()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = stringResource(Res.string.education_featured_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                FeaturedArticleCard(
                                    article = featuredArticle,
                                    onClick = { onArticleClick(featuredArticle.id) },
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = if (searchQuery.isNotBlank()) {
                                stringResource(Res.string.education_search_results)
                            } else {
                                stringResource(Res.string.education_latest_updates)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    if (articles.isEmpty()) {
                        item {
                            EmptyStateCard(text = stringResource(Res.string.education_library_empty))
                        }
                    } else {
                        items(
                            items = articles,
                            key = { it.id },
                        ) { article ->
                            StandardArticleCard(
                                article = article,
                                onClick = { onArticleClick(article.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactProgressHeader(
    stats: UserQuizStats,
    earnedBadges: List<EarnedBadge>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProgressSummaryMetric(
                    value = stats.totalPerfectScores.toString(),
                    label = stringResource(Res.string.education_progress_perfect_scores),
                    modifier = Modifier.weight(1f),
                )
                ProgressSummaryMetric(
                    value = "${stats.averageTimeSpentSeconds.roundToInt()}s",
                    label = stringResource(Res.string.education_progress_average_time),
                    modifier = Modifier.weight(1f),
                )
                ProgressSummaryMetric(
                    value = earnedBadges.size.toString(),
                    label = stringResource(Res.string.education_progress_badges),
                    modifier = Modifier.weight(1f),
                )
            }

            if (earnedBadges.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(Res.string.education_badges_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(earnedBadges.take(3)) { badge ->
                            AssistChip(
                                onClick = {},
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.EmojiEventsW400Outlinedfill1,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                },
                                label = {
                                    Text(
                                        text = badge.badge.name.replace('_', ' '),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressSummaryMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CategoryRow(categories: List<Category>, selectedCategoryId: String?, onCategorySelect: (String?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(
                selected = selectedCategoryId == null,
                onClick = { onCategorySelect(null) },
                label = { Text(stringResource(Res.string.education_category_all)) },
            )
        }
        items(categories) { category ->
            FilterChip(
                selected = selectedCategoryId == category.id,
                onClick = {
                    onCategorySelect(
                        if (selectedCategoryId == category.id) {
                            null
                        } else {
                            category.id
                        },
                    )
                },
                label = { Text(category.title) },
            )
        }
    }
}

@Composable
private fun ScreenStatePanel(
    title: String,
    actionLabel: String?,
    onAction: (() -> Unit)?,
    modifier: Modifier = Modifier,
    leading: @Composable (() -> Unit)? = null,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            leading?.invoke()
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (actionLabel != null && onAction != null) {
                Button(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun EmptyStateCard(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(20.dp),
        )
    }
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.height(56.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            Icon(
                imageVector = Icons.SearchW400Outlinedfill1,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(12.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                text = stringResource(Res.string.education_search_placeholder),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }
    }
}

@Composable
private fun FeaturedArticleCard(article: ArticleSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ArticleCover(
                article = article,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp),
            )

            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = article.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                ArticleMetaRow(article = article)
            }
        }
    }
}

@Composable
private fun StandardArticleCard(article: ArticleSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = article.categories.firstOrNull()?.title.orEmpty(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Icon(
                        imageVector = Icons.ArrowForwardW400Outlinedfill1,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }

                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = article.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                ArticleMetaRow(article = article)
            }

            if (!article.thumbnailUrl.isNullOrBlank()) {
                EducationRemoteImage(
                    imageUrl = article.thumbnailUrl,
                    modifier = Modifier
                        .width(108.dp)
                        .height(96.dp),
                    shape = MaterialTheme.shapes.large,
                    contentScale = ContentScale.Crop,
                    overlay = articleImageGradient(alpha = 0.18f),
                )
            }
        }
    }
}

@Composable
private fun ArticleCover(article: ArticleSummary, modifier: Modifier = Modifier) {
    EducationRemoteImage(
        imageUrl = article.thumbnailUrl,
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        contentScale = ContentScale.Crop,
        overlay = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.scrim.copy(alpha = 0.1f),
                                MaterialTheme.colorScheme.scrim.copy(alpha = 0.42f),
                            ),
                        ),
                    ),
            )

            Text(
                text = article.categories.firstOrNull()?.title.orEmpty(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                ArticleMetaRow(
                    article = article,
                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
                )
            }
        },
    )
}

@Composable
private fun articleImageGradient(alpha: Float): @Composable BoxScope.() -> Unit = {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        MaterialTheme.colorScheme.scrim.copy(alpha = alpha),
                    ),
                ),
            ),
    )
}

@Composable
private fun ArticleMetaRow(article: ArticleSummary, tint: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    val meta = buildList {
        article.publisher?.takeIf { it.isNotBlank() }?.let(::add)
        article.publishedDate?.let { add(DateFormatter.formatMonthDayYear(it)) }
    }.joinToString(" • ")

    if (meta.isNotBlank()) {
        Text(
            text = meta,
            style = MaterialTheme.typography.labelMedium,
            color = tint,
        )
    }
}
