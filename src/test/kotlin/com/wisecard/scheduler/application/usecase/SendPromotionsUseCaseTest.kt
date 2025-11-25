package com.wisecard.scheduler.application.usecase

import com.wisecard.scheduler.application.ports.out.PromotionCrawlingPort
import com.wisecard.scheduler.application.ports.out.PromotionSender
import com.wisecard.scheduler.domain.card.CardCompany
import com.wisecard.scheduler.domain.promotion.PromotionInfo
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SendPromotionsUseCaseTest {

    private lateinit var promotionCrawlingPort: PromotionCrawlingPort
    private lateinit var promotionSender: PromotionSender
    private lateinit var useCase: SendPromotionsUseCase

    @BeforeEach
    fun setUp() {
        promotionCrawlingPort = mockk()
        promotionSender = mockk()

        useCase = SendPromotionsUseCase(
            promotionCrawlingPort = promotionCrawlingPort,
            promotionSender = promotionSender
        )
    }

    @Test
    fun `크롤링한 프로모션을 전송`() {
        val promotions = listOf(
            createPromotionInfo("프로모션1"),
            createPromotionInfo("프로모션2")
        )

        every { promotionCrawlingPort.crawlPromotions() } returns promotions
        every { promotionSender.send(any()) } returns Unit

        useCase.execute()

        verify(exactly = 1) { promotionCrawlingPort.crawlPromotions() }
        verify(exactly = 1) { promotionSender.send(any()) }
    }

    private fun createPromotionInfo(description: String): PromotionInfo {
        return PromotionInfo(
            cardCompany = CardCompany.SAMSUNG,
            description = description,
            imgUrl = "https://example.com/img.jpg",
            url = "https://example.com",
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusDays(30)
        )
    }
}