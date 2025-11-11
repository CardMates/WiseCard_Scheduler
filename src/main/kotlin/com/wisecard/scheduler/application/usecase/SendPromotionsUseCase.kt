package com.wisecard.scheduler.application.usecase

import com.sub.grpc.Promotion
import com.wisecard.scheduler.application.mapper.CardCompanyMapper
import com.wisecard.scheduler.application.ports.out.PromotionCrawlingPort
import com.wisecard.scheduler.application.ports.out.PromotionSender
import com.wisecard.scheduler.util.DateUtils.toProtoTimestamp
import org.springframework.stereotype.Service

@Service
class SendPromotionsUseCase(
    private val promotionCrawlingPort: PromotionCrawlingPort,
    private val promotionSender: PromotionSender
) {
    fun execute() {
        val promotions = promotionCrawlingPort.crawlPromotions()
        val listBuilder = Promotion.CardPromotionList.newBuilder()
        promotions.forEach { promotion ->
            val cardPromotion = Promotion.CardPromotion.newBuilder()
                .setCardCompany(CardCompanyMapper.toProto(promotion.cardCompany))
                .setDescription(promotion.description)
                .setUrl(promotion.url)
                .setImgUrl(promotion.imgUrl)
                .setStartDate(toProtoTimestamp(promotion.startDate))
                .setEndDate(toProtoTimestamp(promotion.endDate))
                .build()
            listBuilder.addCardPromotion(cardPromotion)
        }
        promotionSender.send(listBuilder.build())
    }
}