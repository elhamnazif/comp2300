@file:Suppress("FunctionName")

package com.group8.comp2300.presentation.ui.screens.education

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.symbols.icons.materialsymbols.Icons
import com.app.symbols.icons.materialsymbols.icons.*
import com.app.symbols.icons.materialsymbols.icons.ArticleW500Outlined
import com.app.symbols.icons.materialsymbols.icons.PlayCircleW500Outlined
import com.app.symbols.icons.materialsymbols.icons.QuizW500Outlined
import com.group8.comp2300.domain.model.education.ContentCategory
import com.group8.comp2300.domain.model.education.ContentItem
import com.group8.comp2300.domain.model.education.ContentType
import com.group8.comp2300.presentation.ui.screens.auth.components.color
import com.group8.comp2300.presentation.ui.screens.auth.components.icon
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/** Pure UI component for the Education screen. Takes state and callbacks, no ViewModel dependency. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EducationScreen(
    filteredContent: List<ContentItem>,
    featuredItem: ContentItem?,
    selectedCategory: ContentCategory?,
    onContentClick: (String) -> Unit,
    onCategorySelect: (ContentCategory?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .systemBarsPadding(),
    ) {
        // 1. Search Header
        SearchBar(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(stringResource(Res.string.education_search_placeholder))
        }

        // 2. Category Pills (The "Spokes")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 16.dp),
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onCategorySelect(null) },
                    label = { Text(stringResource(Res.string.education_category_all)) },
                )
            }
            items(ContentCategory.values()) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = {
                        // Toggle logic
                        onCategorySelect(if (selectedCategory == category) null else category)
                    },
                    label = {
                        val labelRes =
                            when (category) {
                                ContentCategory.PUBERTY -> Res.string.education_category_puberty
                                ContentCategory.RELATIONSHIPS -> Res.string.education_category_relationships
                                ContentCategory.STI -> Res.string.education_category_sti
                                ContentCategory.IDENTITY -> Res.string.education_category_identity
                                ContentCategory.SEXUAL_HEALTH -> Res.string.education_category_sexual_health
                            }
                        Text(stringResource(labelRes))
                    },
                    leadingIcon = { Icon(category.icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 3. Hero Card (Only show on "All" view)
            if (selectedCategory == null && featuredItem != null) {
                item {
                    Text(
                        stringResource(Res.string.education_featured_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    FeaturedContentCard(featuredItem, onClick = { onContentClick(featuredItem.id) })
                }
            }

            // 4. The Feed
            item {
                Text(
                    if (selectedCategory == null) {
                        stringResource(Res.string.education_latest_updates)
                    } else {
                        val labelRes =
                            when (selectedCategory) {
                                ContentCategory.PUBERTY -> Res.string.education_category_puberty
                                ContentCategory.RELATIONSHIPS -> Res.string.education_category_relationships
                                ContentCategory.STI -> Res.string.education_category_sti
                                ContentCategory.IDENTITY -> Res.string.education_category_identity
                                ContentCategory.SEXUAL_HEALTH -> Res.string.education_category_sexual_health
                            }
                        stringResource(Res.string.education_library_format, stringResource(labelRes))
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            items(filteredContent) { item ->
                // Don't duplicate the featured item in the list
                if (item.id != featuredItem?.id || selectedCategory != null) {
                    StandardContentCard(item, onClick = { onContentClick(item.id) })
                }
            }
        }
    }
}

// --- Helper Components ---

@Composable
fun SearchBar(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
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
            Box(Modifier.weight(1f)) { content() }
        }
    }
}

@Composable
fun FeaturedContentCard(item: ContentItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(200.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Gradient Background (Placeholder for Image)
            Box(
                modifier =
                Modifier.fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(item.category.color.copy(alpha = 0.6f), item.category.color),
                        ),
                    ),
            )

            // Play Icon Overlay
            if (item.type == ContentType.VIDEO) {
                Icon(
                    com.app.symbols.icons.materialsymbols.Icons.PlayCircleW500Outlined,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp).align(Alignment.Center),
                )
            }

            // Text Content
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                Badge(containerColor = Color.White, contentColor = Color.Black) {
                    val labelRes =
                        when (item.category) {
                            ContentCategory.PUBERTY -> Res.string.education_category_puberty
                            ContentCategory.RELATIONSHIPS -> Res.string.education_category_relationships
                            ContentCategory.STI -> Res.string.education_category_sti
                            ContentCategory.IDENTITY -> Res.string.education_category_identity
                            ContentCategory.SEXUAL_HEALTH -> Res.string.education_category_sexual_health
                        }
                    Text(stringResource(labelRes), modifier = Modifier.padding(4.dp))
                }
                Spacer(Modifier.height(8.dp))
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
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Thumbnail Placeholder
            Box(
                modifier =
                Modifier.size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(item.category.color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector =
                    when (item.type) {
                        ContentType.VIDEO -> com.app.symbols.icons.materialsymbols.Icons.PlayCircleW500Outlined
                        ContentType.QUIZ -> com.app.symbols.icons.materialsymbols.Icons.QuizW500Outlined
                        ContentType.ARTICLE -> com.app.symbols.icons.materialsymbols.Icons.ArticleW500Outlined
                    },
                    contentDescription = null,
                    tint = item.category.color,
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
                Spacer(Modifier.height(4.dp))
                val labelRes =
                    when (item.category) {
                        ContentCategory.PUBERTY -> Res.string.education_category_puberty
                        ContentCategory.RELATIONSHIPS -> Res.string.education_category_relationships
                        ContentCategory.STI -> Res.string.education_category_sti
                        ContentCategory.IDENTITY -> Res.string.education_category_identity
                        ContentCategory.SEXUAL_HEALTH -> Res.string.education_category_sexual_health
                    }
                Text(
                    "${stringResource(labelRes)} • ${item.formattedDuration}",
                    style = MaterialTheme.typography.labelSmall,
                    color = item.category.color,
                )
            }
        }
    }
}
