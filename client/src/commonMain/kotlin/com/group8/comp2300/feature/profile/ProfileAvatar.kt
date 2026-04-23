package com.group8.comp2300.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.group8.comp2300.core.network.resolveRemoteAssetUrl
import com.group8.comp2300.core.ui.components.shimmerEffect

@Composable
internal fun ProfileAvatar(
    initials: String,
    imageModel: Any?,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
) {
    val platformContext = LocalPlatformContext.current
    val resolvedModel = remember(imageModel) {
        when (imageModel) {
            is String -> resolveRemoteAssetUrl(imageModel)
            else -> imageModel
        }
    }
    var hasImageError by remember(resolvedModel) { mutableStateOf(false) }
    val request = resolvedModel?.let { data ->
        ImageRequest.Builder(platformContext)
            .data(data)
            .build()
    }

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier
            .size(80.dp)
            .then(if (isLoading && request == null && initials.isEmpty()) Modifier.shimmerEffect() else Modifier),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            if (request != null && !hasImageError) {
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    onError = { hasImageError = true },
                    onSuccess = { hasImageError = false },
                )
            } else if (!isLoading || initials.isNotEmpty()) {
                Text(
                    initials,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}
