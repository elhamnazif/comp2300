package com.group8.comp2300.domain.model.education

import kotlinx.serialization.Serializable

/** Content category identifier - UI-specific properties (color, icon) are mapped in composeApp */
@Serializable
enum class ContentCategory(val label: String) {
    PUBERTY("Puberty"),
    RELATIONSHIPS("Relationships"),
    STI("STIs & Safety"),
    IDENTITY("Identity"),
    SEXUAL_HEALTH("Sexual Health"),
}
