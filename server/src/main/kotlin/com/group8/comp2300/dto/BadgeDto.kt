package com.group8.comp2300.dto

import com.group8.comp2300.domain.model.education.Badge
import com.group8.comp2300.domain.model.education.EarnedBadge
import kotlinx.serialization.Serializable
@Serializable
data class EarnedBadgeDto(val id: String, val name: String, val iconPath: String, val earnedAt: Long)

@Serializable
data class BadgeResponseDto(val id: String, val name: String, val iconPath: String)

fun Badge.toDto() = BadgeResponseDto(
    id = id,
    name = name,
    iconPath = iconPath,
)

fun EarnedBadge.toDto() = EarnedBadgeDto(
    id = this.badge.id,
    name = this.badge.name,
    iconPath = this.badge.iconPath,
    earnedAt = this.earnedAt,
)
