package com.group8.comp2300.domain.model.user

import kotlinx.serialization.Serializable

@Serializable
enum class Gender(val displayName: String) {
    MALE("Male"),
    FEMALE("Female"),
    NON_BINARY("Non-binary"),
    PREFER_NOT_TO_SAY("Prefer not to say")
    ;

    companion object {
        fun fromDisplayName(displayName: String): Gender? = entries.find { it.displayName == displayName }
    }
}
