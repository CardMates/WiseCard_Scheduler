package com.wisecard.scheduler.infrastructure.storage.adapter

import com.wisecard.scheduler.domain.card.CardCompany
import com.wisecard.scheduler.domain.card.CardInfo
import com.wisecard.scheduler.domain.card.CardType
import com.wisecard.scheduler.infrastructure.storage.CardDataStorageService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class CardStorageAdapterTest {

    private lateinit var storageService: CardDataStorageService
    private lateinit var adapter: CardStorageAdapter
    private lateinit var testDataFile: File

    @BeforeEach
    fun setUp(@TempDir tempDir: Path) {
        val tempDataDir = File(tempDir.toFile(), "data")
        tempDataDir.mkdirs()
        testDataFile = File(tempDataDir, "crawled_cards.json")

        storageService = CardDataStorageService(testDataFile.path)
        adapter = CardStorageAdapter(storageService)
    }

    @Test
    fun `저장된 카드 로드`() {
        val cards = listOf(
            createCardInfo(1, "카드1"),
            createCardInfo(2, "카드2")
        )

        adapter.saveCards(cards)
        val loaded = adapter.loadStoredCards()

        assertThat(loaded).hasSize(2)
        assertThat(loaded[0].cardName).isEqualTo("카드1")
        assertThat(loaded[1].cardName).isEqualTo("카드2")
    }

    @Test
    fun `신규 카드 필터링`() {
        val existingCards = listOf(
            createCardInfo(1, "기존카드1"),
            createCardInfo(2, "기존카드2")
        )
        adapter.saveCards(existingCards)

        val crawledCards = listOf(
            createCardInfo(null, "기존카드1"),
            createCardInfo(null, "신규카드1"),
            createCardInfo(null, "기존카드2"),
            createCardInfo(null, "신규카드2")
        )

        val newCards = adapter.filterNewCards(crawledCards)

        assertThat(newCards).hasSize(2)
        assertThat(newCards.map { it.cardName }).containsExactly("신규카드1", "신규카드2")
    }

    @Test
    fun `신규 카드에 ID 할당`() {
        val existingCards = listOf(
            createCardInfo(1, "카드1"),
            createCardInfo(5, "카드5")
        )
        adapter.saveCards(existingCards)

        val newCards = listOf(
            createCardInfo(null, "신규카드1"),
            createCardInfo(null, "신규카드2")
        )

        val cardsWithIds = adapter.assignIdsToNewCards(newCards)

        assertThat(cardsWithIds).hasSize(2)
        assertThat(cardsWithIds[0].cardId).isEqualTo(6)
        assertThat(cardsWithIds[1].cardId).isEqualTo(7)
    }

    @Test
    fun `저장된 카드가 없을 때 ID는 1부터 시작`() {
        val newCards = listOf(
            createCardInfo(null, "신규카드1"),
            createCardInfo(null, "신규카드2")
        )

        val cardsWithIds = adapter.assignIdsToNewCards(newCards)

        assertThat(cardsWithIds[0].cardId).isEqualTo(1)
        assertThat(cardsWithIds[1].cardId).isEqualTo(2)
    }

    @Test
    fun `카드 병합`() {
        val existingCards = listOf(
            createCardInfo(1, "기존카드1"),
            createCardInfo(2, "기존카드2")
        )

        val newCards = listOf(
            createCardInfo(3, "신규카드1"),
            createCardInfo(4, "신규카드2")
        )

        val merged = adapter.mergeCards(existingCards, newCards)

        assertThat(merged).hasSize(4)
        assertThat(merged.map { it.cardName }).containsExactly(
            "기존카드1", "기존카드2", "신규카드1", "신규카드2"
        )
    }

    @Test
    fun `빈 리스트 저장 및 로드`() {
        adapter.saveCards(emptyList())
        val loaded = adapter.loadStoredCards()

        assertThat(loaded).isEmpty()
    }

    private fun createCardInfo(id: Int?, name: String): CardInfo {
        return CardInfo(
            cardId = id,
            cardUrl = "https://example.com/$name",
            cardCompany = CardCompany.SAMSUNG,
            cardName = name,
            imgUrl = "https://example.com/img/$name.jpg",
            cardType = CardType.CREDIT,
            benefits = null
        )
    }
}