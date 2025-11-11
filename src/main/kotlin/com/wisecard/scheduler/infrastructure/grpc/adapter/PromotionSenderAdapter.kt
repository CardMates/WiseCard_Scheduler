package com.wisecard.scheduler.infrastructure.grpc.adapter

import com.sub.grpc.Promotion
import com.wisecard.scheduler.application.ports.out.PromotionSender
import com.wisecard.scheduler.infrastructure.grpc.PromotionServiceImpl
import org.springframework.stereotype.Component

@Component
class PromotionSenderAdapter(
    private val promotionServiceImpl: PromotionServiceImpl
) : PromotionSender {
    override fun send(promotionList: Promotion.CardPromotionList) {
        promotionServiceImpl.sendPromotions(promotionList)
    }
}