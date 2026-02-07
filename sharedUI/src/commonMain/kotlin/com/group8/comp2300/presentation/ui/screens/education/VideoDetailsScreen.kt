@file:Suppress("FunctionName")

package com.group8.comp2300.presentation.ui.screens.education

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.symbols.icons.materialsymbols.Icons
import com.app.symbols.icons.materialsymbols.icons.*
import com.group8.comp2300.domain.model.education.ContentCategory
import com.group8.comp2300.presentation.ui.screens.auth.components.color
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDetailScreen(
    videoId: String,
    onBack: () -> Unit,
    onActionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EducationViewModel = koinViewModel(),
) {
    val item = viewModel.getContentById(videoId)
    if (item == null) {
        // Handle case where content is not found
        onBack()
        return
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            // Transparent TopBar allowing content to shine through
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.ArrowBackW400Outlinedfill1,
                            stringResource(Res.string.education_video_back_desc),
                            tint = Color.White,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
            // 1. STICKY VIDEO PLAYER (Mock)
            // Stays pinned at the top, does not scroll with the text
            Box(
                modifier = Modifier.fillMaxWidth().height(240.dp).background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                // Placeholder for ExoPlayer
                Icon(
                    imageVector = Icons.PlayArrowW400Outlinedfill1,
                    contentDescription = stringResource(Res.string.education_video_play_desc),
                    tint = Color.White,
                    modifier =
                    Modifier.size(64.dp).background(Color.White.copy(alpha = 0.3f), CircleShape).padding(8.dp),
                )
                Text(
                    stringResource(Res.string.education_video_player_placeholder),
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp),
                )
            }

            // 2. SCROLLABLE CONTENT
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Header Info
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Badge(containerColor = item.category.color, contentColor = Color.White) {
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
                        Spacer(Modifier.width(8.dp))
                        Text(item.formattedDuration, color = Color.Gray)
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { /* Share */ }) { Icon(Icons.ShareW400Outlinedfill1, null) }
                    }
                }

                item {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                // Tags Row
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(item.tags) { tag -> SuggestionChip(onClick = {}, label = { Text("#$tag") }) }
                    }
                }

                // The "Knowledge" (Transcript)
                item {
                    Text(
                        text = stringResource(Res.string.education_video_transcript_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = item.transcript,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.4,
                    )
                }

                // 3. CALL TO ACTION (Contextual)
                // Links education to the "Care" or "Shop" tabs
                val relatedAction = item.relatedAction
                if (relatedAction != null) {
                    item {
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { onActionClick(relatedAction) },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        ) {
                            Icon(Icons.ThumbUpW400Outlinedfill1, null)
                            Spacer(Modifier.width(8.dp))
                            val actionRes =
                                when (relatedAction) {
                                    "Take Consent Quiz" -> Res.string.education_video_action_take_quiz
                                    "Find PrEP Clinic" -> Res.string.education_video_action_find_clinic
                                    "Start Quiz" -> Res.string.education_video_action_start_quiz
                                    else -> null
                                }
                            Text(actionRes?.let { stringResource(it) } ?: relatedAction)
                        }
                    }
                }
            }
        }
    }
}
