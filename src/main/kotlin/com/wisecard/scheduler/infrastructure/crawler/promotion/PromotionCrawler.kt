package com.wisecard.scheduler.infrastructure.crawler.promotion

import com.wisecard.scheduler.domain.card.CardCompany
import com.wisecard.scheduler.domain.promotion.PromotionInfo

interface PromotionCrawler {
    val cardCompany: CardCompany

    fun crawlPromotions(): List<PromotionInfo>
}