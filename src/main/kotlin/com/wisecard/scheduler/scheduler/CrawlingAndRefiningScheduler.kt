package com.wisecard.scheduler.scheduler

import com.sub.grpc.CardCompanyOuterClass
import com.sub.grpc.CardData
import com.sub.grpc.Promotion
import com.wisecard.scheduler.grpc.CardServiceImpl
import com.wisecard.scheduler.grpc.PromotionServiceImpl
import com.wisecard.scheduler.scheduler.crawler.card.CardCrawler
import com.wisecard.scheduler.scheduler.crawler.promotion.PromotionCrawler
import com.wisecard.scheduler.scheduler.dto.CardCompany
import com.wisecard.scheduler.scheduler.dto.CardInfo
import com.wisecard.scheduler.scheduler.dto.CardType
import com.wisecard.scheduler.scheduler.dto.PromotionInfo
import com.wisecard.scheduler.scheduler.llm.LlmClient
import com.wisecard.scheduler.scheduler.service.CardDataStorageService
import com.wisecard.scheduler.scheduler.service.JsonToProtoService
import com.wisecard.scheduler.scheduler.util.DateUtils.toProtoTimestamp
import com.wisecard.scheduler.scheduler.util.logger
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class CrawlingAndRefiningScheduler(
    private val llmClient: LlmClient,
    private val jsonToProtoService: JsonToProtoService,
    private val cardCrawlers: List<CardCrawler>,
    private val promotionCrawlers: List<PromotionCrawler>,
    private val cardService: CardServiceImpl,
    private val promotionService: PromotionServiceImpl,
    private val cardDataStorageService: CardDataStorageService
) : ApplicationRunner {
//) {

    override fun run(args: ApplicationArguments?) {
//    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
//    fun scheduler() {
        // 프로모션
        val promotions = promotionCrawlers.flatMap { it.crawlPromotions() }
        sendAllPromotions(promotions)

        // 카드 혜택
        val existingCards = cardDataStorageService.loadStoredCards()

        val allBasicCards = cardCrawlers.flatMap { it.crawlCreditCardBasicInfos() + it.crawlCheckCardBasicInfos() }
        val newBasicCards = cardDataStorageService.filterNewCards(allBasicCards)
        logger.info("신규 카드 수 ${newBasicCards.size}개가 있습니다.")
        if (newBasicCards.isEmpty()) {
            logger.info("새로운 카드가 없습니다. 기존 데이터를 그대로 전송합니다.")
            sendExistingCards(existingCards)
            return
        }

        val detailedNewCards = crawlDetailedNewCards(newBasicCards)
        val newCardsWithIds = cardDataStorageService.assignIdsToNewCards(detailedNewCards)
        val refinedNewCards = refineNewCards(newCardsWithIds)

        val allCards = cardDataStorageService.mergeCards(existingCards, refinedNewCards)
        cardDataStorageService.saveCards(allCards)
        sendAllCards(allCards)
    }

    private fun crawlDetailedNewCards(newBasicCards: List<CardInfo>): List<CardInfo> {
        return cardCrawlers.flatMap { crawler ->
            val companyCards = newBasicCards.filter { it.cardCompany == crawler.cardCompany }

            if (companyCards.isNotEmpty()) {
                val creditCards = companyCards.filter { it.cardType == CardType.CREDIT }
                val checkCards = companyCards.filter { it.cardType == CardType.CHECK }

                val detailedCreditCards = if (creditCards.isNotEmpty()) {
                    crawler.crawlCreditCardBenefits(creditCards)
                } else emptyList()

                val detailedCheckCards = if (checkCards.isNotEmpty()) {
                    crawler.crawlCheckCardBenefits(checkCards)
                } else emptyList()

                detailedCreditCards + detailedCheckCards
            } else emptyList()
        }
    }

    private fun refineNewCards(newCards: List<CardInfo>): List<CardInfo> {
        return newCards.map { card ->
            val refinedBenefits = llmClient.refine(card.benefits ?: "")
            card.copy(benefits = refinedBenefits)
        }
    }

    private fun sendExistingCards(existingCards: List<CardInfo>) {
        sendAllCards(existingCards)
    }

    private fun sendAllCards(cards: List<CardInfo>) {
        val listBuilder = CardData.CardBenefitList.newBuilder()

        cards.forEach { card ->
            val message = jsonToProtoService.parseJsonToProto(card.benefits ?: "")

            val crawledBenefit = CardData.CardBenefit.newBuilder()
                .setCardId(card.cardId ?: 0)
                .setCardCompany(mapToProtoCompany(card.cardCompany))
                .setCardName(card.cardName)
                .setImgUrl(card.imgUrl ?: "")
                .setCardType(if (card.cardType.name == "CREDIT") CardData.CardType.CREDIT else CardData.CardType.DEBIT)
                .addAllBenefits(message)
                .build()

            listBuilder.addCardBenefits(crawledBenefit)
        }

        cardService.sendCardBenefits(listBuilder.build())
        logger.info("${cards.size}개의 카드를 gRPC로 전송했습니다.")
    }

    private fun sendAllPromotions(promotions: List<PromotionInfo>) {
        val listBuilder = Promotion.CardPromotionList.newBuilder()

        promotions.forEach { promotion ->
            val cardPromotion = Promotion.CardPromotion.newBuilder()
                .setCardCompany(mapToProtoCompany(promotion.cardCompany))
                .setDescription(promotion.description)
                .setUrl(promotion.url)
                .setImgUrl(promotion.imgUrl)
                .setStartDate(toProtoTimestamp(promotion.startDate))
                .setEndDate(toProtoTimestamp(promotion.endDate))
                .build()

            listBuilder.addCardPromotion(cardPromotion)
        }

        promotionService.sendPromotions(listBuilder.build())
        logger.info("${promotions.size}개의 프로모션을 gRPC로 전송했습니다.")
    }

    private fun mapToProtoCompany(company: CardCompany): CardCompanyOuterClass.CardCompany {
        return when (company) {
            CardCompany.HANA -> CardCompanyOuterClass.CardCompany.HANA
            CardCompany.SHINHAN -> CardCompanyOuterClass.CardCompany.SHINHAN
            CardCompany.HYUNDAI -> CardCompanyOuterClass.CardCompany.HYUNDAI
            CardCompany.KOOKMIN -> CardCompanyOuterClass.CardCompany.KOOKMIN
            CardCompany.LOTTE -> CardCompanyOuterClass.CardCompany.LOTTE
            CardCompany.SAMSUNG -> CardCompanyOuterClass.CardCompany.SAMSUNG
        }
    }
}