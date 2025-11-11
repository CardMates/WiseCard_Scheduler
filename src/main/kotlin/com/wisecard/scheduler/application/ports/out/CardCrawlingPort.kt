package com.wisecard.scheduler.application.ports.out

import com.wisecard.scheduler.domain.card.CardInfo

interface CardCrawlingPort {
    fun crawlAllBasicCards(): List<CardInfo>
    fun crawlDetailsFor(newBasicCards: List<CardInfo>): List<CardInfo>
}