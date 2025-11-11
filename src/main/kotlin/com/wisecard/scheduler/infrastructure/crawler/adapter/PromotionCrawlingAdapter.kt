package com.wisecard.scheduler.infrastructure.crawler.adapter

import com.wisecard.scheduler.application.ports.out.PromotionCrawlingPort
import com.wisecard.scheduler.domain.promotion.PromotionInfo
import com.wisecard.scheduler.infrastructure.crawler.promotion.PromotionCrawler
import org.springframework.stereotype.Component

@Component
class PromotionCrawlingAdapter(
    private val promotionCrawlers: List<PromotionCrawler>
) : PromotionCrawlingPort {
    override fun crawlPromotions(): List<PromotionInfo> {
        return promotionCrawlers.flatMap { it.crawlPromotions() }
    }
}