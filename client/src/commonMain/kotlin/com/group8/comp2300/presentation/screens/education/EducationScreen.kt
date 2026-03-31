package com.group8.comp2300.presentation.screens.education

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.group8.comp2300.domain.model.content.ContentTopic
import com.group8.comp2300.domain.model.education.ContentItem
import com.group8.comp2300.domain.model.education.ContentType
import com.group8.comp2300.presentation.components.ScreenHeader
import com.group8.comp2300.presentation.screens.auth.components.color
import com.group8.comp2300.presentation.screens.auth.components.icon
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.ArticleW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.PlayCircleW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.QuizW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.SearchW400Outlinedfill1
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource

private val ContentTopic.labelRes
    get() =
        when (this) {
            ContentTopic.CONTRACEPTION -> Res.string.education_topic_contraception
            ContentTopic.STI_PREVENTION -> Res.string.education_topic_sti_prevention
            ContentTopic.PREGNANCY -> Res.string.education_topic_pregnancy
            ContentTopic.MENSTRUAL_HEALTH -> Res.string.education_topic_menstrual_health
            ContentTopic.CONSENT -> Res.string.education_topic_consent
            ContentTopic.RELATIONSHIPS -> Res.string.education_topic_relationships
            ContentTopic.GENERAL_HEALTH -> Res.string.education_topic_general_health
        }

@Composable
fun EducationScreen(
    filteredContent: List<ContentItem>,
    featuredItem: ContentItem?,
    selectedCategory: ContentTopic?,
    searchQuery: String,
    onContentClick: (String) -> Unit,
    onCategorySelect: (ContentTopic?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
        modifier
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

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 12.dp),
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onCategorySelect(null) },
                    label = { Text(stringResource(Res.string.education_category_all)) },
                )
            }
            items(ContentTopic.entries.toTypedArray()) { topic ->
                FilterChip(
                    selected = selectedCategory == topic,
                    onClick = {
                        onCategorySelect(if (selectedCategory == topic) null else topic)
                    },
                    label = {
                        Text(stringResource(topic.labelRes))
                    },
                    leadingIcon = { Icon(topic.icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (selectedCategory == null && searchQuery.isBlank() && featuredItem != null) {
                item {
                    Text(
                        stringResource(Res.string.education_featured_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    FeaturedContentCard(
                        featuredItem,
                        onClick = { onContentClick(featuredItem.id) },
                    )
                }
            }

            item {
                Text(
                    if (searchQuery.isNotBlank()) {
                        stringResource(Res.string.education_search_results)
                    } else if (selectedCategory == null) {
                        stringResource(Res.string.education_latest_updates)
                    } else {
                        stringResource(Res.string.education_library_format, stringResource(selectedCategory.labelRes))
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            items(filteredContent) { item ->
                if (item.id != featuredItem?.id || selectedCategory != null || searchQuery.isNotBlank()) {
                    StandardContentCard(
                        item,
                        onClick = { onContentClick(item.id) },
                    )
                }
            }
        }
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
                Icons.SearchW400Outlinedfill1,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(12.dp))
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
                                stringResource(Res.string.education_search_placeholder),
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
fun FeaturedContentCard(item: ContentItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(220.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier =
                Modifier.fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(item.category.color.copy(alpha = 0.6f), item.category.color),
                        ),
                    ),
            )

            if (item.type == ContentType.VIDEO) {
                Icon(
                    Icons.PlayCircleW400Outlinedfill1,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp).align(Alignment.Center),
                )
            }

            Column(modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                Badge(containerColor = Color.White, contentColor = Color.Black) {
                    Text(stringResource(item.category.labelRes), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    item.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    item.formattedDuration,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.8f),
                )
            }
        }
    }
}

@Composable
fun StandardContentCard(item: ContentItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                Modifier.size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(item.category.color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector =
                    when (item.type) {
                        ContentType.VIDEO -> Icons.PlayCircleW400Outlinedfill1
                        ContentType.QUIZ -> Icons.QuizW400Outlinedfill1
                        ContentType.ARTICLE -> Icons.ArticleW400Outlinedfill1
                    },
                    contentDescription = null,
                    tint = item.category.color,
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "${stringResource(item.category.labelRes)} • ${item.formattedDuration}",
                    style = MaterialTheme.typography.labelSmall,
                    color = item.category.color,
                )
            }
        }
    }
}
