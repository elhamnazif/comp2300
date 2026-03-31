package com.group8.comp2300.presentation.screens.auth.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.group8.comp2300.domain.model.education.ContentCategory
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.*

/**
 * UI-specific extension properties for ContentCategory. These are kept in client since they
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
            ContentCategory.PUBERTY -> Icons.FaceW400Outlinedfill1
            ContentCategory.RELATIONSHIPS -> Icons.FavoriteW400Outlinedfill1
            ContentCategory.STI -> Icons.HealthAndSafetyW400Outlinedfill1
            ContentCategory.IDENTITY -> Icons.FingerprintW400Outlinedfill1
            ContentCategory.SEXUAL_HEALTH -> Icons.FaceW400Outlinedfill1
        }
