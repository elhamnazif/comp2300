package com.group8.comp2300.presentation.ui.screens.education

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.group8.comp2300.presentation.ui.screens.auth.components.color
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDetailScreen(
        viewModel: EducationViewModel = koinViewModel(),
        videoId: String,
        onBack: () -> Unit,
        onActionClick: (String) -> Unit
) {
    val item = viewModel.getContentById(videoId)
    if (item == null) {
        // Handle case where content is not found
        onBack()
        return
    }
    Scaffold(
            topBar = {
                // Transparent TopBar allowing content to shine through
                TopAppBar(
                        title = {},
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        "Back",
                                        tint = Color.White
                                )
                            }
                        },
                        colors =
                                TopAppBarDefaults.topAppBarColors(
                                        containerColor = Color.Transparent
                                )
                )
            }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
            // 1. STICKY VIDEO PLAYER (Mock)
            // Stays pinned at the top, does not scroll with the text
            Box(
                    modifier = Modifier.fillMaxWidth().height(240.dp).background(Color.Black),
                    contentAlignment = Alignment.Center
            ) {
                // Placeholder for ExoPlayer
                Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier =
                                Modifier.size(64.dp)
                                        .background(Color.White.copy(alpha = 0.3f), CircleShape)
                                        .padding(8.dp)
                )
                Text(
                        "Video Player Placeholder",
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp)
                )
            }

            // 2. SCROLLABLE CONTENT
            LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Info
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Badge(containerColor = item.category.color, contentColor = Color.White) {
                            Text(item.category.label, modifier = Modifier.padding(4.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(item.formattedDuration, color = Color.Gray)
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { /* Share */}) { Icon(Icons.Default.Share, null) }
                    }
                }

                item {
                    Text(
                            text = item.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                    )
                }

                // Tags Row
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(item.tags) { tag ->
                            SuggestionChip(onClick = {}, label = { Text("#$tag") })
                        }
                    }
                }

                // The "Knowledge" (Transcript)
                item {
                    Text(
                            text = "Transcript & Key Points",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                            text = item.transcript,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.4
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
                                colors =
                                        ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary
                                        )
                        ) {
                            Icon(Icons.Default.ThumbUp, null)
                            Spacer(Modifier.width(8.dp))
                            Text(relatedAction)
                        }
                    }
                }
            }
        }
    }
}
