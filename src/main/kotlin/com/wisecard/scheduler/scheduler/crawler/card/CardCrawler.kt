package com.wisecard.scheduler.scheduler.crawler.card

import com.wisecard.scheduler.scheduler.dto.CardInfo

interface CardCrawler {
    fun crawlCreditCards(): List<CardInfo>
    fun crawlCheckCards(): List<CardInfo>
}