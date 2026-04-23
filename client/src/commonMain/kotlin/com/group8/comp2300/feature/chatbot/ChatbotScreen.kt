package com.group8.comp2300.feature.chatbot

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.group8.comp2300.core.ui.components.AppTopBar
import com.group8.comp2300.domain.model.chatbot.ChatbotMessage
import com.group8.comp2300.domain.model.chatbot.ChatbotRole
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.DeleteW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.SendW400Outlinedfill1
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.model.rememberMarkdownState
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ChatbotScreen(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: ChatbotViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size, state.isSending, state.errorMessage) {
        val trailingItems = state.messages.size +
            if (state.isSending) 1 else 0 +
            if (state.errorMessage != null) 1 else 0
        val shouldAutoScroll = state.messages.lastOrNull()?.role == ChatbotRole.USER || listState.isNearBottom()
        if (trailingItems > 0 && shouldAutoScroll) {
            listState.animateScrollToItem(trailingItems - 1)
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        topBar = {
            AppTopBar(
                title = { Text(stringResource(Res.string.chatbot_title)) },
                onBackClick = onBack,
                actions = {
                    if (state.messages.isNotEmpty() || state.draftMessage.isNotBlank()) {
                        IconButton(onClick = viewModel::clearConversation) {
                            Icon(
                                imageVector = Icons.DeleteW400Outlinedfill1,
                                contentDescription = stringResource(Res.string.chatbot_clear_desc),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            TextField(
                                value = state.draftMessage,
                                onValueChange = viewModel::updateDraftMessage,
                                placeholder = { Text(stringResource(Res.string.chatbot_input_placeholder)) },
                                modifier = Modifier.weight(1f),
                                minLines = 1,
                                maxLines = 5,
                                shape = RoundedCornerShape(24.dp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = { viewModel.sendDraftMessage() }),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent,
                                ),
                            )

                            ComposerSendButton(
                                enabled = state.canSend,
                                onClick = viewModel::sendDraftMessage,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        if (state.messages.isEmpty() && !state.isSending && state.draftMessage.isBlank()) {
            EmptyChatState(
                onPromptSelected = viewModel::sendPrompt,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .windowInsetsPadding(WindowInsets.safeDrawing),
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(
                    items = state.messages,
                    key = { index, message -> "${index}_${message.role.name}" },
                ) { _, message ->
                    MessageItem(message = message)
                }

                if (state.isSending) {
                    item {
                        TypingIndicator()
                    }
                }

                state.errorMessage?.let { errorMessage ->
                    item(key = "reply_error") {
                        ReplyErrorItem(
                            message = errorMessage,
                            onRetry = viewModel::retryLastMessage,
                            showRetry = state.canRetry,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyChatState(onPromptSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    val starterPrompts = listOf(
        stringResource(Res.string.chatbot_empty_prompt_booking),
        stringResource(Res.string.chatbot_empty_prompt_tracking),
        stringResource(Res.string.chatbot_empty_prompt_help),
    )

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerLowest),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .widthIn(max = 360.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(Res.string.chatbot_empty_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(Res.string.chatbot_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                FlowRow(
                    modifier = Modifier.widthIn(max = 320.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    starterPrompts.forEach { prompt ->
                        AssistChip(
                            onClick = { onPromptSelected(prompt) },
                            modifier = Modifier.heightIn(min = 40.dp),
                            label = { Text(prompt) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageItem(message: ChatbotMessage) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val userBubbleMaxWidth = minOf(maxWidth * 0.8f, 320.dp)
        val assistantBubbleMaxWidth = minOf(maxWidth * 0.9f, 420.dp)

        if (message.role == ChatbotRole.USER) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.widthIn(max = userBubbleMaxWidth),
                ) {
                    Text(
                        text = message.content,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            return@BoxWithConstraints
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.widthIn(max = assistantBubbleMaxWidth),
            ) {
                val markdownState = rememberMarkdownState(message.content, retainState = true)
                Markdown(
                    markdownState = markdownState,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    error = {
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val assistantBubbleMaxWidth = minOf(maxWidth * 0.9f, 420.dp)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.widthIn(max = assistantBubbleMaxWidth),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TypingDots()
                Text(
                    text = stringResource(Res.string.chatbot_loading_reply),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TypingDots(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "TypingDots")
    Row(
        modifier = modifier.width(24.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.35f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 420, delayMillis = index * 140),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "TypingDotAlpha$index",
            )
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                        shape = CircleShape,
                    ),
            )
        }
    }
}

@Composable
private fun ReplyErrorItem(message: String, onRetry: () -> Unit, showRetry: Boolean) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val assistantBubbleMaxWidth = minOf(maxWidth * 0.9f, 420.dp)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.widthIn(max = assistantBubbleMaxWidth),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                if (showRetry) {
                    TextButton(
                        onClick = onRetry,
                        modifier = Modifier.wrapContentWidth(Alignment.End).align(Alignment.End),
                    ) {
                        Text(stringResource(Res.string.chatbot_retry))
                    }
                }
            }
        }
    }
}

@Composable
private fun ComposerSendButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && isPressed) 0.96f else 1f,
        label = "ComposerSendButtonScale",
    )

    Surface(
        modifier = modifier
            .size(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = CircleShape,
        color = if (enabled) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            interactionSource = interactionSource,
        ) {
            Icon(
                imageVector = Icons.SendW400Outlinedfill1,
                contentDescription = stringResource(Res.string.chatbot_send_desc),
                tint = if (enabled) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

private fun LazyListState.isNearBottom(buffer: Int = 2): Boolean {
    val totalItems = layoutInfo.totalItemsCount
    if (totalItems == 0) return true

    val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return true
    return lastVisibleIndex >= totalItems - 1 - buffer
}
