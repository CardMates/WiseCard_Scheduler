package com.wisecard.scheduler.scheduler.crawler.card

import com.wisecard.scheduler.scheduler.dto.CardCompany
import com.wisecard.scheduler.scheduler.dto.CardInfo

interface CardCrawler {
    val cardCompany: CardCompany

    fun crawlCreditCardBasicInfos(): List<CardInfo>
    fun crawlCheckCardBasicInfos(): List<CardInfo>

    fun crawlCreditCardBenefits(cards: List<CardInfo>): List<CardInfo>
    fun crawlCheckCardBenefits(cards: List<CardInfo>): List<CardInfo>
}