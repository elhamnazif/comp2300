package com.group8.comp2300.mock

import com.group8.comp2300.domain.model.education.ContentCategory
import com.group8.comp2300.domain.model.education.ContentItem
import com.group8.comp2300.domain.model.education.ContentType

val educationContent =
    listOf(
        ContentItem(
            id = "AMZ-001",
            title = "Consent: The FRIES Model",
            description =
                "Freely given, Reversible, Informed, Enthusiastic, and Specific. The new standard.",
            category = ContentCategory.RELATIONSHIPS,
            type = ContentType.VIDEO,
            durationMinutes = 4,
            isFeatured = true,
            videoUrl = "https://www.youtube.com/watch?v=oQbei5JGiT8",
            transcript =
                """
            Consent is crucial in every relationship. We use the F.R.I.E.S. acronym to remember it:
            
            F - Freely Given: Consent is a choice you make without pressure, manipulation, or under the influence of drugs or alcohol.
            
            R - Reversible: Anyone can change their mind about what they feel like doing, anytime. Even if you've done it before.
            
            I - Informed: You can only consent to something if you have the full story. For example, if someone says they'll use a condom and then they don't, there isn't full consent.
            
            E - Enthusiastic: When it comes to sex, you should only do stuff you WANT to do, not things that you feel you're expected to do.
            
            S - Specific: Saying yes to one thing (like kissing) doesn't mean you've said yes to others (like sex).
                """.trimIndent(),
            tags = listOf("Consent", "Safety", "Communication"),
            relatedAction = "Take Consent Quiz"
        ),
        ContentItem(
            id = "AMZ-004",
            title = "PrEP Basics: The Daily Pill",
            description = "How one pill a day can prevent HIV. Is it right for you?",
            category = ContentCategory.STI,
            type = ContentType.VIDEO,
            durationMinutes = 3,
            transcript =
                """
            PrEP stands for Pre-Exposure Prophylaxis. It is a pill you take every day that can protect you from HIV, even if your partner is HIV positive.
            
            Think of it like sunscreen for your immune system. It doesn't protect against other STIs like Syphilis or Gonorrhea (so condoms are still important!), but it is up to 99% effective at preventing HIV transmission via sex.
            
            You can get PrEP from most clinics, often for free or low cost depending on your insurance.
                """.trimIndent(),
            tags = listOf("HIV", "Prevention", "Medication"),
            relatedAction = "Find PrEP Clinic"
        ),
        ContentItem(
            id = "conception-basics",
            title = "Conception Basics Quiz",
            description =
                "Test your knowledge about fertility, conception, and contraception",
            category = ContentCategory.SEXUAL_HEALTH,
            type = ContentType.QUIZ,
            durationMinutes = 5,
            tags = listOf("Conception", "Fertility", "Contraception", "Education"),
            relatedAction = "Start Quiz"
        )
    )
