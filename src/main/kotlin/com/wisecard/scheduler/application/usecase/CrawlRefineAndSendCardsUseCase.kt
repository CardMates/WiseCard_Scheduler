package com.wisecard.scheduler.application.usecase

import com.sub.grpc.CardData
import com.wisecard.scheduler.application.mapper.CardBenefitMapper
import com.wisecard.scheduler.application.mapper.CardCompanyMapper
import com.wisecard.scheduler.application.ports.out.CardBenefitsSender
import com.wisecard.scheduler.application.ports.out.CardStoragePort
import com.wisecard.scheduler.application.ports.out.BenefitsRefiner
import com.wisecard.scheduler.application.ports.out.CardCrawlingPort
import com.wisecard.scheduler.domain.card.CardInfo
import org.springframework.stereotype.Service

@Service
class CrawlRefineAndSendCardsUseCase(
    private val benefitsRefiner: BenefitsRefiner,
    private val cardCrawlingPort: CardCrawlingPort,
    private val cardStoragePort: CardStoragePort,
    private val cardBenefitsSender: CardBenefitsSender
) {
    fun execute() {
        val existingCards = cardStoragePort.loadStoredCards()
        val allBasicCards = cardCrawlingPort.crawlAllBasicCards()
        val newBasicCards = cardStoragePort.filterNewCards(allBasicCards)

        if (newBasicCards.isEmpty()) {
            sendAllCards(existingCards)
            return
        }

        val detailedNewCards = cardCrawlingPort.crawlDetailsFor(newBasicCards)
        val newCardsWithIds = cardStoragePort.assignIdsToNewCards(detailedNewCards)
        val refinedNewCards = refineNewCards(newCardsWithIds)
        val allCards = cardStoragePort.mergeCards(existingCards, refinedNewCards)
        cardStoragePort.saveCards(allCards)
        sendAllCards(allCards)
    }

    private fun refineNewCards(newCards: List<CardInfo>): List<CardInfo> {
        return newCards.map { card ->
            val refinedBenefits = benefitsRefiner.refine(card.benefits ?: "")
            card.copy(benefits = refinedBenefits)
        }
    }

    private fun sendAllCards(cards: List<CardInfo>) {
        val listBuilder = CardData.CardBenefitList.newBuilder()
        cards.forEach { card ->
            val message = CardBenefitMapper.parseJsonToProto(card.benefits ?: "")
            val crawledBenefit = CardData.CardBenefit.newBuilder()
                .setCardId(card.cardId ?: 0)
                .setCardCompany(CardCompanyMapper.toProto(card.cardCompany))
                .setCardName(card.cardName)
                .setImgUrl(card.imgUrl ?: "")
                .setCardType(if (card.cardType.name == "CREDIT") CardData.CardType.CREDIT else CardData.CardType.DEBIT)
                .addAllBenefits(message)
                .build()
            listBuilder.addCardBenefits(crawledBenefit)
        }
        cardBenefitsSender.send(listBuilder.build())
    }
}