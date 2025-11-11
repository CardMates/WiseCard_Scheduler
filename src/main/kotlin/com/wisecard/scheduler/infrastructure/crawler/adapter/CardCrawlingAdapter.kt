package com.wisecard.scheduler.infrastructure.crawler.adapter

import com.wisecard.scheduler.application.ports.out.CardCrawlingPort
import com.wisecard.scheduler.domain.card.CardInfo
import com.wisecard.scheduler.domain.card.CardType
import com.wisecard.scheduler.infrastructure.crawler.card.CardCrawler
import org.springframework.stereotype.Component

@Component
class CardCrawlingAdapter(
    private val cardCrawlers: List<CardCrawler>
) : CardCrawlingPort {
    override fun crawlAllBasicCards(): List<CardInfo> {
        return cardCrawlers.flatMap { it.crawlCreditCardBasicInfos() + it.crawlCheckCardBasicInfos() }
    }

    override fun crawlDetailsFor(newBasicCards: List<CardInfo>): List<CardInfo> {
        return cardCrawlers.flatMap { crawler ->
            val companyCards = newBasicCards.filter { it.cardCompany == crawler.cardCompany }
            if (companyCards.isEmpty()) return@flatMap emptyList<CardInfo>()
            val creditCards = companyCards.filter { it.cardType == CardType.CREDIT }
            val checkCards = companyCards.filter { it.cardType == CardType.CHECK }
            val detailedCredit =
                if (creditCards.isNotEmpty()) crawler.crawlCreditCardBenefits(creditCards) else emptyList()
            val detailedCheck = if (checkCards.isNotEmpty()) crawler.crawlCheckCardBenefits(checkCards) else emptyList()
            detailedCredit + detailedCheck
        }
    }
}