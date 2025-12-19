package com.group8.comp2300.domain.model.user

import kotlinx.serialization.Serializable

@Serializable
enum class SexualOrientation(val displayName: String) {
    HETEROSEXUAL("Heterosexual"),
    GAY("Gay"),
    LESBIAN("Lesbian"),
    BISEXUAL("Bisexual"),
    PANSEXUAL("Pansexual"),
    ASEXUAL("Asexual"),
    PREFER_NOT_TO_SAY("Prefer not to say"),
    ;

    companion object {
        fun fromDisplayName(displayName: String): SexualOrientation? = entries.find { it.displayName == displayName }
    }
}
