package com.wisecard.scheduler.application.ports.out

import com.sub.grpc.CardData

interface CardBenefitsSender {
    fun send(cardBenefitList: CardData.CardBenefitList)
}