package com.group8.comp2300.presentation.ui.screens.auth.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.app.symbols.icons.materialsymbols.icons.FingerprintW500Outlined
import com.app.symbols.icons.materialsymbols.icons.HealthAndSafetyW500Outlined
import com.group8.comp2300.domain.model.education.ContentCategory
import com.app.symbols.icons.materialsymbols.Icons as MaterialIcons

/**
 * UI-specific extension properties for ContentCategory. These are kept in composeApp since they
 * depend on Compose (Color, ImageVector).
 */
val ContentCategory.color: Color
    get() =
        when (this) {
            ContentCategory.PUBERTY -> Color(0xFFE91E63)
            ContentCategory.RELATIONSHIPS -> Color(0xFF9C27B0)
            ContentCategory.STI -> Color(0xFF2196F3)
            ContentCategory.IDENTITY -> Color(0xFFFF9800)
            ContentCategory.SEXUAL_HEALTH -> Color(0xFF4CAF50)
        }

val ContentCategory.icon: ImageVector
    get() =
        when (this) {
            ContentCategory.PUBERTY -> Icons.Default.Face
            ContentCategory.RELATIONSHIPS -> Icons.Default.Favorite
            ContentCategory.STI -> MaterialIcons.HealthAndSafetyW500Outlined
            ContentCategory.IDENTITY -> MaterialIcons.FingerprintW500Outlined
            ContentCategory.SEXUAL_HEALTH -> Icons.Default.Face
        }
