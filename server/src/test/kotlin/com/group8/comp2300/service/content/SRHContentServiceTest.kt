package com.group8.comp2300.service.content

import com.group8.comp2300.domain.model.content.*
import kotlin.test.*

class SRHContentServiceTest {

    private lateinit var service: SRHContentService
    private lateinit var repository: MockSRHContentRepository

    @BeforeTest
    fun setup() {
        repository = MockSRHContentRepository()
        service = SRHContentService(repository)
    }

    @Test
    fun `search by query returns matching content`() {
        // Given
        val request = SearchRequest(query = "contraception")

        // When
        val results = service.searchContent(request)

        // Then
        assertTrue(results.isNotEmpty())
        assertTrue(
            results.any {
                it.content.title.contains("contraception", ignoreCase = true)
            },
        )
    }

    @Test
    fun `filter by topic returns content with matching topic`() {
        // Given
        val request = SearchRequest(
            topics = listOf(ContentTopic.CONTRACEPTION),
        )

        // When
        val results = service.searchContent(request)

        // Then
        assertTrue(results.isNotEmpty())
        assertTrue(
            results.all {
                it.content.topics.contains(ContentTopic.CONTRACEPTION)
            },
        )
    }

    @Test
    fun `filter by multiple topics returns content matching any topic`() {
        // Given
        val request = SearchRequest(
            topics = listOf(ContentTopic.CONTRACEPTION, ContentTopic.STI_PREVENTION),
        )

        // When
        val results = service.searchContent(request)

        // Then
        assertTrue(results.isNotEmpty())
        assertTrue(
            results.all {
                it.content.topics.intersect(setOf(ContentTopic.CONTRACEPTION, ContentTopic.STI_PREVENTION)).isNotEmpty()
            },
        )
    }

    @Test
    fun `filter by content type returns only that type`() {
        // Given
        val request = SearchRequest(
            contentType = ContentType.ARTICLE,
        )

        // When
        val results = service.searchContent(request)

        // Then
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.content.contentType == ContentType.ARTICLE })
    }

    @Test
    fun `combine search query and filters returns refined results`() {
        // Given
        val request = SearchRequest(
            query = "contraception",
            topics = listOf(ContentTopic.CONTRACEPTION),
            contentType = ContentType.ARTICLE,
        )

        // When
        val results = service.searchContent(request)

        // Then
        assertTrue(results.isNotEmpty())
        assertTrue(
            results.all {
                it.content.contentType == ContentType.ARTICLE &&
                    it.content.topics.contains(ContentTopic.CONTRACEPTION)
            },
        )
    }

    @Test
    fun `getContentById returns correct content`() {
        // When
        val content = service.getContentById("1")

        // Then
        assertNotNull(content)
        assertEquals("Understanding Contraception Options", content.title)
    }

    @Test
    fun `getAllTopics returns all topic enums`() {
        // When
        val topics = service.getAllTopics()

        // Then
        assertEquals(7, topics.size)
        assertTrue(topics.contains(ContentTopic.CONTRACEPTION))
        assertTrue(topics.contains(ContentTopic.STI_PREVENTION))
        assertTrue(topics.contains(ContentTopic.PREGNANCY))
    }

    @Test
    fun `search results are sorted by relevance`() {
        // Given
        val request = SearchRequest(query = "contraception")

        // When
        val results = service.searchContent(request)

        // Then
        for (i in 0 until results.size - 1) {
            assertTrue(results[i].relevanceScore >= results[i + 1].relevanceScore)
        }
    }
}
