package com.group8.comp2300.service.content

import com.group8.comp2300.domain.model.content.*
import com.group8.comp2300.domain.repository.SRHContentRepository

class MockSRHContentRepository : SRHContentRepository {

    private val contents = listOf(
        SRHContent(
            id = "1",
            title = "Understanding Contraception Options",
            description =
            "A comprehensive guide to different contraception methods " +
                "including pills, IUDs, and implants. Learn about effectiveness, " +
                "side effects, and how to choose the right method for you.",
            contentType = ContentType.ARTICLE,
            topics = listOf(ContentTopic.CONTRACEPTION),
            keywords = listOf("birth control", "pill", "IUD", "implant", "contraception"),
            contentUrl = "/articles/contraception-guide",
            thumbnailUrl = "/images/contraception.jpg",
            author = "Dr. Sarah Johnson",
            publishedDate = "2024-01-15",
            estimatedReadTime = 10,
        ),
        SRHContent(
            id = "2",
            title = "STI Prevention: What You Need to Know",
            description =
            "Learn about STI prevention methods, testing, and safe practices. " +
                "This video covers everything from condom use to regular testing schedules.",
            contentType = ContentType.VIDEO,
            topics = listOf(ContentTopic.STI_PREVENTION),
            keywords = listOf("STI", "STD", "protection", "condom", "safe sex"),
            contentUrl = "/videos/sti-prevention.mp4",
            thumbnailUrl = "/images/sti.jpg",
            author = "Health Education Team",
            publishedDate = "2024-02-01",
            estimatedReadTime = null,
        ),
        SRHContent(
            id = "3",
            title = "Pregnancy: First Trimester Guide",
            description =
            "What to expect during the first trimester of pregnancy. " +
                "Covers symptoms, nutrition, prenatal care, and when to see a doctor.",
            contentType = ContentType.ARTICLE,
            topics = listOf(ContentTopic.PREGNANCY),
            keywords = listOf("pregnancy", "trimester", "baby", "prenatal", "morning sickness"),
            contentUrl = "/articles/pregnancy-guide",
            thumbnailUrl = "/images/pregnancy.jpg",
            author = "Dr. Emily Chen",
            publishedDate = "2024-01-20",
            estimatedReadTime = 15,
        ),
        SRHContent(
            id = "4",
            title = "Understanding Menstrual Cycle",
            description =
            "Learn about the different phases of your menstrual cycle, " +
                "what's normal, and when to seek medical advice.",
            contentType = ContentType.ARTICLE,
            topics = listOf(ContentTopic.MENSTRUAL_HEALTH),
            keywords = listOf("period", "menstruation", "cycle", "ovulation"),
            contentUrl = "/articles/menstrual-cycle",
            thumbnailUrl = "/images/menstrual.jpg",
            author = "Dr. Maria Garcia",
            publishedDate = "2024-02-10",
            estimatedReadTime = 8,
        ),
        SRHContent(
            id = "5",
            title = "Healthy Relationships: Communication Tips",
            description =
            "Video guide on building healthy relationships, " +
                "setting boundaries, and effective communication with partners.",
            contentType = ContentType.VIDEO,
            topics = listOf(ContentTopic.RELATIONSHIPS, ContentTopic.CONSENT),
            keywords = listOf("relationship", "communication", "boundaries", "consent"),
            contentUrl = "/videos/healthy-relationships.mp4",
            thumbnailUrl = "/images/relationships.jpg",
            author = "Dr. James Wilson",
            publishedDate = "2024-02-15",
            estimatedReadTime = null,
        ),
        SRHContent(
            id = "6",
            title = "Emergency Contraception: What You Should Know",
            description =
            "Information about emergency contraception options, " +
                "how they work, and when to use them.",
            contentType = ContentType.ARTICLE,
            topics = listOf(ContentTopic.CONTRACEPTION),
            keywords = listOf("morning after pill", "emergency contraception", "plan b"),
            contentUrl = "/articles/emergency-contraception",
            thumbnailUrl = "/images/emergency.jpg",
            author = "Dr. Sarah Johnson",
            publishedDate = "2024-02-20",
            estimatedReadTime = 7,
        ),
    )

    override fun search(query: String?): List<SRHContent> {
        if (query.isNullOrBlank()) return contents

        val lowercaseQuery = query.lowercase()
        return contents.filter { content ->
            content.title.lowercase().contains(lowercaseQuery) ||
                content.description.lowercase().contains(lowercaseQuery) ||
                content.keywords.any { it.lowercase().contains(lowercaseQuery) }
        }
    }

    override fun findByTopics(topics: List<ContentTopic>): List<SRHContent> {
        if (topics.isEmpty()) return contents
        return contents.filter { content ->
            content.topics.intersect(topics.toSet()).isNotEmpty()
        }
    }

    override fun findByKeywords(keywords: List<String>): List<SRHContent> {
        if (keywords.isEmpty()) return contents
        val lowercaseKeywords = keywords.map { it.lowercase() }
        return contents.filter { content ->
            content.keywords.any { keyword ->
                lowercaseKeywords.any { it in keyword.lowercase() }
            }
        }
    }

    override fun getById(id: String): SRHContent? = contents.find { it.id == id }

    override fun getAll(): List<SRHContent> = contents
}
