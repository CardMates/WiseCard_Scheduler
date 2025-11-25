package com.wisecard.scheduler.application.usecase

import com.wisecard.scheduler.application.ports.out.BenefitsRefiner
import com.wisecard.scheduler.application.ports.out.CardBenefitsSender
import com.wisecard.scheduler.application.ports.out.CardCrawlingPort
import com.wisecard.scheduler.application.ports.out.CardStoragePort
import com.wisecard.scheduler.domain.card.CardCompany
import com.wisecard.scheduler.domain.card.CardInfo
import com.wisecard.scheduler.domain.card.CardType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CrawlRefineAndSendCardsUseCaseTest {

    private lateinit var benefitsRefiner: BenefitsRefiner
    private lateinit var cardCrawlingPort: CardCrawlingPort
    private lateinit var cardStoragePort: CardStoragePort
    private lateinit var cardBenefitsSender: CardBenefitsSender
    private lateinit var useCase: CrawlRefineAndSendCardsUseCase

    @BeforeEach
    fun setUp() {
        benefitsRefiner = mockk()
        cardCrawlingPort = mockk()
        cardStoragePort = mockk()
        cardBenefitsSender = mockk()

        useCase = CrawlRefineAndSendCardsUseCase(
            benefitsRefiner = benefitsRefiner,
            cardCrawlingPort = cardCrawlingPort,
            cardStoragePort = cardStoragePort,
            cardBenefitsSender = cardBenefitsSender
        )
    }

    @Test
    fun `신규 카드가 없으면 기존 카드만 전송`() {
        // given
        val existingCards = listOf(
            createCardInfo(1, "기존카드1", null),
            createCardInfo(2, "기존카드2", null)
        )

        every { cardStoragePort.loadStoredCards() } returns existingCards
        every { cardCrawlingPort.crawlAllBasicCards() } returns existingCards
        every { cardStoragePort.filterNewCards(any()) } returns emptyList()
        every { cardBenefitsSender.send(any()) } returns Unit

        // when
        useCase.execute()

        // then
        verify(exactly = 1) { cardBenefitsSender.send(any()) }
        verify(exactly = 0) { benefitsRefiner.refine(any()) } // llm 호출하지 않음
    }

    @Test
    fun `신규 카드가 있으면 크롤링 정제 저장 전송 수행`() {
        // given
        val existingCards = listOf(createCardInfo(1, "기존카드", null))
        val newBasicCards = listOf(createCardInfo(null, "신규카드", null))
        val detailedCards = listOf(createCardInfo(null, "신규카드", "raw benefits"))
        val cardsWithIds = listOf(createCardInfo(2, "신규카드", "raw benefits"))
        val refinedCards = listOf(
            createCardInfo(
                2,
                "신규카드",
                """{"benefits": [{"discounts": [], "points": [], "cashbacks": [], "categories": [], "targets": [], "summary": "정제됨"}]}"""
            )
        )
        val allCards = existingCards + cardsWithIds

        every { cardStoragePort.loadStoredCards() } returns existingCards
        every { cardCrawlingPort.crawlAllBasicCards() } returns existingCards + newBasicCards
        every { cardStoragePort.filterNewCards(any()) } returns newBasicCards
        every { cardCrawlingPort.crawlDetailsFor(any()) } returns detailedCards
        every { cardStoragePort.assignIdsToNewCards(any()) } returns cardsWithIds
        every { benefitsRefiner.refine("raw benefits") } returns """{"benefits": [{"discounts": [], "points": [], "cashbacks": [], "categories": [], "targets": [], "summary": "정제됨"}]}"""
        every { cardStoragePort.mergeCards(existingCards, refinedCards) } returns allCards
        every { cardStoragePort.saveCards(any()) } returns Unit
        every { cardBenefitsSender.send(any()) } returns Unit

        // when
        useCase.execute()

        // then
        verify(exactly = 1) { cardCrawlingPort.crawlDetailsFor(newBasicCards) }
        verify(exactly = 1) { benefitsRefiner.refine("raw benefits") }
        verify(exactly = 1) { cardStoragePort.saveCards(allCards) }
        verify(exactly = 1) { cardBenefitsSender.send(any()) }
    }

    private fun createCardInfo(id: Int?, name: String, benefits: String?): CardInfo {
        return CardInfo(
            cardId = id,
            cardUrl = "https://example.com/$name",
            cardCompany = CardCompany.SAMSUNG,
            cardName = name,
            imgUrl = "https://example.com/img/$name.jpg",
            cardType = CardType.CREDIT,
            benefits = benefits
        )
    }
}