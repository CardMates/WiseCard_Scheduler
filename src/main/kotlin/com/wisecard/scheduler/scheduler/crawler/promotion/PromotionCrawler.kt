package com.wisecard.scheduler.scheduler.crawler.promotion

import com.wisecard.scheduler.scheduler.dto.Promotion

interface  PromotionCrawler {
    fun crawlPromotions() : List<Promotion>
}