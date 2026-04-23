package com.group8.comp2300.feature.booking

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.group8.comp2300.core.network.resolveRemoteAssetUrl
import com.group8.comp2300.domain.model.medical.Clinic

@Composable
internal fun ClinicImage(clinic: Clinic, modifier: Modifier = Modifier, contentDescription: String? = null) {
    val platformContext = LocalPlatformContext.current
    var hasError by remember(clinic.imageUrl) { mutableStateOf(false) }
    val request = resolveClinicImageUrl(clinic.imageUrl)?.let { imageUrl ->
        ImageRequest.Builder(platformContext)
            .data(imageUrl)
            .build()
    }

    if (request != null && !hasError) {
        AsyncImage(
            model = request,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop,
            onError = { hasError = true },
            onSuccess = { hasError = false },
        )
    } else {
        Box(modifier = modifier)
    }
}

private fun resolveClinicImageUrl(imageUrl: String?): String? = resolveRemoteAssetUrl(imageUrl)
