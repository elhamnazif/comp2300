package com.group8.comp2300.feature.booking

internal fun appointmentTypeLabel(value: String): String = when (value) {
    "STI_TESTING" -> "STI testing"
    "SYMPTOMS" -> "Symptoms"
    "CONTRACEPTION" -> "Contraception"
    "PREP_PEP" -> "PrEP / PEP"
    "FOLLOW_UP" -> "Follow-up"
    else -> value.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase)
}
