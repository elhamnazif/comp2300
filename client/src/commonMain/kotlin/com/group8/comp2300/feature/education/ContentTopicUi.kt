package com.group8.comp2300.feature.education

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.group8.comp2300.domain.model.content.ContentTopic
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.*
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.StringResource

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

val ContentTopic.labelRes: StringResource
    get() =
        when (this) {
            ContentTopic.CONTRACEPTION -> Res.string.education_topic_contraception
            ContentTopic.STI_PREVENTION -> Res.string.education_topic_sti_prevention
            ContentTopic.PREGNANCY -> Res.string.education_topic_pregnancy
            ContentTopic.MENSTRUAL_HEALTH -> Res.string.education_topic_menstrual_health
            ContentTopic.CONSENT -> Res.string.education_topic_consent
            ContentTopic.RELATIONSHIPS -> Res.string.education_topic_relationships
            ContentTopic.GENERAL_HEALTH -> Res.string.education_topic_general_health
        }
