package com.group8.comp2300.feature.education

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.group8.comp2300.core.format.DateFormatter
import com.group8.comp2300.domain.model.content.ContentTopic
import com.group8.comp2300.domain.model.education.ArticleSummary
import com.group8.comp2300.domain.model.education.Category
import com.group8.comp2300.domain.model.education.EarnedBadge
import com.group8.comp2300.domain.model.education.UserQuizStats
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.*
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
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
    val selectedCategory = categories.firstOrNull { it.id == selectedCategoryId }
    val featuredVisible = featuredArticle != null && selectedCategoryId == null && searchQuery.isBlank()
    val progressVisible =
        stats.totalPerfectScores > 0 || stats.averageTimeSpentSeconds > 0.0 || earnedBadges.isNotEmpty()
    val focusManager = LocalFocusManager.current
    val pullToRefreshState = rememberPullToRefreshState()
    val scaleFraction = {
        if (isLoading) {
            1f
        } else {
            LinearOutSlowInEasing.transform(pullToRefreshState.distanceFraction).coerceIn(0f, 1f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pullToRefresh(
                state = pullToRefreshState,
                isRefreshing = isLoading,
                onRefresh = onRetry,
            ),
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(paddingValues),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    EducationTitleRow()
                }

                stickyHeader {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            SearchBar(
                                query = searchQuery,
                                onQueryChange = onSearchQueryChange,
                                onSearchImeAction = { focusManager.clearFocus() },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            CategoryRow(
                                categories = categories,
                                selectedCategoryId = selectedCategoryId,
                                onCategorySelect = onCategorySelect,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                when {
                    isLoading -> {
                        item {
                            LoadingPanel(
                                title = stringResource(Res.string.education_library_loading),
                            )
                        }
                    }

                    isError -> {
                        item {
                            EmptyStatePanel(title = stringResource(Res.string.education_library_loading_error))
                        }
                    }

                    else -> {
                        if (featuredVisible) {
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

                        if (progressVisible) {
                            item {
                                CompactProgressHeader(
                                    stats = stats,
                                    earnedBadges = earnedBadges,
                                )
                            }
                        }

                        item {
                            SectionHeader(
                                title = when {
                                    searchQuery.isNotBlank() -> stringResource(Res.string.education_search_results)
                                    selectedCategory != null -> selectedCategory.title
                                    else -> stringResource(Res.string.education_latest_updates)
                                },
                                supportingText = formatArticleCount(articles.size),
                            )
                        }

                        if (articles.isEmpty()) {
                            item {
                                EmptyStatePanel(title = stringResource(Res.string.education_library_empty))
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

        Box(
            Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .graphicsLayer {
                    scaleX = scaleFraction()
                    scaleY = scaleFraction()
                },
        ) {
            PullToRefreshDefaults.LoadingIndicator(
                state = pullToRefreshState,
                isRefreshing = isLoading,
            )
        }
    }
}

@Composable
private fun EducationTitleRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Education",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun CompactProgressHeader(
    stats: UserQuizStats,
    earnedBadges: List<EarnedBadge>,
    modifier: Modifier = Modifier,
) {
    val metricItems = buildList {
        if (stats.totalPerfectScores > 0) {
            add(
                ProgressMetric(
                    value = stats.totalPerfectScores.toString(),
                    label = stringResource(Res.string.education_progress_perfect_scores),
                    icon = Icons.CheckCircleW400Outlinedfill1,
                    tint = MaterialTheme.colorScheme.primary,
                ),
            )
        }
        if (stats.averageTimeSpentSeconds > 0.0) {
            add(
                ProgressMetric(
                    value = "${stats.averageTimeSpentSeconds.roundToInt()}s",
                    label = stringResource(Res.string.education_progress_average_time),
                    icon = Icons.InfoW400Outlinedfill1,
                    tint = MaterialTheme.colorScheme.tertiary,
                ),
            )
        }
        if (earnedBadges.isNotEmpty()) {
            add(
                ProgressMetric(
                    value = earnedBadges.size.toString(),
                    label = stringResource(Res.string.education_progress_badges),
                    icon = Icons.EmojiEventsW400Outlinedfill1,
                    tint = MaterialTheme.colorScheme.secondary,
                ),
            )
        }
    }

    if (metricItems.isEmpty()) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(Res.string.education_progress_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                metricItems.forEach { item ->
                    ProgressSummaryMetric(item)
                }
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
private fun ProgressSummaryMetric(metric: ProgressMetric, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = metric.tint.copy(alpha = 0.14f),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = metric.icon,
                contentDescription = null,
                tint = metric.tint,
                modifier = Modifier.size(18.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = metric.value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = metric.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CategoryRow(
    categories: List<Category>,
    selectedCategoryId: String?,
    onCategorySelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = selectedCategoryId == null,
                onClick = { onCategorySelect(null) },
                label = { Text(stringResource(Res.string.education_category_all)) },
            )
        }
        items(categories) { category ->
            val accent = category.topicAccent()
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
                leadingIcon = accent?.let {
                    {
                        Icon(
                            imageVector = it.icon,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                        )
                    }
                },
                colors = accent?.let {
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = it.color.copy(alpha = 0.16f),
                        selectedLabelColor = it.color,
                        selectedLeadingIconColor = it.color,
                        iconColor = it.color.copy(alpha = 0.92f),
                    )
                } ?: FilterChipDefaults.filterChipColors(),
                label = { Text(category.title) },
            )
        }
    }
}

@Composable
private fun EmptyStatePanel(title: String, modifier: Modifier = Modifier, body: String? = null) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 320.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            body?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun LoadingPanel(title: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 320.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator()
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchImeAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = stringResource(Res.string.education_search_placeholder),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.SearchW400Outlinedfill1,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.CloseW400Outlinedfill1,
                                contentDescription = stringResource(Res.string.education_search_clear_desc),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearchImeAction() }),
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
                ArticleCategoryLabel(article = article)
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = article.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
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
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    ArticleCategoryLabel(article = article, modifier = Modifier.weight(1f, fill = false))
                    Icon(
                        imageVector = Icons.ArrowForwardW400Outlinedfill1,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }

                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = article.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
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
        overlay = articleImageGradient(alpha = 0.12f),
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

@Composable
private fun SectionHeader(title: String, supportingText: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = supportingText,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ArticleCategoryLabel(article: ArticleSummary, modifier: Modifier = Modifier) {
    val category = article.categories.firstOrNull()
    val accent = category?.topicAccent()

    if (category == null) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        accent?.let {
            Icon(
                imageVector = it.icon,
                contentDescription = null,
                tint = it.color,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text = category.title,
            style = MaterialTheme.typography.labelLarge,
            color = accent?.color ?: MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun formatArticleCount(count: Int): String = when (count) {
    1 -> stringResource(Res.string.education_results_count_single, count)
    else -> stringResource(Res.string.education_results_count_multiple, count)
}

private data class ProgressMetric(val value: String, val label: String, val icon: ImageVector, val tint: Color)

private data class TopicAccent(val icon: ImageVector, val color: Color)

private fun Category.topicAccent(): TopicAccent? {
    val topic = when (id) {
        "id-contraception" -> ContentTopic.CONTRACEPTION

        "id-sti-prevention" -> ContentTopic.STI_PREVENTION

        "id-menstrual-health" -> ContentTopic.MENSTRUAL_HEALTH

        "id-consent" -> ContentTopic.CONSENT

        "id-relationships" -> ContentTopic.RELATIONSHIPS

        "id-sexual-health" -> ContentTopic.GENERAL_HEALTH

        "id-puberty" -> ContentTopic.GENERAL_HEALTH

        else ->
            when (title.lowercase()) {
                "contraception" -> ContentTopic.CONTRACEPTION
                "sti prevention", "stis & safety" -> ContentTopic.STI_PREVENTION
                "menstrual health" -> ContentTopic.MENSTRUAL_HEALTH
                "consent" -> ContentTopic.CONSENT
                "relationships" -> ContentTopic.RELATIONSHIPS
                "sexual health", "general health", "puberty" -> ContentTopic.GENERAL_HEALTH
                else -> null
            }
    } ?: return null

    return TopicAccent(
        icon = topic.icon,
        color = topic.color,
    )
}
