package com.group8.comp2300.presentation.screens.auth.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.app.symbols.icons.materialsymbols.Icons as MaterialIcons
import com.app.symbols.icons.materialsymbols.icons.*
import com.group8.comp2300.domain.model.education.ContentCategory

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
            ContentCategory.PUBERTY -> MaterialIcons.FaceW400Outlinedfill1
            ContentCategory.RELATIONSHIPS -> MaterialIcons.FavoriteW400Outlinedfill1
            ContentCategory.STI -> MaterialIcons.HealthAndSafetyW500Outlined
            ContentCategory.IDENTITY -> MaterialIcons.FingerprintW500Outlined
            ContentCategory.SEXUAL_HEALTH -> MaterialIcons.FaceW400Outlinedfill1
        }
