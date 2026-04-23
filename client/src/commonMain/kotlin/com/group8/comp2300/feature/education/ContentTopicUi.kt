package com.group8.comp2300.feature.education

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.group8.comp2300.domain.model.content.ContentTopic
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.*

/**
 * UI-specific extension properties for ContentTopic. These are kept in client since they
 * depend on Compose (Color, ImageVector).
 */
val ContentTopic.color: Color
    get() =
        when (this) {
            ContentTopic.CONTRACEPTION -> Color(0xFF4CAF50)
            ContentTopic.STI_PREVENTION -> Color(0xFF2196F3)
            ContentTopic.PREGNANCY -> Color(0xFFE91E63)
            ContentTopic.MENSTRUAL_HEALTH -> Color(0xFFF44336)
            ContentTopic.CONSENT -> Color(0xFF9C27B0)
            ContentTopic.RELATIONSHIPS -> Color(0xFF7B1FA2)
            ContentTopic.GENERAL_HEALTH -> Color(0xFF009688)
        }

val ContentTopic.icon: ImageVector
    get() =
        when (this) {
            ContentTopic.CONTRACEPTION -> Icons.LocalPharmacyW400Outlined
            ContentTopic.STI_PREVENTION -> Icons.HealthAndSafetyW400Outlined
            ContentTopic.PREGNANCY -> Icons.FaceW400Outlinedfill1
            ContentTopic.MENSTRUAL_HEALTH -> Icons.FavoriteW400Outlinedfill1
            ContentTopic.CONSENT -> Icons.ShieldW400Outlined
            ContentTopic.RELATIONSHIPS -> Icons.AccountBoxW400Outlinedfill1
            ContentTopic.GENERAL_HEALTH -> Icons.InfoW400Outlinedfill1
        }

