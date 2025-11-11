package com.wisecard.scheduler.infrastructure.grpc.adapter

import com.sub.grpc.CardData
import com.wisecard.scheduler.application.ports.out.CardBenefitsSender
import com.wisecard.scheduler.infrastructure.grpc.CardServiceImpl
import org.springframework.stereotype.Component

@Component
class CardBenefitsSenderAdapter(
    private val cardServiceImpl: CardServiceImpl
) : CardBenefitsSender {
    override fun send(cardBenefitList: CardData.CardBenefitList) {
        cardServiceImpl.sendCardBenefits(cardBenefitList)
    }
}