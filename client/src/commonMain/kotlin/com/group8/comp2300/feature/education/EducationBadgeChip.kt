package com.group8.comp2300.feature.education

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.group8.comp2300.core.network.resolveRemoteAssetUrl
import com.group8.comp2300.core.ui.components.AppTopBar
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.EmojiEventsW400Outlinedfill1

internal data class EducationBadgePreview(val badgeName: String, val iconPath: String?)

@Composable
internal fun EducationBadgeChip(
    badgeName: String,
    iconPath: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AssistChip(
        onClick = onClick,
        modifier = modifier,
        leadingIcon = {
            EducationBadgeArtwork(
                iconPath = iconPath,
                contentDescription = null,
            )
        },
        label = {
            Text(
                text = badgeName.replace('_', ' '),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}

@Composable
internal fun EducationBadgeArtwork(iconPath: String?, contentDescription: String?, modifier: Modifier = Modifier) {
    val platformContext = LocalPlatformContext.current
    var hasError by remember(iconPath) { mutableStateOf(false) }
    val request = resolveRemoteAssetUrl(iconPath)?.let { imageUrl ->
        ImageRequest.Builder(platformContext)
            .data(imageUrl)
            .build()
    }

    Surface(
        modifier = modifier.size(24.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            if (request != null && !hasError) {
                AsyncImage(
                    model = request,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    onError = { hasError = true },
                    onSuccess = { hasError = false },
                )
            } else {
                Icon(
                    imageVector = Icons.EmojiEventsW400Outlinedfill1,
                    contentDescription = contentDescription,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
internal fun EducationBadgePreviewDialog(preview: EducationBadgePreview, onDismiss: () -> Unit) {
    val platformContext = LocalPlatformContext.current
    var hasError by remember(preview.iconPath) { mutableStateOf(false) }
    val request = resolveRemoteAssetUrl(preview.iconPath)?.let { imageUrl ->
        ImageRequest.Builder(platformContext)
            .data(imageUrl)
            .build()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                AppTopBar(
                    title = {
                        Text(
                            text = preview.badgeName.replace('_', ' '),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    onBackClick = onDismiss,
                    backContentDescription = "Close badge preview",
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (request != null && !hasError) {
                                AsyncImage(
                                    model = request,
                                    contentDescription = preview.badgeName.replace('_', ' '),
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxWidth(),
                                    onError = { hasError = true },
                                    onSuccess = { hasError = false },
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.EmojiEventsW400Outlinedfill1,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(120.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
