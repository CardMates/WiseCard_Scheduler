package com.wisecard.scheduler.application.ports.out

import com.sub.grpc.Promotion

interface PromotionSender {
    fun send(promotionList: Promotion.CardPromotionList)
}