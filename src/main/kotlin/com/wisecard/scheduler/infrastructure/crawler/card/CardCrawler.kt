package com.wisecard.scheduler.infrastructure.crawler.card

import com.wisecard.scheduler.domain.card.CardCompany
import com.wisecard.scheduler.domain.card.CardInfo

interface CardCrawler {
    val cardCompany: CardCompany

    fun crawlCreditCardBasicInfos(): List<CardInfo>
    fun crawlCheckCardBasicInfos(): List<CardInfo>

    fun crawlCreditCardBenefits(cards: List<CardInfo>): List<CardInfo>
    fun crawlCheckCardBenefits(cards: List<CardInfo>): List<CardInfo>
}