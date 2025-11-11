package com.wisecard.scheduler.application.ports.out

import com.wisecard.scheduler.domain.promotion.PromotionInfo

interface PromotionCrawlingPort {
    fun crawlPromotions(): List<PromotionInfo>
}