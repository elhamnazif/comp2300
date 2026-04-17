package com.group8.comp2300.mock

import com.group8.comp2300.domain.model.education.Category

object ContentCategory {
    val CONTRACEPTION = Category("id-contraception", "Contraception")
    val STI_PREVENTION = Category("id-sti-prevention", "STI Prevention")
    val PUBERTY = Category("id-puberty", "Puberty")
    val MENSTRUAL_HEALTH = Category("id-menstrual-health", "Menstrual Health")
    val CONSENT = Category("id-consent", "Consent")
    val RELATIONSHIPS = Category("id-relationships", "Relationships")
    val SEXUAL_HEALTH = Category("id-sexual-health", "Sexual Health")

    val all = listOf(
        CONTRACEPTION,
        STI_PREVENTION,
        PUBERTY,
        MENSTRUAL_HEALTH,
        CONSENT,
        RELATIONSHIPS,
        SEXUAL_HEALTH
    )
}


