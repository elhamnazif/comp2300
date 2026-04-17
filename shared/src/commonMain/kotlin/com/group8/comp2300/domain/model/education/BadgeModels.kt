package com.group8.comp2300.domain.model.education

import kotlinx.serialization.Serializable

@Serializable
data class Badge(
    val id: String,
    val name: String,
    val iconPath: String,
    val isLocked: Boolean = true
)

@Serializable
data class EarnedBadge(
    val badge: Badge,
    val earnedAt: Long
)