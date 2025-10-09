package com.wisecard.scheduler.scheduler.crawler.promotion

import com.wisecard.scheduler.scheduler.dto.CardCompany
import com.wisecard.scheduler.scheduler.dto.PromotionInfo

interface PromotionCrawler {
    val cardCompany: CardCompany

    fun crawlPromotions(): List<PromotionInfo>
}